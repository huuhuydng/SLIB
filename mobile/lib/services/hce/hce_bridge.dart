import 'package:flutter/services.dart';

/// Bridge to Android HCE service to push the user ID into SharedPreferences.
class HceBridge {
  static const MethodChannel _channel = MethodChannel('slib/hce');

  static Future<void> setUserId(String userId) async {
    try {
      await _channel.invokeMethod('setUserId', {'userId': userId});
    } catch (_) {}
  }

  static Future<void> clearUserId() async {
    try {
      await _channel.invokeMethod('clearUserId');
    } catch (_) {
      // Ignore errors on platforms without the channel (e.g., iOS).
    }
  }

  static Future<bool> isNfcEnabled() async {
    try {
      return await _channel.invokeMethod<bool>('isNfcEnabled') ?? false;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> isDefaultPaymentService() async {
    try {
      return await _channel.invokeMethod<bool>('isDefaultPaymentService') ??
          false;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> requestDefaultPaymentService() async {
    try {
      return await _channel.invokeMethod<bool>(
            'requestDefaultPaymentService',
          ) ??
          false;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> openNfcPaymentSettings() async {
    try {
      return await _channel.invokeMethod<bool>('openNfcPaymentSettings') ??
          false;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> openNfcSettings() async {
    try {
      return await _channel.invokeMethod<bool>('openNfcSettings') ?? false;
    } catch (_) {
      return false;
    }
  }
}
