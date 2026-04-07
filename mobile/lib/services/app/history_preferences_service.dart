import 'package:shared_preferences/shared_preferences.dart';

class HistoryPreferencesService {
  static const String _hiddenKeyPrefix = 'history_hidden';

  Future<Set<String>> loadHiddenIds({
    required String scope,
    required String userId,
  }) async {
    final prefs = await SharedPreferences.getInstance();
    return (prefs.getStringList(_buildKey(scope, userId)) ?? <String>[])
        .toSet();
  }

  Future<void> hideItem({
    required String scope,
    required String userId,
    required String itemId,
  }) async {
    final prefs = await SharedPreferences.getInstance();
    final key = _buildKey(scope, userId);
    final ids = (prefs.getStringList(key) ?? <String>[]).toSet();
    ids.add(itemId);
    await prefs.setStringList(key, ids.toList()..sort());
  }

  Future<void> unhideItem({
    required String scope,
    required String userId,
    required String itemId,
  }) async {
    final prefs = await SharedPreferences.getInstance();
    final key = _buildKey(scope, userId);
    final ids = (prefs.getStringList(key) ?? <String>[]).toSet();
    ids.remove(itemId);
    await prefs.setStringList(key, ids.toList()..sort());
  }

  Future<void> clearHiddenItems({
    required String scope,
    required String userId,
  }) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_buildKey(scope, userId));
  }

  String _buildKey(String scope, String userId) {
    return '$_hiddenKeyPrefix:$scope:$userId';
  }
}
