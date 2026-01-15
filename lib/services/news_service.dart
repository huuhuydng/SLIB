import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/news_model.dart';
import 'package:slib/services/app/local_storage_service.dart';

class NewsService {
  static String baseUrl = ApiConstants.newsUrl; 
  final LocalStorageService _localService = LocalStorageService();

  Future<List<News>> fetchPublicNews() async {
    try {
      final response = await http.get(Uri.parse('$baseUrl/public'));

      if (response.statusCode == 200) {
        List<dynamic> body = jsonDecode(utf8.decode(response.bodyBytes)); 
        return body.map((dynamic item) => News.fromJson(item)).toList();
      } else {
        throw Exception('Failed to load news');
      }
    } catch (e) {
      throw Exception('Error fetching news: $e');
    }
  }

  Future<News> fetchNewsDetail(int id) async {
    final response = await http.get(Uri.parse('$baseUrl/public/detail/$id'));
    if (response.statusCode == 200) {
      return News.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
    } else {
      throw Exception('Failed to load news detail');
    }
  }
}