import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/upcoming_booking.dart';
import 'package:slib/services/auth_service.dart';
import 'package:slib/views/home/widgets/booking_action_dialog.dart';

class BookingHistoryScreen extends StatefulWidget {
  const BookingHistoryScreen({super.key});

  @override
  State<BookingHistoryScreen> createState() => _BookingHistoryScreenState();
}

class _BookingHistoryScreenState extends State<BookingHistoryScreen> {
  List<Map<String, dynamic>> _activeBookings = [];
  List<Map<String, dynamic>> _completedBookings = [];
  List<Map<String, dynamic>> _cancelledBookings = [];
  
  bool _isLoading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadBookings();
  }

  Future<void> _loadBookings() async {
    final authService = Provider.of<AuthService>(context, listen: false);
    final user = authService.currentUser;

    if (user == null) {
      setState(() {
        _errorMessage = "Vui lòng đăng nhập";
        _isLoading = false;
      });
      return;
    }

    try {
      final url = Uri.parse("${ApiConstants.bookingUrl}/user/${user.id}");
      final response = await http.get(url);

      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(utf8.decode(response.bodyBytes));
        final now = DateTime.now();

        _activeBookings = [];
        _completedBookings = [];
        _cancelledBookings = [];

        for (var booking in data) {
          final status = (booking['status'] ?? '').toString().toUpperCase();
          final endTime = DateTime.parse(booking['endTime']);
          final startTime = DateTime.parse(booking['startTime']);
          
          // New flat DTO structure - zoneName and areaName are directly in response
          final parsedBooking = {
            'reservationId': booking['reservationId'],
            'seatCode': booking['seatCode'] ?? 'N/A',
            'zoneName': booking['zoneName'] ?? 'N/A',
            'areaName': booking['areaName'] ?? 'N/A',
            'startTime': startTime,
            'endTime': endTime,
            'status': status,
            'date': DateFormat('dd/MM/yyyy').format(startTime),
            'time': '${DateFormat('HH:mm').format(startTime)} - ${DateFormat('HH:mm').format(endTime)}',
          };

          if (status == 'CANCEL' || status == 'CANCELLED') {
            _cancelledBookings.add(parsedBooking);
          } else if (status == 'EXPIRED') {
            // EXPIRED = no-show (đặt nhưng không đến check-in)
            _cancelledBookings.add(parsedBooking);
          } else if (status == 'COMPLETED' || endTime.isBefore(now)) {
            _completedBookings.add(parsedBooking);
          } else {
            _activeBookings.add(parsedBooking);
          }
        }

        _activeBookings.sort((a, b) => (a['startTime'] as DateTime).compareTo(b['startTime']));
        _completedBookings.sort((a, b) => (b['startTime'] as DateTime).compareTo(a['startTime']));
        _cancelledBookings.sort((a, b) => (b['startTime'] as DateTime).compareTo(a['startTime']));

        setState(() => _isLoading = false);
      } else {
        throw Exception("Failed to load bookings");
      }
    } catch (e) {
      setState(() {
        _errorMessage = e.toString();
        _isLoading = false;
      });
    }
  }

  void _showActionDialog(Map<String, dynamic> booking) {
    final upcomingBooking = UpcomingBooking(
      reservationId: booking['reservationId'],
      status: booking['status'],
      seatId: 0,
      seatCode: booking['seatCode'],
      zoneId: 0,
      zoneName: booking['zoneName'],
      areaId: 0,
      areaName: booking['areaName'],
      startTime: booking['startTime'],
      endTime: booking['endTime'],
      dayOfWeek: '',
      dayOfMonth: (booking['startTime'] as DateTime).day,
      timeRange: booking['time'],
    );

    BookingActionDialog.show(context, upcomingBooking, () {
      _loadBookings();
    });
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 3,
      child: Scaffold(
        backgroundColor: const Color(0xFFF5F7FA),
        appBar: AppBar(
          title: const Text(
            "Lịch sử đặt chỗ",
            style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black87),
          ),
          backgroundColor: Colors.white,
          centerTitle: true,
          elevation: 0,
          iconTheme: const IconThemeData(color: Colors.black87),
          bottom: TabBar(
            labelColor: AppColors.brandColor,
            unselectedLabelColor: Colors.grey,
            indicatorColor: AppColors.brandColor,
            indicatorWeight: 3,
            labelStyle: const TextStyle(fontWeight: FontWeight.bold),
            tabs: const [
              Tab(text: "Sắp tới"),
              Tab(text: "Hoàn thành"),
              Tab(text: "Đã hủy"),
            ],
          ),
        ),
        body: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : _errorMessage != null
                ? Center(child: Text(_errorMessage!, style: const TextStyle(color: Colors.red)))
                : TabBarView(
                    children: [
                      _buildBookingList(_activeBookings, isActive: true),
                      _buildBookingList(_completedBookings),
                      _buildBookingList(_cancelledBookings),
                    ],
                  ),
      ),
    );
  }

  Widget _buildBookingList(List<Map<String, dynamic>> bookings, {bool isActive = false}) {
    if (bookings.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.history_toggle_off, size: 60, color: Colors.grey[300]),
            const SizedBox(height: 10),
            Text("Không có dữ liệu", style: TextStyle(color: Colors.grey[500])),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadBookings,
      child: ListView.separated(
        padding: const EdgeInsets.all(20),
        itemCount: bookings.length,
        separatorBuilder: (context, index) => const SizedBox(height: 15),
        itemBuilder: (context, index) {
          return _buildBookingCard(bookings[index], isActive: isActive);
        },
      ),
    );
  }

  Widget _buildBookingCard(Map<String, dynamic> booking, {bool isActive = false}) {
    final status = booking['status'] as String;
    final startTime = booking['startTime'] as DateTime;
    final endTime = booking['endTime'] as DateTime;
    final now = DateTime.now();
    
    Color statusColor;
    String statusText;
    IconData statusIcon;
    bool isOngoing = now.isAfter(startTime) && now.isBefore(endTime);

    if (status == 'CANCEL' || status == 'CANCELLED') {
      statusColor = Colors.red;
      statusText = "Đã hủy";
      statusIcon = Icons.cancel_outlined;
    } else if (status == 'EXPIRED') {
      statusColor = Colors.orange;
      statusText = "Không đến";
      statusIcon = Icons.warning_amber_rounded;
    } else if (status == 'COMPLETED' || endTime.isBefore(now)) {
      statusColor = Colors.green;
      statusText = "Hoàn thành";
      statusIcon = Icons.check_circle;
    } else if (isOngoing) {
      statusColor = Colors.teal;
      statusText = "Đang sử dụng";
      statusIcon = Icons.timelapse;
    } else {
      statusColor = AppColors.brandColor;
      statusText = status == 'PROCESSING' ? "Chờ xác nhận" : "Sắp tới";
      statusIcon = Icons.event_available;
    }

    return GestureDetector(
      onTap: isActive ? () => _showActionDialog(booking) : null,
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.05),
              blurRadius: 10,
              offset: const Offset(0, 4),
            )
          ],
          border: Border(left: BorderSide(color: statusColor, width: 4)),
        ),
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        booking['zoneName'],
                        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 4),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                        decoration: BoxDecoration(
                          color: Colors.grey[100],
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          "Ghế: ${booking['seatCode']}",
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: Colors.grey[700],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: statusColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(statusIcon, size: 14, color: statusColor),
                      const SizedBox(width: 4),
                      Text(
                        statusText,
                        style: TextStyle(
                          color: statusColor,
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 15),
            const Divider(height: 1),
            const SizedBox(height: 15),
            Row(
              children: [
                Expanded(
                  child: Row(
                    children: [
                      Icon(Icons.calendar_today_outlined, size: 16, color: Colors.grey[400]),
                      const SizedBox(width: 8),
                      Text(
                        booking['date'],
                        style: TextStyle(color: Colors.grey[700], fontSize: 13),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: Row(
                    children: [
                      Icon(Icons.access_time, size: 16, color: Colors.grey[400]),
                      const SizedBox(width: 8),
                      Text(
                        booking['time'],
                        style: TextStyle(
                          color: Colors.grey[700],
                          fontSize: 13,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
                if (isActive) 
                  Icon(Icons.chevron_right, color: Colors.grey[400]),
              ],
            ),
          ],
        ),
      ),
    );
  }
}