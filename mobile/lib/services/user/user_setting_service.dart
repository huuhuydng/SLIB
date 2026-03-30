import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:slib/core/constants/api_constants.dart';
import '../../models/user_setting.dart';

class UserSettingService {
  static const String baseUrl = ApiConstants.settingUrl;
  final _storage = const FlutterSecureStorage();

  Future<String?> _getToken() async => await _storage.read(key: 'jwt_token');

  Future<Map<String, String>> _authHeaders({bool json = false}) async {
    final token = await _getToken();
    return {
      if (token != null) 'Authorization': 'Bearer $token',
      if (json) 'Content-Type': 'application/json',
    };
  }

  Future<UserSetting> getSettings(String userId) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/$userId'),
        headers: await _authHeaders(),
      );
      debugPrint(
        'Fetch settings response: ${response.statusCode}, body: ${response.body}',
      );
      if (response.statusCode == 200) {
        return UserSetting.fromJson(
          jsonDecode(utf8.decode(response.bodyBytes)),
        );
      } else {
        throw Exception('Lỗi tải cài đặt: ${response.body}');
      }
    } catch (e) {
      throw Exception('Lỗi kết nối: $e');
    }
  }

  Future<UserSetting> updateSettings(
    String userId,
    UserSetting newSetting,
  ) async {
    try {
      final response = await http.put(
        Uri.parse('$baseUrl/$userId'),
        headers: await _authHeaders(json: true),
        body: jsonEncode(newSetting.toJson()),
      );

      if (response.statusCode == 200) {
        return UserSetting.fromJson(
          jsonDecode(utf8.decode(response.bodyBytes)),
        );
      } else {
        throw Exception('Lỗi cập nhật: ${response.body}');
      }
    } catch (e) {
      throw Exception('Lỗi kết nối: $e');
    }
  }
}
