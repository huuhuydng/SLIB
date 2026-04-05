// File: lib/core/constants/api_constants.dart
class ApiConstants {
  // CHÚ Ý QUAN TRỌNG CHO TEAM MEMBER KHI PULL CODE VỀ:
  // 1. NẾU CẠY HÀNG NGÀY TRÊN MÁY ẢO (Emulator): Đóng dòng domain có slibsystem.site, mẻ̛ dòng localhost ra.
  // 2. NẾU BUILDP APK HOẶC CHẠY MÁY THẬT: Đóng localhost, mẻ̛ dòng slibsystem.site.

  // --- CHỌN 1 TRONG 2 CẤU HÌNH BÊN DƯỚI ---

  // [CẤU HÌNH 1 - PRODUCTION]: Dành cho máy thật, build APK, demo.
  static const String domain = "https://api.slibsystem.site";

  // [CẤU HÌNH 2 - DEVELOPMENT]: Dành cho máy ảo (Emulator Android) chạy chung với Backend trên cùng máy tính.
  // static const String domain = "http://10.0.2.2:8080";

  // [CẤU HÌNH 3 - LAN TESTING]: Dành cho máy thật test qua mạng LAN (cùng WiFi với máy tính).
  // static const String domain = "http://192.168.17.162:8080";

  // [CẤU HÌNH 4 - ADB REVERSE]: Dành cho máy thật qua USB (adb reverse tcp:8080 tcp:8080)
  // static const String domain = "http://localhost:8080";

  // Ghi chú: aiDomain không còn cần vì AI Chat đi qua backend proxy

  static const String authUrl = "$domain/slib/users";
  static const String authBaseUrl = "$domain/slib/auth";

  static const String newsUrl = "$domain/slib/news";
  static const String newBooksUrl = "$domain/slib/new-books";

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

  // Kiosk
  static const String kioskUrl = "$domain/slib/kiosk";

  // AI Analytics (đi qua backend proxy /slib/ai/analytics/...)
  static const String aiAnalyticsUrl = "$domain/slib/ai/analytics";
}
