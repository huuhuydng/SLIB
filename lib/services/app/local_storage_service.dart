import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:slib/models/news_model.dart';
import '../../models/user_setting.dart';

class LocalStorageService {
  static const String _keySettings = "user_settings_cache";
  static const String _keyNewsCache = "news_list_cache";

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
    List<Map<String, dynamic>> jsonList = newsList.map((news) => news.toJson()).toList();
    String jsonString = jsonEncode(jsonList);
    
    await prefs.setString(_keyNewsCache, jsonString);
  }

  Future<List<News>> loadNewsList() async {
    final prefs = await SharedPreferences.getInstance();
    String? jsonString = prefs.getString(_keyNewsCache);

    if (jsonString == null) return []; 

    try {
      List<dynamic> decodedList = jsonDecode(jsonString);
      List<News> newsList = decodedList.map((item) => News.fromJson(item)).toList();
      return newsList;
    } catch (e) {
      return [];
    }
  }
}