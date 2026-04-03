import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import 'dart:convert';
import '../core/constants/api_constants.dart';

class SupportRequestService {
  static const String _baseUrl = '${ApiConstants.domain}/slib/support-requests';

  /// Gui yeu cau ho tro moi (description + images)
  Future<Map<String, dynamic>> createRequest({
    required String token,
    required String description,
    List<File>? images,
  }) async {
    var uri = Uri.parse(_baseUrl);
    var request = http.MultipartRequest('POST', uri);
    
    request.headers['Authorization'] = 'Bearer $token';
    request.fields['description'] = description;

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
      throw Exception('Failed to create support request: ${response.statusCode}');
    }
  }

  /// Lay danh sach yeu cau cua sinh vien
  Future<List<dynamic>> getMyRequests(String token) async {
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
      throw Exception('Failed to get support requests: ${response.statusCode}');
    }
  }
}
