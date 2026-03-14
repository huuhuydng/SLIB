import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import '../../core/constants/api_constants.dart';

class SeatStatusReportService {
  static const String _baseUrl =
      '${ApiConstants.domain}/slib/seat-status-reports';

  Future<Map<String, dynamic>> createReport({
    required String token,
    required int seatId,
    required String issueType,
    String? description,
    File? image,
  }) async {
    final request = http.MultipartRequest('POST', Uri.parse(_baseUrl));
    request.headers['Authorization'] = 'Bearer $token';
    request.fields['seatId'] = seatId.toString();
    request.fields['issueType'] = issueType;
    if (description != null && description.trim().isNotEmpty) {
      request.fields['description'] = description.trim();
    }
    if (image != null) {
      final ext = image.path.split('.').last.toLowerCase();
      final mimeType = ext == 'png' ? 'image/png' : 'image/jpeg';
      request.files.add(
        await http.MultipartFile.fromPath(
          'image',
          image.path,
          contentType: MediaType.parse(mimeType),
        ),
      );
    }

    final streamed = await request.send();
    final response = await http.Response.fromStream(streamed);
    if (response.statusCode == 201) {
      return json.decode(response.body);
    }
    throw Exception(
      'Gửi báo cáo tình trạng ghế thất bại: ${response.statusCode}',
    );
  }

  Future<List<dynamic>> getMyReports(String token) async {
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
    throw Exception(
      'Lấy lịch sử báo cáo tình trạng ghế thất bại: ${response.statusCode}',
    );
  }
}
