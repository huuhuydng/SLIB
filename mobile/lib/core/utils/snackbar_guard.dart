import 'package:flutter/material.dart';

class SnackbarGuard {
  static final Map<String, DateTime> _lastShownAt = <String, DateTime>{};

  static bool shouldShow(
    String key, {
    Duration cooldown = const Duration(seconds: 3),
  }) {
    final now = DateTime.now();
    final lastShownAt = _lastShownAt[key];

    if (lastShownAt != null && now.difference(lastShownAt) < cooldown) {
      return false;
    }

    _lastShownAt[key] = now;
    return true;
  }

  static void show(
    BuildContext context, {
    required String message,
    String? key,
    Duration cooldown = const Duration(seconds: 3),
    Duration duration = const Duration(seconds: 4),
    Color? backgroundColor,
    SnackBarBehavior? behavior,
  }) {
    final dedupeKey = key ?? message;
    if (!shouldShow(dedupeKey, cooldown: cooldown)) {
      return;
    }

    final messenger = ScaffoldMessenger.maybeOf(context);
    if (messenger == null) {
      return;
    }

    messenger.hideCurrentSnackBar();
    messenger.showSnackBar(
      SnackBar(
        content: Text(message),
        duration: duration,
        backgroundColor: backgroundColor,
        behavior: behavior,
      ),
    );
  }
}
