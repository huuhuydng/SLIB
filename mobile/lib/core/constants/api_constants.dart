// File: lib/core/constants/api_constants.dart
class ApiConstants {
  // Chỉ sửa 1 chỗ này duy nhất cho cả app
  // REAL DEVICE: Dung Cloudflare Tunnel URL
  static const String domain = "https://between-smith-added-granny.trycloudflare.com";
  // static const String domain = "http://10.0.2.2:8080";
  
  // Note: aiDomain không còn cần vì AI Chat đi qua backend proxy


  
  static const String authUrl = "$domain/slib/users";
  static const String authBaseUrl = "$domain/slib/auth";

  static const String newsUrl = "$domain/slib/news";

  static const String bookingUrl = "$domain/slib/bookings";

  static const String areaUrl = "$domain/slib/areas";

  static const String zoneUrl = "$domain/slib/zones";

  static const String seatUrl = "$domain/slib/seats";

  static const String factoriesUrl = "$domain/slib/area_factories";

  static const String amenitiesUrl = "$domain/slib/zone_amenities";

  static const String settingUrl = "$domain/slib/settings";

  static const String studentProfileUrl = "$domain/slib/student-profile";

  static const String activityUrl = "$domain/slib/activities";

  // Chat URLs
  static const String chatUrl = "$domain/slib/chat";
  // AI Chat đi qua backend proxy (vì ngrok chỉ expose backend)
  static const String aiChatUrl = "$domain/slib/ai/proxy-chat";

  // Violation Reports
  static const String violationReportUrl = "$domain/slib/violation-reports";
}