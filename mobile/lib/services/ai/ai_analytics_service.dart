import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'package:slib/core/constants/api_constants.dart';

class AIAnalyticsService {
  static const _storage = FlutterSecureStorage();

  static String get _baseUrl => ApiConstants.aiAnalyticsUrl;

  static Future<Map<String, String>> _authHeaders() async {
    final token = await _storage.read(key: 'jwt_token');
    return {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  /// Lấy dự đoán mật độ sử dụng
  static Future<Map<String, dynamic>?> getDensityPrediction({String? zoneId}) async {
    try {
      final queryParams = zoneId != null ? '?zone_id=$zoneId' : '';
      final response = await http.get(
        Uri.parse('$_baseUrl/density-prediction$queryParams'),
        headers: await _authHeaders(),
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        return json.decode(response.body);
      }
      return null;
    } catch (e) {
      print('Error fetching density prediction: $e');
      return null;
    }
  }

  /// Lấy thống kê sử dụng
  static Future<Map<String, dynamic>?> getUsageStatistics({String period = 'week'}) async {
    try {
      final response = await http.get(
        Uri.parse('$_baseUrl/usage-statistics?period=$period'),
        headers: await _authHeaders(),
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        return json.decode(response.body);
      }
      return null;
    } catch (e) {
      print('Error fetching usage statistics: $e');
      return null;
    }
  }

  /// Lấy công suất thời gian thực
  static Future<Map<String, dynamic>?> getRealtimeCapacity() async {
    try {
      final response = await http.get(
        Uri.parse('$_baseUrl/realtime-capacity'),
        headers: await _authHeaders(),
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        return json.decode(response.body);
      }
      return null;
    } catch (e) {
      print('Error fetching realtime capacity: $e');
      return null;
    }
  }

  /// Lấy gợi ý chỗ ngồi
  static Future<Map<String, dynamic>?> getSeatRecommendation({
    required String userId,
    String? zonePreference,
    String? timeSlot,
  }) async {
    try {
      var queryParams = 'user_id=$userId';
      if (zonePreference != null) queryParams += '&zone_preference=$zonePreference';
      if (timeSlot != null) queryParams += '&time_slot=$timeSlot';

      print('[AI_DEBUG] getSeatRecommendation URL: $_baseUrl/seat-recommendation?$queryParams');

      final response = await http.get(
        Uri.parse('$_baseUrl/seat-recommendation?$queryParams'),
        headers: await _authHeaders(),
      ).timeout(const Duration(seconds: 10));

      print('[AI_DEBUG] getSeatRecommendation status: ${response.statusCode}');
      print('[AI_DEBUG] getSeatRecommendation body: ${response.body.substring(0, response.body.length > 500 ? 500 : response.body.length)}');
      if (response.statusCode == 200) {
        return json.decode(response.body);
      }
      return null;
    } catch (e) {
      print('Error fetching seat recommendation: $e');
      return null;
    }
  }

  /// Lấy phân tích hành vi sinh viên
  static Future<Map<String, dynamic>?> getStudentBehavior({
    required String userId,
    int days = 30,
  }) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/student-behavior'),
        headers: await _authHeaders(),
        body: json.encode({
          'user_id': userId,
          'days': days,
        }),
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        return json.decode(response.body);
      }
      return null;
    } catch (e) {
      print('Error fetching student behavior: $e');
      return null;
    }
  }

  /// Tạo insight từ dữ liệu AI cho hiển thị trên mobile
  static Future<AICardData?> generateAICardData(String userId) async {
    try {
      // Gọi song song các API
      final results = await Future.wait([
        getRealtimeCapacity(),
        getDensityPrediction(),
        getSeatRecommendation(userId: userId),
      ]);

      final capacity = results[0];
      final density = results[1];
      final seatRec = results[2];

      if (capacity == null && density == null && seatRec == null) {
        return null;
      }

      // Tạo nội dung card
      String title = '';
      String message = '';
      String? seatCode;
      int? seatId;
      int? zoneId;
      String? zoneName;
      String? recommendedTimeSlot;
      String? actionText;

      // Ưu tiên 1: Gợi ý chỗ ngồi
      if (seatRec != null && seatRec['recommendations'] != null && (seatRec['recommendations'] as List).isNotEmpty) {
        final rec = seatRec['recommendations'][0];
        print('[AI_DEBUG] Top recommendation: ${rec}');
        seatCode = rec['seat_code'];
        seatId = rec['seat_id'] is int ? rec['seat_id'] : null;
        zoneId = rec['zone_id'] is int ? rec['zone_id'] : null;
        zoneName = rec['zone'];

        title = 'Gợi ý cho bạn: $seatCode — $zoneName';

        // Tạo mô tả ngắn gọn
        final reason = rec['reason'] as String? ?? '';
        message = reason.isNotEmpty ? reason : 'Đang trống, sẵn sàng cho bạn.';

        actionText = 'Đặt chỗ $seatCode';

        // Lấy recommended time slot từ user preferences
        final prefs = seatRec['preferences'];
        if (prefs != null && prefs['user_favorite_time'] != null) {
          final favHour = prefs['user_favorite_time'] as int;
          recommendedTimeSlot = '${favHour.toString().padLeft(2, '0')}:00';
        }
      }
      // Ưu tiên 2: Cảnh báo đông đúc
      else if (capacity != null && capacity['occupancy_rate'] != null) {
        final occupancy = (capacity['occupancy_rate'] as num).toDouble();
        if (occupancy >= 90) {
          title = 'Cảnh báo: Thư viện gần kín chỗ';
          message = 'Hiện tại ${occupancy.toStringAsFixed(0)}% công suất. Nên đặt chỗ trước!';
          actionText = 'Xem chỗ trống';
        } else if (occupancy >= 70) {
          title = 'Thư viện đang đông';
          message = 'Hiện tại ${occupancy.toStringAsFixed(0)}% công suất. Nên đến sớm!';
          actionText = 'Xem chỗ trống';
        } else {
          title = 'Thư viện còn nhiều chỗ';
          message = 'Hiện tại chỉ ${occupancy.toStringAsFixed(0)}% công suất. Đây là thời điểm tốt!';
          actionText = 'Đặt chỗ ngay';
        }
      }
      // Ưu tiên 3: Dự báo giờ cao điểm
      else if (density != null && density['quiet_hours'] != null && (density['quiet_hours'] as List).isNotEmpty) {
        final quietHour = density['quiet_hours'][0];
        title = 'Dự báo: ${quietHour['label']} thư viện sẽ vắng';
        message = 'Đây là thời điểm lý tưởng để học tập. Tỉ lệ công suất dự kiến thấp.';
        actionText = 'Đặt chỗ trước';
      }
      // Fallback
      else {
        title = 'Chào mừng bạn!';
        message = 'Hãy đặt chỗ để có trải nghiệm tốt nhất tại thư viện.';
        actionText = 'Đặt chỗ ngay';
      }

      return AICardData(
        title: title,
        message: message,
        seatCode: seatCode,
        seatId: seatId,
        zoneId: zoneId,
        zoneName: zoneName,
        recommendedTimeSlot: recommendedTimeSlot,
        actionText: actionText,
        capacity: capacity,
        seatRecommendation: seatRec,
      );
    } catch (e) {
      print('Error generating AI card data: $e');
      return null;
    }
  }
}

/// Data class for AI Card
class AICardData {
  final String title;
  final String message;
  final String? seatCode;
  final int? seatId;
  final int? zoneId;
  final String? zoneName;
  final String? recommendedTimeSlot;
  final String? actionText;
  final Map<String, dynamic>? capacity;
  final Map<String, dynamic>? seatRecommendation;

  AICardData({
    required this.title,
    required this.message,
    this.seatCode,
    this.seatId,
    this.zoneId,
    this.zoneName,
    this.recommendedTimeSlot,
    this.actionText,
    this.capacity,
    this.seatRecommendation,
  });
}
