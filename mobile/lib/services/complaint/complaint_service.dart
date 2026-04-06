import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:slib/core/constants/api_constants.dart';

class ComplaintService {
  static const String _baseUrl = ApiConstants.complaintUrl;

  Future<List<dynamic>> getMyComplaints(String token) async {
    final response = await http.get(
      Uri.parse('$_baseUrl/my'),
      headers: {
        'Authorization': 'Bearer $token',
        'Content-Type': 'application/json',
      },
    );

    if (response.statusCode == 200) {
      return json.decode(response.body);
    }

    throw Exception('Lấy lịch sử khiếu nại thất bại: ${response.statusCode}');
  }
}
