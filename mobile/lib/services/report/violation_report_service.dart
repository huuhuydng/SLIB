import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import 'dart:convert';
import '../../core/constants/api_constants.dart';

class ViolationReportService {
  static const String _baseUrl =
      '${ApiConstants.domain}/slib/violation-reports';

  /// Tạo báo cáo vi phạm mới (seatId, violationType, description + images)
  Future<Map<String, dynamic>> createReport({
    required String token,
    required int seatId,
    required String violationType,
    String? description,
    List<File>? images,
  }) async {
    var uri = Uri.parse(_baseUrl);
    var request = http.MultipartRequest('POST', uri);

    request.headers['Authorization'] = 'Bearer $token';
    request.fields['seatId'] = seatId.toString();
    request.fields['violationType'] = violationType;
    if (description != null && description.isNotEmpty) {
      request.fields['description'] = description;
    }

    if (images != null) {
      for (int i = 0; i < images.length; i++) {
        final file = images[i];
        final ext = file.path.split('.').last.toLowerCase();
        final mimeType = ext == 'png' ? 'image/png' : 'image/jpeg';

        request.files.add(await http.MultipartFile.fromPath(
          'images',
          file.path,
          contentType: MediaType.parse(mimeType),
        ));
      }
    }

    final streamedResponse = await request.send();
    final response = await http.Response.fromStream(streamedResponse);

    if (response.statusCode == 201) {
      return json.decode(response.body);
    } else {
      debugPrint('[ViolationReport] POST failed with status ${response.statusCode}');
      throw Exception(
          'Gửi báo cáo thất bại: ${response.statusCode}');
    }
  }

  /// Lấy danh sách báo cáo của sinh viên
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
    } else {
      throw Exception(
          'Lấy báo cáo thất bại: ${response.statusCode}');
    }
  }
}
