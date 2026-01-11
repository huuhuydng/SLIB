import 'package:flutter/services.dart';

/// Bridge to Android HCE service to push the user ID into SharedPreferences.
class HceBridge {
  static const MethodChannel _channel = MethodChannel('slib/hce');

  static Future<void> setUserId(String userId) async {
    try {
      await _channel.invokeMethod('setUserId', {'userId': userId});
    } catch (_) {
    }
  }

  static Future<void> clearUserId() async {
    try {
      await _channel.invokeMethod('clearUserId');
    } catch (_) {
      // Ignore errors on platforms without the channel (e.g., iOS).
    }
  }
}
