import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/new_book_model.dart';

class NewBookService {
  static String baseUrl = ApiConstants.newBooksUrl;

  Future<List<NewBook>> fetchPublicNewBooks() async {
    try {
      final response = await http.get(Uri.parse('$baseUrl/public'));

      if (response.statusCode == 200) {
        final body =
            jsonDecode(utf8.decode(response.bodyBytes)) as List<dynamic>;
        return body
            .map((item) => NewBook.fromJson(item as Map<String, dynamic>))
            .toList();
      }

      throw Exception('Failed to load new books');
    } catch (e) {
      throw Exception('Error fetching new books: $e');
    }
  }

  Future<NewBook> fetchNewBookDetail(int id) async {
    final response = await http.get(Uri.parse('$baseUrl/public/$id'));

    if (response.statusCode == 200) {
      return NewBook.fromJson(
        jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>,
      );
    }

    throw Exception('Failed to load new book detail');
  }
}
