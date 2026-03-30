import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:slib/models/new_book_model.dart';
import 'package:slib/models/news_model.dart';
import '../../models/user_setting.dart';

class LocalStorageService {
  static const String _keySettings = "user_settings_cache";
  static const String _keyNewsCache = "news_list_cache";
  static const String _keyNewBooksCache = "new_books_list_cache";
  static const String _keyRememberMe = "remember_me";
  static const String _keyCredIdentifier = "saved_identifier";
  static const String _keyCredPassword = "saved_password";

  final _secureStorage = const FlutterSecureStorage();

  // ============ GHI NHỚ ĐĂNG NHẬP ============

  /// Lưu trạng thái checkbox "Ghi nhớ đăng nhập"
  Future<void> saveRememberMe(bool value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_keyRememberMe, value);
  }

  /// Đọc trạng thái checkbox "Ghi nhớ đăng nhập"
  Future<bool> loadRememberMe() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_keyRememberMe) ?? false;
  }

  /// Lưu tài khoản và mật khẩu (mã hóa an toàn)
  Future<void> saveCredentials(String identifier, String password) async {
    await _secureStorage.write(key: _keyCredIdentifier, value: identifier);
    await _secureStorage.write(key: _keyCredPassword, value: password);
  }

  /// Đọc tài khoản và mật khẩu đã lưu
  Future<Map<String, String>?> loadCredentials() async {
    final identifier = await _secureStorage.read(key: _keyCredIdentifier);
    final password = await _secureStorage.read(key: _keyCredPassword);

    if (identifier != null && password != null) {
      return {'identifier': identifier, 'password': password};
    }
    return null;
  }

  /// Xóa tài khoản và mật khẩu đã lưu
  Future<void> clearCredentials() async {
    await _secureStorage.delete(key: _keyCredIdentifier);
    await _secureStorage.delete(key: _keyCredPassword);
  }

  Future<void> saveSettings(UserSetting setting) async {
    final prefs = await SharedPreferences.getInstance();
    String jsonString = jsonEncode(setting.toJson());
    await prefs.setString(_keySettings, jsonString);
  }

  Future<UserSetting?> loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    String? jsonString = prefs.getString(_keySettings);

    if (jsonString == null) return null;

    try {
      return UserSetting.fromJson(jsonDecode(jsonString));
    } catch (e) {
      return null;
    }
  }

  Future<void> clearData() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_keySettings);
  }

  Future<void> saveNewsList(List<News> newsList) async {
    final prefs = await SharedPreferences.getInstance();
    List<Map<String, dynamic>> jsonList = newsList
        .map((news) => news.toJson())
        .toList();
    String jsonString = jsonEncode(jsonList);

    await prefs.setString(_keyNewsCache, jsonString);
  }

  Future<List<News>> loadNewsList() async {
    final prefs = await SharedPreferences.getInstance();
    String? jsonString = prefs.getString(_keyNewsCache);

    if (jsonString == null) return [];

    try {
      List<dynamic> decodedList = jsonDecode(jsonString);
      List<News> newsList = decodedList
          .map((item) => News.fromJson(item))
          .toList();
      return newsList;
    } catch (e) {
      return [];
    }
  }

  Future<void> saveNewBooksList(List<NewBook> newBooks) async {
    final prefs = await SharedPreferences.getInstance();
    final jsonList = newBooks.map((book) => book.toJson()).toList();
    final jsonString = jsonEncode(jsonList);

    await prefs.setString(_keyNewBooksCache, jsonString);
  }

  Future<List<NewBook>> loadNewBooksList() async {
    final prefs = await SharedPreferences.getInstance();
    final jsonString = prefs.getString(_keyNewBooksCache);

    if (jsonString == null) return [];

    try {
      final decodedList = jsonDecode(jsonString) as List<dynamic>;
      return decodedList
          .map((item) => NewBook.fromJson(item as Map<String, dynamic>))
          .toList();
    } catch (e) {
      return [];
    }
  }
}
