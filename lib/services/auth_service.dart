import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/services/hce_bridge.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

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
  UserProfile? currentUser;

  // --- LOGIN ---
  Future<UserProfile?> signInWithGoogle() async {
    try {
      final GoogleSignInAccount? googleUser = await _googleSignIn.signIn();
      
      if (googleUser == null) return null; // Người dùng huỷ đăng nhập

      if (!googleUser.email.toLowerCase().endsWith('@fpt.edu.vn')) {
        await _googleSignIn.signOut();
        throw Exception('Vui lòng sử dụng email sinh viên FPT (@fpt.edu.vn)!');
      }

      String googleName = googleUser.displayName ?? googleUser.email.split('@')[0];

      // 1. Lấy Token từ Google
      final GoogleSignInAuthentication googleAuth = await googleUser.authentication;
      final String? idToken = googleAuth.idToken;

      if (idToken == null) {
        throw Exception('Không lấy được ID Token từ Google.');
      }

      // 👉 2. Lấy FCM Token TRƯỚC khi gọi API Login (QUAN TRỌNG)
      String? fcmToken;
      try {
        fcmToken = await FirebaseMessaging.instance.getToken();
        print("📲 FCM Token captured for login: $fcmToken");
      } catch (e) {
        print("⚠️ Warning: Không lấy được FCM Token lúc login: $e");
      }

      // 👉 3. Gửi ID Token + FCM Token lên Backend
      final response = await http.post(
        Uri.parse('$baseUrl/login-google'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'id_token': idToken,
          'full_name': googleName,
          'noti_device': fcmToken, // ✅ Gửi kèm FCM Token luôn
        }),
      );

      if (response.statusCode == 200) {
        final decodedBody = utf8.decode(response.bodyBytes);
        final jsonMap = jsonDecode(decodedBody);
        
        // Backend trả về: { "access_token": "...", "user": { ... } }
        final String accessToken = jsonMap['access_token'];
        await _saveToken(accessToken);

        if (jsonMap['user'] != null) {
          currentUser = UserProfile.fromJson(jsonMap['user']);
          
          await _saveStudentCode(currentUser!.studentCode);
          await HceBridge.setStudentCode(currentUser!.studentCode);
        }
        notifyListeners();
        return currentUser;
      } else {
        throw Exception('Đăng nhập thất bại (Code: ${response.statusCode}): ${response.body}');
      }
    } catch (e) {
      if (e.toString().contains("Vui lòng sử dụng email")) rethrow;
      print('Google Sign-In Error: $e');
      throw Exception('Đăng nhập thất bại. Vui lòng thử lại.');
    }
  }

  // --- AUTO LOGIN (Mở lại app) ---
  Future<bool> checkLoginStatus() async {
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

        currentUser = UserProfile.fromJson(jsonMap);

        if (currentUser != null) {
          await HceBridge.setStudentCode(currentUser!.studentCode);
          syncFcmToken(currentUser!.id); 
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

  Future<UserProfile?> getProfile({bool forceRefresh = false}) async {
    try {
      if (currentUser != null && !forceRefresh) {
        return currentUser;
      }
      String? token = await getToken();
      if (token == null) return null;

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
        
        currentUser = UserProfile.fromJson(jsonMap);
        notifyListeners();
        return currentUser;
      }
    } catch (e) {
      print("Lỗi getProfile: $e");
    }
    return null;
  }

  Future<void> logout() async {
    try {
      await _storage.deleteAll(); 
      await _googleSignIn.signOut();
      await HceBridge.clearStudentCode();
      currentUser = null;
      notifyListeners();
    } catch (e) {
      print("Lỗi logout: $e");
      // Vẫn clear local state ngay cả khi Google Sign-In fail
      currentUser = null;
      notifyListeners();
    }
  }

  // Hàm này dùng cho Auto-Login hoặc khi muốn sync thủ công
  Future<void> syncFcmToken(String userId) async {
    try {
      String? fcmToken = await FirebaseMessaging.instance.getToken();
      String? jwt = await getToken();
      
      if (fcmToken == null || jwt == null) return;
      
      // Nếu FCM token hiện tại giống hệt cái trong currentUser thì thôi không cần gọi API (Tối ưu)
      if (currentUser?.notiDevice == fcmToken) {
         print("✅ FCM Token is up-to-date, skipping sync.");
         return;
      }

      print("📤 Syncing FCM manually...");
      
      final response = await http.patch(
        Uri.parse('$baseUrl/me'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $jwt',
        },
        body: jsonEncode({'noti_device': fcmToken}), 
      );
      
      if (response.statusCode == 200) {
        print("✅ FCM Token synced manually via /me");
      }
    } catch (e) {
      print("❌ FCM Sync Error: $e");
    }
  }

  Future<void> _saveToken(String token) async => 
      await _storage.write(key: 'jwt_token', value: token);

  Future<String?> getToken() async => 
      await _storage.read(key: 'jwt_token');

  Future<void> _saveStudentCode(String code) async => 
      await _storage.write(key: 'student_code', value: code);

  Future<void> signInWithFEID() async {
    print("Tính năng đang phát triển...");
  }
}