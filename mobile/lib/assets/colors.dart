import 'package:flutter/material.dart';

class AppColors {
  // Text Colors
  static const Color textPrimary = Color(0xFF1A1A1A);
  static const Color textSecondary = Color(0xFF4A5568);
  static const Color textThird = Color(0xFFA0AEC0);
  static const Color textOnColor = Color(0xFFFFFFFF);

  // Brand Color
  static const Color brandColor = Color(0xFFFF751F);

  // Background Colors
  static const Color backgroundPrimary = Color(0xFFFFFFFF);
  static const Color backgroundSecondary = Color(0xFFFFF7F2);

  // Border Color
  static const Color borderPrimary = Color(0xFFE2E8F0);

  // Accent Colors
  static const Color accent1 = Color(0xFF0054A6);
  static const Color accent2 = Color(0xFF4CA75B);
  static const Color accent3 = Color(0xFFD32F2F);
  static const Color accent4 = Color(0xFFFDB913);

  // Legacy colors (deprecated - kept for backwards compatibility)
  @Deprecated('Use brandColor instead')
  static const Color mainColor = Color(0xFF6C63FF);

  @Deprecated('Use accent3 instead')
  static const Color secondaryColor = Color(0xFFFF6584);

  @Deprecated('Use backgroundPrimary instead')
  static const Color backgroundColor = Color(0xFFF5F5F5);

  @Deprecated('Use textPrimary instead')
  static const Color textColor = Color(0xFF333333);

  static const Color success = Color(0xFF388E3C);
  static const Color error = Color(0xFFD32F2F);

  static const Color textGrey = Color(0xFF4A5568);

  static const Color available = Color(0xFF4CAF50); // Xanh lá - Vắng
  static const Color busy = Color(0xFFFF9800); // Cam - Khá đông
  static const Color full = Color(0xFFF44336);

  static const Color seatAvailable = Color(0xFFE2E8F0);
  static const Color seatOccupied = Color(0xFFCBD5E0);

  static const Color greyLight = Color(0xFFF7FAFC);
}
