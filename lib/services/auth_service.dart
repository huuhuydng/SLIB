import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:slib/services/hce_bridge.dart';
import '../models/auth_response.dart';

class AuthService {
  // ⚠️ LƯU Ý QUAN TRỌNG VỀ IP:
  // - Nếu chạy máy ảo Android: Dùng '10.0.2.2'
  // - Nếu chạy máy ảo iOS: Dùng '127.0.0.1' hoặc 'localhost'
  // - Nếu chạy thiết bị thật: Dùng IP LAN của máy tính (VD: 192.168.1.x)
  static String getBaseUrl() {
    return "https://hyperscrupulous-ropeable-alverta.ngrok-free.dev/slib/users";
  }

  static String baseUrl = getBaseUrl();

  // Khởi tạo kho lưu trữ bảo mật (lưu Token, v.v.)
  final _storage = const FlutterSecureStorage();
  static const _studentCodeKey = 'student_code';

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

  // 4. Hàm Kiểm Tra Trạng Thái Đăng Nhập
  Future<bool> checkLoginStatus() async {
    String? token = await getToken();
    if (token == null) {
      return false;
    }
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/me'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );
      if (response.statusCode == 200) {
        // Nếu đã đăng nhập sẵn, khôi phục mã SV cho HCE (nếu có lưu)
        final studentCode = await _getStudentCode();
        if (studentCode != null && studentCode.isNotEmpty) {
          await HceBridge.setStudentCode(studentCode);
        }
        return true;
      } else {
        await logout();
        return false;
      }
    } catch (e) {
      return false;
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
    await HceBridge.clearStudentCode();
  }
}
