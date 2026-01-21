// File: lib/core/constants/api_constants.dart
class ApiConstants {
  // Chỉ sửa 1 chỗ này duy nhất cho cả app
  static const String domain = "https://hyperscrupulous-ropeable-alverta.ngrok-free.dev";
  // static const String domain = "https://slib-backend-oftc.onrender.com";
  
  static const String authUrl = "$domain/slib/users";
  static const String authBaseUrl = "$domain/slib/auth";  // New auth endpoints

  static const String newsUrl = "$domain/slib/news";

  static const String bookingUrl = "$domain/slib/bookings";

  static const String areaUrl = "$domain/slib/areas";

  static const String zoneUrl = "$domain/slib/zones";

  static const String seatUrl = "$domain/slib/seats";

  static const String factoriesUrl = "$domain/slib/area_factories";

  static const String amenitiesUrl = "$domain/slib/zone_amenities";

  static const String settingUrl = "$domain/slib/settings";

}