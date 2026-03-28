import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/student_profile.dart';
import 'package:slib/services/auth/auth_service.dart';

class StudentProfileService {
  final AuthService _authService;

  StudentProfileService(this._authService);

  /// Fetch current user's student profile
  Future<StudentProfile?> getMyProfile() async {
    try {
      final token = await _authService.getToken();
      if (token == null) return null;

      final response = await http.get(
        Uri.parse('${ApiConstants.studentProfileUrl}/me'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        final decodedBody = utf8.decode(response.bodyBytes);
        final jsonMap = jsonDecode(decodedBody);
        return StudentProfile.fromJson(jsonMap);
      }
      return null;
    } catch (e) {
      print("Error fetching student profile: $e");
      return null;
    }
  }

  /// Update profile info (fullName, phone, dob)
  Future<bool> updateProfile({
    String? fullName,
    String? phone,
    String? dob,
  }) async {
    try {
      final token = await _authService.getToken();
      if (token == null) return false;

      final body = <String, dynamic>{};
      if (fullName != null) body['fullName'] = fullName;
      if (phone != null) body['phone'] = phone;
      if (dob != null) body['dob'] = dob;

      final response = await http.put(
        Uri.parse('${ApiConstants.studentProfileUrl}/me'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode(body),
      );

      return response.statusCode == 200;
    } catch (e) {
      print("Error updating profile: $e");
      return false;
    }
  }

  /// Upload avatar
  Future<String?> uploadAvatar(File file) async {
    try {
      final token = await _authService.getToken();
      if (token == null) return null;

      final uri = Uri.parse('${ApiConstants.studentProfileUrl}/me/avatar');
      final request = http.MultipartRequest('POST', uri);
      
      request.headers['Authorization'] = 'Bearer $token';
      
      final ext = file.path.split('.').last.toLowerCase();
      final mimeType = ext == 'png' ? 'image/png' : 'image/jpeg';
      
      request.files.add(await http.MultipartFile.fromPath(
        'file',
        file.path,
        contentType: MediaType.parse(mimeType),
      ));

      final streamedResponse = await request.send();
      final response = await http.Response.fromStream(streamedResponse);

      if (response.statusCode == 200) {
        final json = jsonDecode(utf8.decode(response.bodyBytes));
        // Return new avatar URL from response
        return json['avtUrl'] as String?;
      }
      return null;
    } catch (e) {
      print("Error uploading avatar: $e");
      return null;
    }
  }
}
