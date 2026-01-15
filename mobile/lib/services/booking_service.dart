import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:intl/intl.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/seat.dart';
import 'package:slib/models/zones.dart';

class BookingService {
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

  Future<int> getAvailableSeat(int zoneId) async {
    final url = Uri.parse("${ApiConstants.seatUrl}/getAvailableSeat/$zoneId");
    final response = await http.get(url);

    if (response.statusCode == 200) {
      final int count = int.parse(response.body);
      return count;
    } else {
      print("Status: ${response.statusCode}, Body: ${response.body}");
      throw Exception("Lỗi: ${response.body}");
    }
  }

  Future<List<Seat>> getSeats(int zoneId) async {
    final url = Uri.parse("${ApiConstants.seatUrl}/getAllSeat/$zoneId");
    final response = await http.get(url);

    // In ra status code và body để debug
    debugPrint("Status: ${response.statusCode}");
    debugPrint("Body: ${response.body}");

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
    debugPrint("Status: ${response.statusCode}");
    debugPrint("Body: ${response.body}");
    if (response.statusCode == 200) {
      final List<dynamic> jsonData = jsonDecode(response.body);
      return jsonData.map((e) => Seat.fromJson(e)).toList();
    } else {
      throw Exception("Failed to load seats by time: ${response.body}");
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
      return jsonDecode(response.body); // JSON có reservationId
    } else {
      throw Exception("Failed to create booking: ${response.body}");
    }
  }

  Future<void> cancelReservation(String reservationId) async {
    final url = Uri.parse("${ApiConstants.bookingUrl}/cancel/$reservationId");
    final response = await http.put(url);

    if (response.statusCode != 200) {
      throw Exception("Failed to cancel reservation: ${response.body}");
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
}
