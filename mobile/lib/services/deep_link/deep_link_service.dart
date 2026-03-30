import 'dart:async';
import 'package:app_links/app_links.dart';
import 'package:flutter/material.dart';

/// Handles deep links from NFC background tag reading on iOS.
///
/// NFC tags on seats contain static NDEF URL records:
///   slib://seat/{seatId}
///
/// Flow:
/// 1. iPhone (screen on, unlocked) near NFC tag on seat
/// 2. iOS reads NDEF in background → shows notification
/// 3. User taps → app opens with deep link
/// 4. App sends seatId to backend
/// 5. Backend finds user's active BOOKED reservation for that seat → confirms
class DeepLinkService {
  static final DeepLinkService _instance = DeepLinkService._internal();
  factory DeepLinkService() => _instance;
  DeepLinkService._internal();

  final AppLinks _appLinks = AppLinks();
  StreamSubscription<Uri>? _subscription;

  /// Global navigator key for showing dialogs from deep links
  static final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

  /// Initialize deep link listening
  void initialize() {
    // Handle link when app is already running
    _subscription = _appLinks.uriLinkStream.listen((Uri uri) {
      debugPrint('[DeepLink] Received: $uri');
      _handleDeepLink(uri);
    });

    // Handle link that opened the app (cold start)
    _appLinks.getInitialLink().then((Uri? uri) {
      if (uri != null) {
        debugPrint('[DeepLink] Initial link: $uri');
        // Delay to let app initialize first
        Future.delayed(const Duration(seconds: 3), () {
          _handleDeepLink(uri);
        });
      }
    });
  }

  void dispose() {
    _subscription?.cancel();
  }

  /// Process deep link URI
  void _handleDeepLink(Uri uri) {
    if (uri.scheme != 'slib') return;

    debugPrint('[DeepLink] Host: ${uri.host}, Path: ${uri.path}');

    // slib://seat/{seatId}/{signature}
    // NFC tag on seat contains this static URL with HMAC signature
    if (uri.host == 'seat') {
      final segments = uri.pathSegments;
      if (segments.length >= 2) {
        final seatId = segments[0];
        final signature = segments[1];
        _confirmSeatBySeatId(seatId, signature);
      } else {
        _showResultDialog(false, 'Link NFC không hợp lệ');
      }
    }
  }

  /// Call backend to confirm booking by seat ID with HMAC signature.
  /// Backend verifies signature and finds user's active BOOKED reservation.
  Future<void> _confirmSeatBySeatId(String seatId, String signature) async {
    _showResultDialog(
      false,
      'Xác nhận qua deep link hiện chưa được bật trên máy chủ. Vui lòng mở ứng dụng và quét NFC trực tiếp để check-in ghế.',
    );
  }

  void _showResultDialog(bool success, String message) {
    final ctx = navigatorKey.currentContext;
    if (ctx == null) return;

    showDialog(
      context: ctx,
      builder: (_) => Dialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                success ? Icons.check_circle : Icons.error,
                size: 64,
                color: success ? Colors.green : Colors.red,
              ),
              const SizedBox(height: 16),
              Text(
                success ? 'Thành công!' : 'Không thể xác nhận',
                style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text(
                message,
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 14, color: Colors.grey[600]),
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () {
                    final c = navigatorKey.currentContext;
                    if (c != null) Navigator.pop(c);
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: success ? Colors.green : const Color(0xFFFF751F),
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  child: const Text(
                    'Đóng',
                    style: TextStyle(color: Colors.white, fontSize: 16),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
