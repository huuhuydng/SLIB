import 'dart:async';
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
import 'package:slib/services/hce/hce_bridge.dart';
import 'package:slib/services/user/user_setting_service.dart';
import 'package:slib/services/app/local_storage_service.dart';
import 'package:slib/services/deep_link/deep_link_service.dart';
import 'package:slib/views/authentication/on_boarding_screen.dart';

class AuthService extends ChangeNotifier {
  static const String _webClientId =
      '262933313086-mhbevhu0b7hfqekchf6a99vnebjfr8b5.apps.googleusercontent.com';

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
  bool _isHandlingSessionExpiry = false;

  UserProfile? get currentUser => _currentUser;
  UserSetting? get currentSetting => _currentSetting;

  static String _extractReadableErrorMessage(dynamic error) {
    String raw = error.toString().trim();
    if (raw.startsWith('Exception: ')) {
      raw = raw.substring(11).trim();
    }

    if (raw.isEmpty) {
      return 'Đã xảy ra lỗi, vui lòng thử lại.';
    }

    try {
      final decoded = jsonDecode(raw);
      if (decoded is Map<String, dynamic>) {
        final message = decoded['message'] ?? decoded['error'];
        if (message is String && message.trim().isNotEmpty) {
          return message.trim();
        }
      }
    } catch (_) {}

    return raw;
  }

  Future<bool> checkLoginStatus() async {
    _currentSetting = await _localService.loadSettings();
    notifyListeners();

    String? token = await getToken();
    if (token == null) return false;
    try {
      var response = await http.get(
        Uri.parse('$baseUrl/me'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      // Auto-refresh: nếu access token hết hạn, thử refresh trước khi logout
      if (response.statusCode == 401 || response.statusCode == 403) {
        final refreshed = await refreshAccessToken();
        if (refreshed) {
          token = await getToken();
          response = await http.get(
            Uri.parse('$baseUrl/me'),
            headers: {
              'Content-Type': 'application/json',
              'Authorization': 'Bearer $token',
            },
          );
        } else {
          await logout();
          return false;
        }
      }

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
        debugPrint("Check login failed with status: ${response.statusCode}");
        if (response.statusCode == 401 || response.statusCode == 403) {
          await _handleSessionExpired();
        }
        return false;
      }
    } catch (e) {
      debugPrint("Check login error: $e");
      return false;
    }
  }

  Future<UserProfile?> signInWithGoogle() async {
    try {
      final GoogleSignInAccount? googleUser = await _googleSignIn.signIn();
      if (googleUser == null) return null;

      final GoogleSignInAuthentication googleAuth =
          await googleUser.authentication;
      final String? idToken = googleAuth.idToken;

      if (idToken == null) {
        throw Exception('Không lấy được ID Token từ Google.');
      }

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
        final String accessToken =
            jsonMap['accessToken'] ?? jsonMap['access_token'];
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
          userCode: jsonMap['userCode'] ?? jsonMap['studentCode'] ?? '',
          role: jsonMap['role'] ?? 'STUDENT',
          reputationScore: jsonMap['reputationScore'] ?? 100,
          isActive: jsonMap['isActive'] ?? true,
          passwordChanged: jsonMap['passwordChanged'] ?? true,
        );

        await _saveUserCode(_currentUser!.userCode);
        await HceBridge.setUserId(_currentUser!.id);
        await _fetchAndSyncSettings(_currentUser!.id);

        // Fetch full profile from /me to get avtUrl and other fields
        // not included in login response
        await checkLoginStatus();

        notifyListeners();
        return _currentUser;
      } else {
        final decodedBody = utf8.decode(response.bodyBytes);
        try {
          final jsonMap = jsonDecode(decodedBody);
          throw Exception(
            jsonMap['message'] ??
                jsonMap['error'] ??
                'Đăng nhập thất bại: ${response.statusCode}',
          );
        } catch (_) {
          throw Exception(
            decodedBody.isNotEmpty
                ? decodedBody
                : 'Đăng nhập thất bại: ${response.statusCode}',
          );
        }
      }
    } catch (e) {
      debugPrint('Google Sign-In Error: $e');
      final message = _extractReadableErrorMessage(e);
      if (message.isEmpty) {
        throw Exception('Đăng nhập thất bại. Vui lòng thử lại.');
      }
      throw Exception(message);
    }
  }

  /// Sign in with email/username/MSSV and password
  Future<UserProfile?> signInWithPassword(
    String identifier,
    String password,
  ) async {
    try {
      String? fcmToken;
      try {
        fcmToken = await FirebaseMessaging.instance.getToken();
      } catch (_) {}

      final response = await http.post(
        Uri.parse('${ApiConstants.authBaseUrl}/login'),
        headers: {
          'Content-Type': 'application/json',
          'X-Device-Info': 'mobile-app',
        },
        body: jsonEncode({'identifier': identifier, 'password': password}),
      );

      if (response.statusCode == 200) {
        final decodedBody = utf8.decode(response.bodyBytes);
        final jsonMap = jsonDecode(decodedBody);

        final String accessToken =
            jsonMap['accessToken'] ?? jsonMap['access_token'];
        final String? refreshToken = jsonMap['refreshToken'];

        await _saveToken(accessToken);
        if (refreshToken != null) {
          await _saveRefreshToken(refreshToken);
        }

        _currentUser = UserProfile(
          id: jsonMap['id'] ?? '',
          email: jsonMap['email'] ?? '',
          fullName: jsonMap['fullName'] ?? '',
          userCode: jsonMap['userCode'] ?? '',
          role: jsonMap['role'] ?? 'STUDENT',
          reputationScore: jsonMap['reputationScore'] ?? 100,
          isActive: jsonMap['isActive'] ?? true,
          passwordChanged: jsonMap['passwordChanged'] ?? false,
        );

        await _saveUserCode(_currentUser!.userCode);
        await HceBridge.setUserId(_currentUser!.id);

        // Sync FCM token after password login
        if (fcmToken != null) {
          syncFcmToken(_currentUser!.id);
        }

        await _fetchAndSyncSettings(_currentUser!.id);

        // Fetch full profile from /me to get avtUrl and other fields
        // not included in login response
        await checkLoginStatus();

        notifyListeners();
        return _currentUser;
      } else {
        final errorBody = utf8.decode(response.bodyBytes);
        String errorMessage;
        try {
          final jsonError = jsonDecode(errorBody);
          errorMessage =
              jsonError['message'] ?? jsonError['error'] ?? errorBody;
        } catch (_) {
          errorMessage = errorBody;
        }
        throw Exception(errorMessage);
      }
    } catch (e) {
      debugPrint('Password Sign-In Error: $e');
      throw Exception(_extractReadableErrorMessage(e));
    }
  }

  /// Change password for authenticated user
  Future<void> changePassword(
    String currentPassword,
    String newPassword,
  ) async {
    try {
      String? token = await getToken();
      if (token == null) {
        throw Exception('Bạn chưa đăng nhập');
      }

      final response = await http.post(
        Uri.parse('${ApiConstants.authBaseUrl}/change-password'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode({
          'currentPassword': currentPassword,
          'newPassword': newPassword,
        }),
      );

      if (response.statusCode == 200) {
        // Update local user state
        if (_currentUser != null) {
          _currentUser = _currentUser!.copyWith(passwordChanged: true);
          notifyListeners();
        }
      } else {
        final errorBody = utf8.decode(response.bodyBytes);
        String errorMessage;
        try {
          final jsonError = jsonDecode(errorBody);
          errorMessage =
              jsonError['message'] ?? jsonError['error'] ?? errorBody;
        } catch (_) {
          errorMessage = errorBody;
        }
        throw Exception(errorMessage);
      }
    } catch (e) {
      throw Exception(_extractReadableErrorMessage(e));
    }
  }

  Future<void> _fetchAndSyncSettings(String userId) async {
    try {
      final freshSetting = await _settingApiService.getSettings(userId);
      _currentSetting = freshSetting;
      await _localService.saveSettings(freshSetting);
    } catch (e) {
      debugPrint("⚠️ Lỗi sync setting từ server: $e");
    }
  }

  Future<void> updateSetting(UserSetting newSetting) async {
    _currentSetting = newSetting;
    notifyListeners();

    await _localService.saveSettings(newSetting);

    try {
      await _settingApiService.updateSettings(newSetting.userId, newSetting);
    } catch (e) {
      debugPrint("❌ Lỗi sync setting lên server: $e");
    }
  }

  Future<void> logout() async {
    try {
      // QUAN TRỌNG: Xóa FCM token trên server TRƯỚC khi xóa JWT
      // Nếu không, user cũ vẫn giữ FCM token → nhận notification sai
      try {
        String? token = await _readStoredToken();
        if (token != null) {
          await http.patch(
            Uri.parse('$baseUrl/me'),
            headers: {
              'Content-Type': 'application/json',
              'Authorization': 'Bearer $token',
            },
            body: jsonEncode({'notiDevice': null}),
          );
          debugPrint('✅ FCM token cleared on server');
        }
      } catch (e) {
        debugPrint('⚠️ Could not clear FCM token on server: $e');
      }

      // Xóa từng key riêng lẻ thay vì deleteAll()
      // để giữ lại credentials đã lưu cho "Ghi nhớ đăng nhập"
      await _storage.delete(key: 'jwt_token');
      await _storage.delete(key: 'refresh_token');
      await _storage.delete(key: 'user_code');
      await _googleSignIn.signOut();
      await HceBridge.clearUserId();

      _currentUser = null;
      _currentSetting = null;
      await _localService.clearData();

      notifyListeners();
    } catch (e) {
      debugPrint("Lỗi logout: $e");
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
        body: jsonEncode({'notiDevice': fcmToken}),
      );
      if (response.statusCode == 200) debugPrint("✅ FCM Synced");
    } catch (e) {
      debugPrint("❌ FCM Sync Error: $e");
    }
  }

  // Token storage methods
  Future<void> _saveToken(String token) async =>
      await _storage.write(key: 'jwt_token', value: token);
  Future<String?> _readStoredToken() async =>
      await _storage.read(key: 'jwt_token');
  Future<String?> getToken() async {
    final token = await _readStoredToken();
    if (token == null) return null;

    if (!_isJwtExpired(token)) {
      return token;
    }

    final refreshed = await refreshAccessToken();
    if (refreshed) {
      return await _readStoredToken();
    }

    await _handleSessionExpired();
    return null;
  }

  Future<void> _saveRefreshToken(String token) async =>
      await _storage.write(key: 'refresh_token', value: token);
  Future<String?> getRefreshToken() async =>
      await _storage.read(key: 'refresh_token');
  Future<void> _saveUserCode(String code) async =>
      await _storage.write(key: 'user_code', value: code);

  Future<UserProfile?> getProfile({bool forceRefresh = false}) async {
    if (_currentUser != null && !forceRefresh) return _currentUser;
    await checkLoginStatus();
    return _currentUser;
  }

  /// Refresh access token using refresh token
  Future<bool> refreshAccessToken() async {
    // Tránh nhiều request refresh đồng thời (race condition)
    if (_refreshCompleter != null) {
      return _refreshCompleter!.future;
    }
    _refreshCompleter = Completer<bool>();

    try {
      final refreshToken = await getRefreshToken();
      if (refreshToken == null) {
        _refreshCompleter!.complete(false);
        _refreshCompleter = null;
        return false;
      }

      final response = await http.post(
        Uri.parse('${ApiConstants.authBaseUrl}/refresh'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'refreshToken': refreshToken}),
      );

      if (response.statusCode == 200) {
        final jsonMap = jsonDecode(utf8.decode(response.bodyBytes));
        await _saveToken(jsonMap['accessToken']);
        _refreshCompleter!.complete(true);
        _refreshCompleter = null;
        return true;
      }
      _refreshCompleter!.complete(false);
      _refreshCompleter = null;
      return false;
    } catch (e) {
      debugPrint("❌ Token refresh error: $e");
      _refreshCompleter?.complete(false);
      _refreshCompleter = null;
      return false;
    }
  }

  Completer<bool>? _refreshCompleter;

  /// Lấy token hợp lệ, tự động refresh nếu cần.
  /// Trả về null nếu không thể lấy token (cần đăng nhập lại).
  Future<String?> getValidToken() async {
    final token = await getToken();
    if (token == null) return null;

    // Thử gọi /me để check token còn sống không
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/me'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) return token;

      if (response.statusCode == 401 || response.statusCode == 403) {
        final refreshed = await refreshAccessToken();
        if (refreshed) return await _readStoredToken();
        await _handleSessionExpired();
        return null;
      }

      // Lỗi khác (500, network) - trả token hiện tại, để caller xử lý
      return token;
    } catch (_) {
      return token;
    }
  }

  /// HTTP request tự động kèm auth token và auto-refresh khi 401.
  /// Dùng cho tất cả API call cần xác thực trong app.
  Future<http.Response> authenticatedRequest(
    String method,
    Uri url, {
    Map<String, String>? headers,
    Object? body,
  }) async {
    var token = await getToken();
    final mergedHeaders = {
      if (token != null) 'Authorization': 'Bearer $token',
      ...?headers,
    };

    var response = await _sendRequest(method, url, mergedHeaders, body);

    if (response.statusCode == 401 || response.statusCode == 403) {
      final refreshed = await refreshAccessToken();
      if (refreshed) {
        token = await _readStoredToken();
        mergedHeaders['Authorization'] = 'Bearer $token';
        response = await _sendRequest(method, url, mergedHeaders, body);
      } else {
        await _handleSessionExpired();
      }

      if (response.statusCode == 401 || response.statusCode == 403) {
        await _handleSessionExpired();
      }
    }

    return response;
  }

  bool _isJwtExpired(String token) {
    try {
      final parts = token.split('.');
      if (parts.length != 3) return false;

      final payload = parts[1];
      final normalized = base64Url.normalize(payload);
      final decoded = utf8.decode(base64Url.decode(normalized));
      final map = jsonDecode(decoded);
      if (map is! Map<String, dynamic>) return false;

      final exp = map['exp'];
      if (exp is! num) return false;

      final expiry = DateTime.fromMillisecondsSinceEpoch(
        exp.toInt() * 1000,
        isUtc: true,
      );

      return DateTime.now().toUtc().isAfter(
        expiry.subtract(const Duration(seconds: 15)),
      );
    } catch (_) {
      return false;
    }
  }

  Future<void> _handleSessionExpired() async {
    if (_isHandlingSessionExpiry) return;
    _isHandlingSessionExpiry = true;

    try {
      await logout();

      final navigator = DeepLinkService.navigatorKey.currentState;
      if (navigator != null) {
        navigator.pushAndRemoveUntil(
          MaterialPageRoute(builder: (_) => const OnBoardingScreen()),
          (route) => false,
        );
      }
    } finally {
      _isHandlingSessionExpiry = false;
    }
  }

  Future<http.Response> _sendRequest(
    String method,
    Uri url,
    Map<String, String> headers,
    Object? body,
  ) async {
    switch (method.toUpperCase()) {
      case 'GET':
        return http.get(url, headers: headers);
      case 'POST':
        return http.post(url, headers: headers, body: body);
      case 'PUT':
        return http.put(url, headers: headers, body: body);
      case 'PATCH':
        return http.patch(url, headers: headers, body: body);
      case 'DELETE':
        return http.delete(url, headers: headers, body: body);
      default:
        return http.get(url, headers: headers);
    }
  }

  // ============ FORGOT PASSWORD METHODS ============

  /// Step 1: Send OTP to email for password reset
  Future<Map<String, dynamic>> forgotPassword(String email) async {
    try {
      final response = await http.post(
        Uri.parse('${ApiConstants.domain}/slib/auth/forgot-password'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email.trim().toLowerCase()}),
      );

      final decodedBody = utf8.decode(response.bodyBytes);
      final jsonMap = jsonDecode(decodedBody);

      if (response.statusCode == 200) {
        return jsonMap;
      } else {
        throw Exception(jsonMap['message'] ?? 'Không thể gửi OTP');
      }
    } catch (e) {
      throw Exception(_extractReadableErrorMessage(e));
    }
  }

  /// Step 2: Verify OTP and get reset token
  Future<String> verifyOtp(String email, String otp) async {
    try {
      final response = await http.post(
        Uri.parse('${ApiConstants.domain}/slib/auth/verify-otp'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'email': email.trim().toLowerCase(),
          'token': otp,
          'type': 'recovery',
        }),
      );

      final decodedBody = utf8.decode(response.bodyBytes);
      final jsonMap = jsonDecode(decodedBody);

      if (response.statusCode == 200 && jsonMap['success'] == true) {
        // Extract access token from result string
        final resultStr = jsonMap['result'] as String;
        final resultJson = jsonDecode(resultStr);
        return resultJson['access_token'];
      } else {
        throw Exception(
          jsonMap['message'] ?? 'Mã OTP không đúng hoặc đã hết hạn',
        );
      }
    } catch (e) {
      throw Exception(_extractReadableErrorMessage(e));
    }
  }

  /// Resend OTP
  Future<void> resendOtp(String email) async {
    try {
      final response = await http.post(
        Uri.parse('${ApiConstants.domain}/slib/auth/resend-otp'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'email': email.trim().toLowerCase(),
          'type': 'recovery',
        }),
      );

      if (response.statusCode != 200) {
        final decodedBody = utf8.decode(response.bodyBytes);
        final jsonMap = jsonDecode(decodedBody);
        throw Exception(jsonMap['message'] ?? 'Không thể gửi lại OTP');
      }
    } catch (e) {
      throw Exception(_extractReadableErrorMessage(e));
    }
  }

  /// Step 3: Reset password with token
  Future<void> resetPassword(String token, String newPassword) async {
    try {
      final response = await http.post(
        Uri.parse('${ApiConstants.domain}/slib/auth/update-password'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode({'password': newPassword}),
      );

      if (response.statusCode != 200) {
        final decodedBody = utf8.decode(response.bodyBytes);
        final jsonMap = jsonDecode(decodedBody);
        throw Exception(jsonMap['message'] ?? 'Không thể đặt lại mật khẩu');
      }
    } catch (e) {
      throw Exception(_extractReadableErrorMessage(e));
    }
  }
}
