import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:intl/intl.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/amenity.dart';
import 'package:slib/models/area.dart';
import 'package:slib/models/area_factory.dart';
import 'package:slib/models/library_setting.dart';
import 'package:slib/models/seat.dart';
import 'package:slib/models/zone_occupancy.dart';
import 'package:slib/models/zones.dart';
import 'package:slib/utils/nfc_uid_hasher.dart';

class BookingService {
  // ================= AREAS =================
  Future<List<Area>> getAllAreas() async {
    final url = Uri.parse(ApiConstants.areaUrl);
    final response = await http.get(url);

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Area.fromJson(json)).toList();
    } else {
      throw Exception("Failed to load areas: ${response.body}");
    }
  }

  // ================= FACTORIES (Obstacles) =================
  Future<List<AreaFactory>> getFactoriesByArea(int areaId) async {
    final url = Uri.parse("${ApiConstants.factoriesUrl}?areaId=$areaId");
    final response = await http.get(url);

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => AreaFactory.fromJson(json)).toList();
    } else {
      throw Exception("Failed to load factories: ${response.body}");
    }
  }

  // ================= ZONES =================
  Future<List<Zone>> getZonesByArea(int areaId) async {
    final url = Uri.parse("${ApiConstants.zoneUrl}?areaId=$areaId");
    final response = await http.get(url);

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Zone.fromJson(json)).toList();
    } else {
      throw Exception("Failed to load zones: ${response.body}");
    }
  }

  Future<List<Zones>> getAllZones() async {
    final url = Uri.parse("${ApiConstants.zoneUrl}/getAllZones");
    final response = await http.get(url);

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Zones.fromJson(json)).toList();
    } else {
      throw Exception("Failed to load zones: ${response.body}");
    }
  }

  /// Get zone occupancy (mật độ sử dụng) for all zones in an area
  Future<List<ZoneOccupancy>> getZoneOccupancy(int areaId) async {
    final url = Uri.parse("${ApiConstants.zoneUrl}/occupancy/$areaId");
    final response = await http.get(url);

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => ZoneOccupancy.fromJson(json)).toList();
    } else {
      throw Exception("Failed to load zone occupancy: ${response.body}");
    }
  }

  // ================= AMENITIES =================
  /// Get amenities (tiện ích) for a specific zone
  Future<List<Amenity>> getAmenities(int zoneId) async {
    final url = Uri.parse("${ApiConstants.amenitiesUrl}?zoneId=$zoneId");
    final response = await http.get(url);

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Amenity.fromJson(json)).toList();
    } else {
      throw Exception("Failed to load amenities: ${response.body}");
    }
  }

  // ================= LIBRARY SETTINGS =================
  /// Get library settings (cấu hình thư viện)
  Future<LibrarySetting> getLibrarySettings() async {
    final url = Uri.parse("${ApiConstants.settingUrl}/library");
    final response = await http.get(url);

    if (response.statusCode == 200) {
      final Map<String, dynamic> data = jsonDecode(response.body);
      return LibrarySetting.fromJson(data);
    } else {
      throw Exception("Failed to load library settings: ${response.body}");
    }
  }

  /// Get time slots (khung giờ đã được generate từ cấu hình)
  Future<List<TimeSlot>> getTimeSlots() async {
    final url = Uri.parse("${ApiConstants.settingUrl}/time-slots");
    final response = await http.get(url);

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => TimeSlot.fromJson(json)).toList();
    } else {
      throw Exception("Failed to load time slots: ${response.body}");
    }
  }

  // ================= SEATS =================
  Future<int> getAvailableSeat(int zoneId) async {
    final url = Uri.parse("${ApiConstants.seatUrl}/getAvailableSeat/$zoneId");
    final response = await http.get(url);

    if (response.statusCode == 200) {
      final int count = int.parse(response.body);
      return count;
    } else {
      debugPrint("Status: ${response.statusCode}, Body: ${response.body}");
      throw Exception("Lỗi: ${response.body}");
    }
  }

  Future<List<Seat>> getSeats(int zoneId) async {
    final url = Uri.parse("${ApiConstants.seatUrl}/getAllSeat/$zoneId");
    final response = await http.get(url);

    // debugPrint("Status: ${response.statusCode}");
    // debugPrint("Body: ${response.body}");

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Seat.fromJson(json)).toList();
    } else {
      throw Exception(
        "Lỗi khi lấy danh sách ghế: ${response.statusCode} ${response.body}",
      );
    }
  }

  Future<List<Seat>> getSeatsByTime(
    int zoneId,
    DateTime date,
    String start,
    String end,
  ) async {
    final dateStr = DateFormat('yyyy-MM-dd').format(date);
    final url = Uri.parse(
      "${ApiConstants.seatUrl}/getSeatsByTime/$zoneId?date=$dateStr&start=$start&end=$end",
    );
    final response = await http.get(url);
    // debugPrint("Status: ${response.statusCode}");
    // debugPrint("Body: ${response.body}");
    if (response.statusCode == 200) {
      final List<dynamic> jsonData = jsonDecode(response.body);
      return jsonData.map((e) => Seat.fromJson(e)).toList();
    } else {
      throw Exception("Failed to load seats by time: ${response.body}");
    }
  }

  /// Lấy tất cả seats của 1 area trong 1 API call - tối ưu performance
  /// Trả về Map<zoneId, List<Seat>>
  Future<Map<int, List<Seat>>> getAllSeatsByArea(
    int areaId,
    DateTime date,
    String start,
    String end,
  ) async {
    final dateStr = DateFormat('yyyy-MM-dd').format(date);
    final url = Uri.parse(
      "${ApiConstants.seatUrl}/area/$areaId/all-seats?date=$dateStr&start=$start&end=$end",
    );
    final response = await http.get(url);
    
    if (response.statusCode == 200) {
      final Map<String, dynamic> data = jsonDecode(response.body);
      Map<int, List<Seat>> result = {};
      data.forEach((key, value) {
        final zoneId = int.parse(key);
        final seats = (value as List).map((e) => Seat.fromJson(e)).toList();
        result[zoneId] = seats;
      });
      return result;
    } else {
      throw Exception("Failed to load all seats by area: ${response.body}");
    }
  }

  Future<List<Seat>> getSeatsByDate(int zoneId, DateTime date) async {
    final dateStr = DateFormat('yyyy-MM-dd').format(date);
    final url = Uri.parse(
      "${ApiConstants.seatUrl}/getSeatsByDate/$zoneId?date=$dateStr",
    );
    final response = await http.get(url);

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Seat.fromJson(json)).toList();
    } else {
      throw Exception("Failed to load seats by date: ${response.body}");
    }
  }

  // ================= BOOKING =================
  Future<Map<String, dynamic>> createBooking({
    required String userId,
    required int seatId,
    required DateTime date,
    required String start,
    required String end,
  }) async {
    final url = Uri.parse("${ApiConstants.bookingUrl}/create");

    final body = jsonEncode({
      "user_id": userId,
      "seat_id": seatId.toString(),
      "start_time": "${DateFormat('yyyy-MM-dd').format(date)}T$start:00",
      "end_time": "${DateFormat('yyyy-MM-dd').format(date)}T$end:00",
    });

    final response = await http.post(
      url,
      headers: {"Content-Type": "application/json"},
      body: body,
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      // Parse error message từ backend
      String errorMsg = response.body;
      try {
        final errorJson = jsonDecode(response.body);
        // Backend trả về error: "Error: Bạn đã đặt ghế..."
        if (errorJson['error'] != null) {
          errorMsg = errorJson['error'].toString().replaceFirst('Error: ', '');
        } else if (errorJson['message'] != null) {
          errorMsg = errorJson['message'];
        }
      } catch (_) {
        // Nếu không parse được, dùng raw message
        if (errorMsg.contains('Error: ')) {
          errorMsg = errorMsg.split('Error: ').last;
        }
      }
      throw Exception(errorMsg);
    }
  }

  Future<void> cancelReservation(String reservationId) async {
    final url = Uri.parse("${ApiConstants.bookingUrl}/cancel/$reservationId");
    final response = await http.put(url);

    if (response.statusCode != 200) {
      throw Exception("Failed to cancel reservation: ${response.body}");
    }
  }

  /// Get upcoming booking for a user
  /// Returns null if no upcoming booking
  Future<dynamic> getUpcomingBooking(String userId) async {
    final url = Uri.parse("${ApiConstants.bookingUrl}/upcoming/$userId");
    final response = await http.get(url);

    if (response.statusCode == 200) {
      final decodedBody = utf8.decode(response.bodyBytes);
      return jsonDecode(decodedBody);
    } else if (response.statusCode == 204) {
      // No content - no upcoming booking
      return null;
    } else {
      throw Exception("Failed to load upcoming booking: ${response.body}");
    }
  }

  Future<void> updateStatus(String reservationId, String status) async {
    final url = Uri.parse(
      "${ApiConstants.bookingUrl}/updateStatusReserv/$reservationId?status=$status",
    );
    final response = await http.put(url);

    if (response.statusCode != 200) {
      throw Exception("Failed to update status: ${response.body}");
    }
  }

  /// Confirm seat with NFC tag data
  Future<Map<String, dynamic>> confirmSeatWithNfc(String reservationId, String nfcData) async {
    final url = Uri.parse("${ApiConstants.bookingUrl}/confirm-nfc/$reservationId");
    
    final response = await http.post(
      url,
      headers: {"Content-Type": "application/json"},
      body: jsonEncode({"nfc_data": nfcData}),
    );

    if (response.statusCode == 200) {
      return jsonDecode(utf8.decode(response.bodyBytes));
    } else {
      final errorMsg = utf8.decode(response.bodyBytes);
      throw Exception(errorMsg);
    }
  }

  // ================= NFC UID MAPPING =================

  /// Get seat by NFC tag UID (UID Mapping Strategy)
  /// 
  /// This is used for check-in flow where the student scans an NFC tag
  /// and the app looks up which seat it corresponds to.
  /// 
  /// [nfcUid] - NFC tag UID in uppercase HEX format (e.g., "04A23C91")
  /// Returns the seat information if found, throws exception otherwise.
  Future<Seat> getSeatByNfcUid(String nfcUid) async {
    // Hash the UID before sending to backend (must match backend's hash)
    final hashedUid = NfcUidHasher.hashUid(nfcUid);
    debugPrint('BookingService: Raw UID: $nfcUid');
    debugPrint('BookingService: Hashed UID: $hashedUid');
    final url = Uri.parse("${ApiConstants.seatUrl}/by-nfc-uid/$hashedUid");
    final response = await http.get(url);

    if (response.statusCode == 200) {
      final Map<String, dynamic> data = jsonDecode(utf8.decode(response.bodyBytes));
      return Seat.fromJson(data);
    } else if (response.statusCode == 404) {
      // Seat not found for this NFC UID
      throw Exception("Không tìm thấy ghế với NFC UID này");
    } else {
      final errorBody = utf8.decode(response.bodyBytes);
      throw Exception("Lỗi khi tìm ghế: $errorBody");
    }
  }

  /// Confirm seat check-in using NFC UID
  /// 
  /// This method:
  /// 1. Looks up the seat by NFC UID
  /// 2. Validates that it matches the reservation's seat
  /// 3. Confirms the booking
  /// 
  /// [reservationId] - The booking reservation ID
  /// [nfcUid] - NFC tag UID in uppercase HEX format
  /// [expectedSeatId] - The expected seat ID from the reservation
  Future<Map<String, dynamic>> confirmSeatWithNfcUid(
    String reservationId, 
    String nfcUid,
    int expectedSeatId,
  ) async {
    // Step 1: Look up seat by NFC UID
    final seat = await getSeatByNfcUid(nfcUid);
    
    // Step 2: Validate seat matches reservation
    if (seat.seatId != expectedSeatId) {
      throw Exception(
        "Ghế không khớp! Bạn đang đặt ghế ID: $expectedSeatId nhưng thẻ NFC là ghế ID: ${seat.seatId} (${seat.seatCode})"
      );
    }
    
    // Step 3: Confirm the booking (use existing confirm endpoint)
    // Create NFC data in legacy format for backward compatibility
    final nfcData = "SEAT_ID:${seat.seatCode}_ZONE:${seat.zoneId}";
    return confirmSeatWithNfc(reservationId, nfcData);
  }
}
