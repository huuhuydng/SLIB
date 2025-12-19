import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/auth_response.dart';

class AuthService {
  // ⚠️ LƯU Ý QUAN TRỌNG VỀ IP:
  // - Nếu chạy máy ảo Android: Dùng '10.0.2.2'
  // - Nếu chạy máy ảo iOS: Dùng '127.0.0.1' hoặc 'localhost'
  // - Nếu chạy thiết bị thật: Dùng IP LAN của máy tính (VD: 192.168.1.x)
  static String getBaseUrl() {
    if (Platform.isAndroid) {
      return "http://10.0.2.2:8080/slib/users";
    } else {
      return "http://127.0.0.1:8080/slib/users";
    }
  }

  static String baseUrl = getBaseUrl();

  // Khởi tạo kho lưu trữ bảo mật (lưu Token, v.v.)
  final _storage = const FlutterSecureStorage();

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
        // Lưu Token vào kho
        await _saveToken(authData.accessToken);

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
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/verify'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email, 'token': otp}),
      );

      if (response.statusCode == 200) {
        final jsonMap = jsonDecode(response.body);

        final authData = AuthResponse.fromJson(jsonMap);
        // Lưu Token vào kho
        await _saveToken(authData.accessToken);

        return true;
      } else {
        throw Exception("Mã OTP không chính xác");
      }
    } catch (e) {
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

  // Lấy Token ra để dùng
  Future<String?> getToken() async {
    return await _storage.read(key: 'jwt_token');
  }

  // Đăng xuất (Xóa Token)
  Future<void> logout() async {
    await _storage.delete(key: 'jwt_token');
  }
}
