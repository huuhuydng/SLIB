import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

// --- IMPORTS MỚI ---
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/models/user_setting.dart';   
import 'package:slib/services/hce_bridge.dart';
import 'package:slib/services/user_setting_service.dart'; 
import 'package:slib/services/app/local_storage_service.dart'; 

class AuthService extends ChangeNotifier {
  static const String _webClientId = '262933313086-mhbevhu0b7hfqekchf6a99vnebjfr8b5.apps.googleusercontent.com';

  static String getBaseUrl() {
    return ApiConstants.authUrl; 
  }
  static String baseUrl = getBaseUrl();

  final GoogleSignIn _googleSignIn = GoogleSignIn(
    serverClientId: _webClientId,
    scopes: ['email', 'profile'],
  );

  final _storage = const FlutterSecureStorage();
  
  final UserSettingService _settingApiService = UserSettingService();
  final LocalStorageService _localService = LocalStorageService();

  UserProfile? _currentUser;
  UserSetting? _currentSetting;

  UserProfile? get currentUser => _currentUser;
  UserSetting? get currentSetting => _currentSetting;

  Future<bool> checkLoginStatus() async {
    _currentSetting = await _localService.loadSettings();
    notifyListeners(); 

    String? token = await getToken();
    if (token == null) return false;
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/me'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        final decodedBody = utf8.decode(response.bodyBytes);
        final jsonMap = jsonDecode(decodedBody);
        _currentUser = UserProfile.fromJson(jsonMap);

        if (_currentUser != null) {
          await HceBridge.setUserId(_currentUser!.id);
          syncFcmToken(_currentUser!.id);
          await _fetchAndSyncSettings(_currentUser!.id);
        }
        notifyListeners();
        return true;
      } else {
        await logout();
        return false;
      }
    } catch (e) {
      print("Check login error: $e");
      return false;
    }
  }

  Future<UserProfile?> signInWithGoogle() async {
    try {
      final GoogleSignInAccount? googleUser = await _googleSignIn.signIn();
      if (googleUser == null) return null;

      if (!googleUser.email.toLowerCase().endsWith('@fpt.edu.vn')) {
        await _googleSignIn.signOut();
        throw Exception('Vui lòng sử dụng email sinh viên FPT (@fpt.edu.vn)!');
      }

      final GoogleSignInAuthentication googleAuth = await googleUser.authentication;
      final String? idToken = googleAuth.idToken;

      if (idToken == null) throw Exception('Không lấy được ID Token từ Google.');

      String? fcmToken;
      try {
        fcmToken = await FirebaseMessaging.instance.getToken();
      } catch (_) {}

      final response = await http.post(
        Uri.parse('$baseUrl/login-google'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'id_token': idToken,
          'full_name': googleUser.displayName, 
          'noti_device': fcmToken,
        }),
      );

      if (response.statusCode == 200) {
        final decodedBody = utf8.decode(response.bodyBytes);
        final jsonMap = jsonDecode(decodedBody);

        // New JWT response format - accessToken and user data at root level
        final String accessToken = jsonMap['accessToken'] ?? jsonMap['access_token'];
        final String? refreshToken = jsonMap['refreshToken'];
        
        await _saveToken(accessToken);
        if (refreshToken != null) {
          await _saveRefreshToken(refreshToken);
        }

        // User data is now at root level, not nested in 'user'
        _currentUser = UserProfile(
          id: jsonMap['id'] ?? '',
          email: jsonMap['email'] ?? '',
          fullName: jsonMap['fullName'] ?? '',
          studentCode: jsonMap['studentCode'] ?? '',
          role: jsonMap['role'] ?? 'STUDENT',
          reputationScore: jsonMap['reputationScore'] ?? 100,
          isActive: jsonMap['isActive'] ?? true,
        );
        
        await _saveStudentCode(_currentUser!.studentCode);
        await HceBridge.setUserId(_currentUser!.id);
        await _fetchAndSyncSettings(_currentUser!.id);

        notifyListeners();
        return _currentUser;
      } else {
        throw Exception('Đăng nhập thất bại: ${response.statusCode}');
      }
    } catch (e) {
      if (e.toString().contains("Vui lòng sử dụng email")) rethrow;
      print('Google Sign-In Error: $e');
      throw Exception('Đăng nhập thất bại. Vui lòng thử lại.');
    }
  }


  Future<void> _fetchAndSyncSettings(String userId) async {
    try {
      final freshSetting = await _settingApiService.getSettings(userId);
      _currentSetting = freshSetting;
      await _localService.saveSettings(freshSetting); 
    } catch (e) {
      print("⚠️ Lỗi sync setting từ server: $e");
    }
  }

  Future<void> updateSetting(UserSetting newSetting) async {
    _currentSetting = newSetting;
    notifyListeners();

    await _localService.saveSettings(newSetting);

    try {
      await _settingApiService.updateSettings(newSetting.userId, newSetting);
    } catch (e) {
      print("❌ Lỗi sync setting lên server: $e");
    }
  }

  Future<void> logout() async {
    try {
      await _storage.deleteAll();
      await _googleSignIn.signOut();
      await HceBridge.clearUserId();
      
      _currentUser = null;
      _currentSetting = null;
      await _localService.clearData();
      
      notifyListeners();
    } catch (e) {
      print("Lỗi logout: $e");
      _currentUser = null;
      _currentSetting = null;
      notifyListeners();
    }
  }
  
  Future<void> syncFcmToken(String userId) async {
    try {
      String? fcmToken = await FirebaseMessaging.instance.getToken();
      String? jwt = await getToken();

      if (fcmToken == null || jwt == null) return;

      if (_currentUser?.notiDevice == fcmToken) return;

      final response = await http.patch(
        Uri.parse('$baseUrl/me'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $jwt',
        },
        body: jsonEncode({'noti_device': fcmToken}),
      );
      if(response.statusCode == 200) print("✅ FCM Synced");
    } catch (e) {
      print("❌ FCM Sync Error: $e");
    }
  }

  // Token storage methods
  Future<void> _saveToken(String token) async => await _storage.write(key: 'jwt_token', value: token);
  Future<String?> getToken() async => await _storage.read(key: 'jwt_token');
  Future<void> _saveRefreshToken(String token) async => await _storage.write(key: 'refresh_token', value: token);
  Future<String?> getRefreshToken() async => await _storage.read(key: 'refresh_token');
  Future<void> _saveStudentCode(String code) async => await _storage.write(key: 'student_code', value: code);
  
  Future<UserProfile?> getProfile({bool forceRefresh = false}) async {
    if (_currentUser != null && !forceRefresh) return _currentUser;
    await checkLoginStatus(); 
    return _currentUser;
  }

  /// Refresh access token using refresh token
  Future<bool> refreshAccessToken() async {
    try {
      final refreshToken = await getRefreshToken();
      if (refreshToken == null) return false;

      final response = await http.post(
        Uri.parse('${ApiConstants.authBaseUrl}/refresh'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'refreshToken': refreshToken}),
      );

      if (response.statusCode == 200) {
        final jsonMap = jsonDecode(utf8.decode(response.bodyBytes));
        await _saveToken(jsonMap['accessToken']);
        return true;
      }
      return false;
    } catch (e) {
      print("❌ Token refresh error: $e");
      return false;
    }
  }
}