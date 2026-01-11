import 'dart:async';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/seat.dart';
import 'package:slib/services/booking_service.dart';

class BookingConfirmScreen extends StatefulWidget {
  final Seat seat;
  final DateTime date;
  final String timeSlot;
  final String zoneName;
  final String reservationId;
  final String userId;

  const BookingConfirmScreen({
    super.key,
    required this.seat,
    required this.date,
    required this.timeSlot,
    required this.zoneName,
    required this.reservationId,
    required this.userId,
  });

  @override
  State<BookingConfirmScreen> createState() => _BookingConfirmScreenState();
}

class _BookingConfirmScreenState extends State<BookingConfirmScreen> {
  Timer? _timer;
  int _secondsLeft = 60; // 1 phút

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      setState(() => _secondsLeft--);
      if (_secondsLeft <= 0) {
        _cancelReservation();
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _cancelReservation() async {
    final bookingService = Provider.of<BookingService>(context, listen: false);
    await bookingService.cancelReservation(widget.reservationId);

    if (mounted) {
      Navigator.popUntil(context, (route) => route.isFirst);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Đặt chỗ đã bị hủy do quá thời gian")),
      );
    }
  }

  Future<void> _confirmReservation() async {
    _timer?.cancel();
    final bookingService = Provider.of<BookingService>(context, listen: false);
    await bookingService.updateStatus(widget.reservationId, "BOOKED");

    if (mounted) {
      _showSuccessBottomSheet(context);
    }
  }

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
      onWillPop: () async {
        // Khi user nhấn back → hủy reservation giống như hết giờ
        await _cancelReservation();
        return false; // chặn pop mặc định, vì mình đã điều hướng thủ công
      },
      child: Scaffold(
        backgroundColor: const Color(0xFFF5F7FA),
        appBar: AppBar(
          title: const Text(
            "Xác nhận thông tin",
            style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black),
          ),
          backgroundColor: Colors.white,
          elevation: 0,
          centerTitle: true,
          iconTheme: const IconThemeData(color: Colors.black),
        ),
        body: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            children: [
              const Icon(Icons.chair, size: 80, color: AppColors.brandColor),
              Text(
                "⏳ Thời gian còn lại: $_secondsLeft giây",
                style: const TextStyle(color: Colors.red),
              ),
              const SizedBox(height: 16),
              const Text(
                "Kiểm tra lại chỗ ngồi",
                style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              const Text(
                "Vui lòng kiểm tra kỹ thời gian và vị trí\ntrước khi xác nhận giữ chỗ.",
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey),
              ),
              const SizedBox(height: 32),

              // Card thông tin chi tiết
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(20),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.05),
                      blurRadius: 20,
                      offset: const Offset(0, 10),
                    ),
                  ],
                ),
                child: Column(
                  children: [
                    _buildInfoRow(
                      Icons.location_on_rounded,
                      "Khu vực",
                      widget.zoneName,
                    ),
                    const Divider(height: 30),
                    _buildInfoRow(
                      Icons.chair_alt_rounded,
                      "Mã số ghế",
                      widget.seat.seatCode,
                    ),
                    const Divider(height: 30),
                    _buildInfoRow(
                      Icons.calendar_month_rounded,
                      "Ngày đặt",
                      DateFormat('dd MMMM, yyyy').format(widget.date),
                    ),
                    const Divider(height: 30),
                    _buildInfoRow(
                      Icons.access_time_filled_rounded,
                      "Khung giờ",
                      widget.timeSlot,
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 40),

              Row(
                children: const [
                  Icon(Icons.info_outline, size: 16, color: Colors.orange),
                  SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      "Lưu ý: Bạn cần check-in trong vòng 15 phút sau khi khung giờ bắt đầu.",
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.orange,
                        fontStyle: FontStyle.italic,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),

              SizedBox(
                width: double.infinity,
                height: 55,
                child: ElevatedButton(
                  onPressed: _confirmReservation,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.brandColor,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                    elevation: 0,
                  ),
                  child: const Text(
                    "XÁC NHẬN ĐẶT CHỖ",
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfoRow(IconData icon, String title, String value) {
    return Row(
      children: [
        Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            color: AppColors.brandColor.withOpacity(0.1),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(icon, color: AppColors.brandColor, size: 24),
        ),
        const SizedBox(width: 16),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(color: Colors.grey, fontSize: 13),
            ),
            Text(
              value,
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
          ],
        ),
      ],
    );
  }

  void _showSuccessBottomSheet(BuildContext context) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(30)),
      ),
      builder: (context) => Container(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.check_circle_rounded,
              color: Colors.green,
              size: 80,
            ),
            const SizedBox(height: 16),
            const Text(
              "Đặt chỗ thành công!",
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Text(
              "Hệ thống đã ghi nhận yêu cầu của bạn.",
              style: TextStyle(color: Colors.grey),
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: () =>
                  Navigator.popUntil(context, (route) => route.isFirst),
              child: const Text("VỀ TRANG CHỦ"),
            ),
          ],
        ),
      ),
    );
  }
}
