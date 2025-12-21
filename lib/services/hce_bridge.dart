import 'package:flutter/services.dart';

/// Bridge to Android HCE service to push the student code into SharedPreferences.
class HceBridge {
  static const MethodChannel _channel = MethodChannel('slib/hce');

  static Future<void> setStudentCode(String code) async {
    try {
      await _channel.invokeMethod('setStudentCode', {'code': code});
    } catch (_) {
      // Silently ignore to avoid breaking login flow; optionally log with debugPrint.
    }
  }

  static Future<void> clearStudentCode() async {
    try {
      await _channel.invokeMethod('clearStudentCode');
    } catch (_) {
      // Ignore errors on platforms without the channel (e.g., iOS).
    }
  }
}
