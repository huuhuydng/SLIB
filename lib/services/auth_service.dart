import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/services/hce_bridge.dart';
import '../models/auth_response.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

class AuthService {

  static const String baseUrl = ApiConstants.authUrl;
  UserProfile? currentUser;

  // Khởi tạo kho lưu trữ bảo mật (lưu Token, v.v.)
  final _storage = const FlutterSecureStorage();
  static const _studentCodeKey = 'student_code';
  static const _savedEmailKey = 'saved_email';
  static const _savedPasswordKey = 'saved_password';

  // 1. Hàm Đăng Nhập
  Future<AuthResponse?> login(String email, String password) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email, 'password': password}),
      );

      if (response.statusCode == 200) {
        final jsonMap = jsonDecode(response.body);

        final authData = AuthResponse.fromJson(jsonMap);

        if (authData.role != 'STUDENT') {
          await logout(); 
          throw Exception('Tài khoản này không phải là Sinh viên!');
        }

        // Lưu Token vào kho
        await _saveToken(authData.accessToken);
        await _saveStudentCode(authData.studentCode);
        await HceBridge.setStudentCode(authData.studentCode);

        syncFcmToken(authData.id);

        return authData;
      } else {
        throw Exception('Đăng nhập thất bại: ${response.body}');
      }
    } catch (e) {
      print('Lỗi login: $e');
      rethrow; // Ném lỗi ra để UI hiển thị thông báo
    }
  }

  // 2. Hàm Đăng Ký
  Future<bool> register({
    required String email,
    required String password,
    required String fullName,
    required String studentCode,
    required String dob, // Format: YYYY-MM-DD
  }) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/register'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'email': email,
          'password': password,
          'fullName': fullName,
          'studentCode': studentCode,
          'dob': dob,
        }),
      );

      if (response.statusCode == 200) {
        return true;
      } else {
        throw Exception(response.body);
      }
    } catch (e) {
      rethrow;
    }
  }

  // 3. Hàm Xác Thực OTP
  Future<bool> verifyOtp(String email, String otp) async {
    // 1. Clean dữ liệu trước khi gửi
    final cleanEmail = email.trim().toLowerCase(); 
    final cleanOtp = otp.trim();
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/verify'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'email': cleanEmail,
          'token': cleanOtp,
          'type': 'signup',
        }),
      );
      if (response.statusCode == 200) {
        // Parse kết quả
        final jsonMap = jsonDecode(response.body);
        final authData = AuthResponse.fromJson(jsonMap);
        
        // Lưu Token
        await _saveToken(authData.accessToken);
        await _saveStudentCode(authData.studentCode);
        await HceBridge.setStudentCode(authData.studentCode);

        syncFcmToken(authData.id);

        return true;
      } else {
        // ⚠️ QUAN TRỌNG: Lấy lỗi thật từ Server để hiển thị
        // Ví dụ server trả: "Mã xác thực không đúng hoặc đã hết hạn!"
        throw Exception(response.body); 
      }
    } catch (e) {
      print("Lỗi Verify Dart: $e");
      rethrow;
    }
  }

  // 3.1 Hàm Gửi Lại OTP
  Future<void> resendOtp(String email) async {
    final cleanEmail = email.trim().toLowerCase();
    try {
      // Gọi lại API register để gửi OTP mới
      // Hoặc nếu backend có endpoint riêng /resend-otp thì dùng endpoint đó
      final response = await http.post(
        Uri.parse('$baseUrl/resend-otp'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': cleanEmail, 'type': 'signup'}),
      );
      
      if (response.statusCode == 200) {
        return;
      } else {
        throw Exception('Không thể gửi lại OTP: ${response.body}');
      }
    } catch (e) {
      print("Lỗi Resend OTP: $e");
      rethrow;
    }
  }

  // 3.2 Hàm Quên Mật Khẩu - Gửi OTP về email
  Future<void> forgotPassword(String email) async {
    final cleanEmail = email.trim().toLowerCase();
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/forgot-password'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': cleanEmail}),
      );
      
      if (response.statusCode == 200) {
        return;
      } else {
        throw Exception('Không thể gửi mã OTP: ${response.body}');
      }
    } catch (e) {
      print("Lỗi Forgot Password: $e");
      rethrow;
    }
  }

  // 3.3 Verify OTP Recovery và lấy token
  Future<String> verifyRecoveryOtp(String email, String otp) async {
    final cleanEmail = email.trim().toLowerCase();
    final cleanOtp = otp.trim();
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/verify'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'email': cleanEmail,
          'token': cleanOtp,
          'type': 'recovery', // Khác với signup
        }),
      );
      
      if (response.statusCode == 200) {
        final jsonMap = jsonDecode(response.body);
        // Lấy accessToken từ response
        return jsonMap['accessToken'] as String;
      } else {
        throw Exception('Mã OTP không đúng hoặc đã hết hạn');
      }
    } catch (e) {
      print("Lỗi Verify Recovery OTP: $e");
      rethrow;
    }
  }

  // 3.4 Đặt lại mật khẩu với token
  Future<void> resetPasswordWithToken(String token, String newPassword) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/reset-password'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode({'password': newPassword}),
      );
      
      if (response.statusCode == 200) {
        return;
      } else {
        throw Exception('Không thể đặt lại mật khẩu: ${response.body}');
      }
    } catch (e) {
      print("Lỗi Reset Password With Token: $e");
      rethrow;
    }
  }

  // 4. Hàm Kiểm Tra Trạng Thái Đăng Nhập
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
        // 1. Parse dữ liệu luôn (Đừng vứt đi nữa!)
        // Lưu ý decode utf8 để không lỗi font tiếng Việt
        final decodedBody = utf8.decode(response.bodyBytes);
        final jsonMap = jsonDecode(decodedBody);
        
        // Lưu vào biến toàn cục của class AuthService
        currentUser = UserProfile.fromJson(jsonMap);

        // 2. Setup HCE (Logic cũ của bạn)
        final studentCode = await _getStudentCode();
        if (studentCode != null && studentCode.isNotEmpty) {
          await HceBridge.setStudentCode(studentCode);
        } else {
            // Nếu local mất studentCode, lấy từ API bù vào luôn cho chắc
            await HceBridge.setStudentCode(currentUser!.studentCode);
            await _saveStudentCode(currentUser!.studentCode);
        }

        // 3. Tiện tay Sync luôn FCM Token (Vì đã có ID từ currentUser)
        // Gọi không cần await để App vào nhanh, cái này chạy ngầm
        syncFcmToken(currentUser!.id);

        return true;
      } else {
        // Token hết hạn hoặc lỗi
        await logout();
        return false;
      }
    } catch (e) {
      print("Check login error: $e");
      return false;
    }
  }

  // 5. Đồng Bộ FCM Token Lên Backend
  Future<void> syncFcmToken(String userId) async {
    try {
      String? fcmToken = await FirebaseMessaging.instance.getToken();
      
      if (fcmToken == null) {
        print("Không lấy được FCM Token");
        return;
      }

      print("FCM Token: $fcmToken");

      String? jwt = await getToken();
      if (jwt == null) return;

      //update user
      // URL: PUT /slib/users/update/{id}
      final response = await http.put(
        Uri.parse('$baseUrl/update/$userId'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $jwt',
        },
        body: jsonEncode({
          'notiDevice': fcmToken, // Đổi thành camelCase để khớp Java Entity
        }),
      );
      if (response.statusCode == 200) {
        print("Đã cập nhật noti_device thành công!");
      } else {
        print("Lỗi update FCM: ${response.body}");
      }
    } catch (e) {
      print("Exception syncFcmToken: $e");
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
        return UserProfile.fromJson(jsonMap);
      } else {
        print("Lỗi lấy profile: ${response.body}");
        return null;
      }
    } catch (e) {
      print("Exception getProfile: $e");
      return null;
    }
  }

  // Lưu Token vào "két sắt"
  Future<void> _saveToken(String token) async {
    await _storage.write(key: 'jwt_token', value: token);
  }

  Future<void> _saveStudentCode(String code) async {
    await _storage.write(key: _studentCodeKey, value: code);
  }

  Future<String?> _getStudentCode() async {
    return _storage.read(key: _studentCodeKey);
  }

  // Lấy Token ra để dùng
  Future<String?> getToken() async {
    return await _storage.read(key: 'jwt_token');
  }

  // Đăng xuất (Xóa Token)
  Future<void> logout() async {
    await _storage.delete(key: 'jwt_token');
    await _storage.delete(key: _studentCodeKey);
    await clearSavedCredentials(); // Xóa cả email/password đã lưu
    await HceBridge.clearStudentCode();
  }

  // === CHỨC NĂNG GHI NHỚ ĐĂNG NHẬP ===
  
  // Lưu credentials (email + password)
  Future<void> saveCredentials(String email, String password) async {
    await _storage.write(key: _savedEmailKey, value: email);
    await _storage.write(key: _savedPasswordKey, value: password);
  }

  // Lấy credentials đã lưu
  Future<Map<String, String>?> getSavedCredentials() async {
    final email = await _storage.read(key: _savedEmailKey);
    final password = await _storage.read(key: _savedPasswordKey);
    
    if (email != null && password != null) {
      return {'email': email, 'password': password};
    }
    return null;
  }

  // Xóa credentials đã lưu
  Future<void> clearSavedCredentials() async {
    await _storage.delete(key: _savedEmailKey);
    await _storage.delete(key: _savedPasswordKey);
  }
}
