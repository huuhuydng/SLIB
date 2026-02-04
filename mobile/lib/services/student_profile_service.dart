import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/student_profile.dart';
import 'package:slib/services/auth_service.dart';

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
      print("❌ Error fetching student profile: $e");
      return null;
    }
  }
}
