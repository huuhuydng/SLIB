import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'package:slib/core/constants/api_constants.dart';

class KioskService {
  static const String _baseUrl = ApiConstants.kioskUrl;
  static const _storage = FlutterSecureStorage();

  static Future<String?> _getToken() async =>
      await _storage.read(key: 'jwt_token');

  /// Validate QR code from kiosk
  /// Returns session token if successful
  static Future<Map<String, dynamic>> validateQr(String qrPayload, String kioskCode) async {
    try {
      final token = await _getToken();
      if (token == null) {
        throw Exception('Not authenticated');
      }

      final response = await http.post(
        Uri.parse('$_baseUrl/qr/validate'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode({
          'qrPayload': qrPayload,
          'kioskCode': kioskCode,
        }),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
        final error = jsonDecode(response.body);
        throw Exception(error['message'] ?? 'Validate failed');
      }
    } catch (e) {
      rethrow;
    }
  }

  /// Complete session after QR validation
  /// This will create check-in record and return session info
  /// [userId] - The current user's ID
  static Future<Map<String, dynamic>> completeSession(String sessionToken, String userId) async {
    try {
      final token = await _getToken();
      if (token == null) {
        throw Exception('Not authenticated');
      }

      final response = await http.post(
        Uri.parse('$_baseUrl/session/complete'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode({
          'sessionToken': sessionToken,
          'userId': userId,
        }),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
        final error = jsonDecode(response.body);
        throw Exception(error['message'] ?? 'Complete session failed');
      }
    } catch (e) {
      rethrow;
    }
  }

  /// Get current active session for kiosk
  static Future<Map<String, dynamic>?> getActiveSession(String kioskCode) async {
    try {
      final token = await _getToken();
      if (token == null) {
        return null;
      }

      final response = await http.get(
        Uri.parse('$_baseUrl/session/$kioskCode'),
        headers: {
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      } else if (response.statusCode == 404) {
        return null;
      } else {
        return null;
      }
    } catch (e) {
      return null;
    }
  }

  /// Check out from kiosk
  static Future<void> checkOut(String sessionToken) async {
    try {
      final token = await _getToken();
      if (token == null) {
        throw Exception('Not authenticated');
      }

      final response = await http.post(
        Uri.parse('$_baseUrl/session/checkout'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode({
          'sessionToken': sessionToken,
        }),
      );

      if (response.statusCode != 200) {
        final error = jsonDecode(response.body);
        throw Exception(error['message'] ?? 'Check-out failed');
      }
    } catch (e) {
      rethrow;
    }
  }

  /// Check-out từ mobile app (không cần sessionToken)
  /// Tìm AccessLog active theo userId và đóng
  static Future<void> checkOutMobile(String userId) async {
    try {
      final token = await _getToken();
      if (token == null) {
        throw Exception('Not authenticated');
      }

      final response = await http.post(
        Uri.parse('$_baseUrl/session/checkout-mobile'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode({
          'userId': userId,
        }),
      );

      if (response.statusCode != 200) {
        final error = jsonDecode(response.body);
        throw Exception(error['message'] ?? 'Check-out failed');
      }
    } catch (e) {
      rethrow;
    }
  }

  /// Kiểm tra trạng thái check-in hiện tại
  static Future<bool> checkStatus(String userId) async {
    try {
      final token = await _getToken();
      if (token == null) return false;

      final response = await http.get(
        Uri.parse('$_baseUrl/session/check-status/$userId'),
        headers: {
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return data['isCheckedIn'] == true;
      }
      return false;
    } catch (e) {
      return false;
    }
  }
}
