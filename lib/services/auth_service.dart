import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/services/hce_bridge.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
// import 'auth_response.dart'; // Import model response của bạn

class AuthService extends ChangeNotifier {
  
  // Cấu hình Google
  static const String _webClientId = '262933313086-mhbevhu0b7hfqekchf6a99vnebjfr8b5.apps.googleusercontent.com';
  
  // URL Backend
  static String getBaseUrl() {
    return "https://hyperscrupulous-ropeable-alverta.ngrok-free.dev/slib/users";
  }
  static String baseUrl = getBaseUrl();

  final GoogleSignIn _googleSignIn = GoogleSignIn(
    serverClientId: _webClientId, 
    scopes: ['email', 'profile'],
  );

  final _storage = const FlutterSecureStorage();
  UserProfile? currentUser;

  // --- 1. CHỨC NĂNG CHÍNH: LOGIN GOOGLE ---
  Future<UserProfile?> signInWithGoogle() async {
    try {
      // 1. Trigger Google Sign In
      final GoogleSignInAccount? googleUser = await _googleSignIn.signIn();
      
      if (googleUser == null) return null;

      // 2. Check đuôi email FPT
      if (!googleUser.email.toLowerCase().endsWith('@fpt.edu.vn')) {
        await _googleSignIn.signOut();
        throw Exception('Vui lòng sử dụng email sinh viên FPT (@fpt.edu.vn)!');
      }

      // 👉 LẤY HỌ TÊN (Không lấy avatar nữa)
      // Nếu google không có tên hiển thị, lấy tạm phần đầu email làm tên
      String googleName = googleUser.displayName ?? googleUser.email.split('@')[0];

      // 3. Lấy Token
      final GoogleSignInAuthentication googleAuth = await googleUser.authentication;
      final String? idToken = googleAuth.idToken;

      if (idToken == null) {
        throw Exception('Không lấy được ID Token từ Google.');
      }

      // 4. Gửi về Backend (Chỉ gửi Token và Tên)
      final response = await http.post(
        Uri.parse('$baseUrl/login-google'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'id_token': idToken,
          'full_name': googleName, // Backend nhớ hứng trường này để INSERT vào bảng users
        }),
      );

      if (response.statusCode == 200) {
        // Dùng utf8.decode để tránh lỗi font tiếng Việt khi parse
        final decodedBody = utf8.decode(response.bodyBytes);
        final jsonMap = jsonDecode(decodedBody);
        
        // 1. Lấy Token
        final String accessToken = jsonMap['access_token'];
        await _saveToken(accessToken);

        // 2. Lấy User Info (Key là 'user' như Backend trả về)
        if (jsonMap['user'] != null) {
          currentUser = UserProfile.fromJson(jsonMap['user']);
          
          // Lưu MSSV để dùng cho HCE
          await _saveStudentCode(currentUser!.studentCode);
          await HceBridge.setStudentCode(currentUser!.studentCode);
          syncFcmToken(currentUser!.id);
        }

        notifyListeners();
        return currentUser;
      } else {
        throw Exception('Đăng nhập thất bại với mã lỗi: ${response.statusCode}');
      }
    } catch (e) {
      if (e.toString().contains("Vui lòng sử dụng email")) rethrow;
      print('Google Sign-In Error: $e');
      throw Exception('Đăng nhập thất bại. Vui lòng thử lại.');
    }
  }

  // --- 2. KIỂM TRA TRẠNG THÁI ĐĂNG NHẬP (Splash Screen dùng) ---
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

        // Đảm bảo sync lại student code vào Native
        if (currentUser != null) {
          await HceBridge.setStudentCode(currentUser!.studentCode);
          syncFcmToken(currentUser!.id);
        }
        
        notifyListeners();
        return true;
      } else {
        await logout(); // Token hết hạn -> Logout
        return false;
      }
    } catch (e) {
      print("Check login error: $e");
      return false; // Coi như chưa login nếu lỗi mạng để user đăng nhập lại
    }
  }

  Future<UserProfile?> getProfile({bool forceRefresh = false}) async {
    try {
      // 1. Nếu đã có dữ liệu và không bắt buộc tải lại -> Trả về luôn (nhanh)
      if (currentUser != null && !forceRefresh) {
        return currentUser;
      }

      // 2. Nếu chưa có -> Lấy Token để gọi API
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
        // Decode UTF-8 để hiển thị tiếng Việt không lỗi
        final decodedBody = utf8.decode(response.bodyBytes);
        final jsonMap = jsonDecode(decodedBody);
        
        // Cập nhật biến currentUser
        currentUser = UserProfile.fromJson(jsonMap);
        notifyListeners(); // Báo cho UI cập nhật
        return currentUser;
      }
    } catch (e) {
      print("Lỗi getProfile: $e");
    }
    return null;
  }


  // --- 3. TIỆN ÍCH (Logout, Token, FCM) ---

  Future<void> logout() async {
    // Xóa sạch mọi thứ
    await _storage.deleteAll(); 
    await _googleSignIn.signOut();
    await HceBridge.clearStudentCode();
    currentUser = null;
    notifyListeners();
  }

  Future<void> syncFcmToken(String userId) async {
    try {
      String? fcmToken = await FirebaseMessaging.instance.getToken();
      String? jwt = await getToken();
      if (fcmToken == null || jwt == null) return;

      // API update FCM token
      await http.put(
        Uri.parse('$baseUrl/update/$userId'), // Hoặc endpoint riêng update-fcm
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $jwt',
        },
        body: jsonEncode({'noti_device': fcmToken}), // Khớp với DB snake_case
      );
    } catch (e) {
      print("FCM Sync Error: $e");
    }
  }

  // --- CÁC HÀM GET/SET LOCAL ---
  Future<void> _saveToken(String token) async => 
      await _storage.write(key: 'jwt_token', value: token);

  Future<String?> getToken() async => 
      await _storage.read(key: 'jwt_token');

  Future<void> _saveStudentCode(String code) async => 
      await _storage.write(key: 'student_code', value: code);

  // --- Placeholder cho FEID sau này ---
  Future<void> signInWithFEID() async {
    // TODO: Sau này có API FEID thì triển khai ở đây
    print("Tính năng đang phát triển...");
  }
}