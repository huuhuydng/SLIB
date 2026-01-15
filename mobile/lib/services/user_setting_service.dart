import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:slib/core/constants/api_constants.dart';
import '../models/user_setting.dart';

class UserSettingService {
  static const String baseUrl = ApiConstants.settingUrl; 

  Future<UserSetting> getSettings(String userId) async {
    try {
      final response = await http.get(Uri.parse('$baseUrl/$userId'));
      print('Fetch settings response: ${response.statusCode}, body: ${response.body}');
      if (response.statusCode == 200) {
        return UserSetting.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
      } else {
        throw Exception('Lỗi tải cài đặt: ${response.body}');
      }
    } catch (e) {
      throw Exception('Lỗi kết nối: $e');
    }
  }

  Future<UserSetting> updateSettings(String userId, UserSetting newSetting) async {
    try {
      final response = await http.put(
        Uri.parse('$baseUrl/$userId'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(newSetting.toJson()),
      );

      if (response.statusCode == 200) {
        return UserSetting.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
      } else {
        throw Exception('Lỗi cập nhật: ${response.body}');
      }
    } catch (e) {
      throw Exception('Lỗi kết nối: $e');
    }
  }
}