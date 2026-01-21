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
  final String userId;
  final String reservationId; // Required - đã tạo từ floor_plan_screen

  const BookingConfirmScreen({
    super.key,
    required this.seat,
    required this.date,
    required this.timeSlot,
    required this.zoneName,
    required this.userId,
    required this.reservationId,
  });

  @override
  State<BookingConfirmScreen> createState() => _BookingConfirmScreenState();
}

class _BookingConfirmScreenState extends State<BookingConfirmScreen> {
  Timer? _timer;
  int _secondsLeft = 120; // 2 phút để xác nhận
  bool _isBooking = false;

  @override
  void initState() {
    super.initState();
    _startTimer();
  }

  void _startTimer() {
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_secondsLeft > 0) {
        setState(() => _secondsLeft--);
      } else {
        _timeout();
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  /// Hết giờ - hủy reservation
  Future<void> _timeout() async {
    _timer?.cancel();
    try {
      final bookingService = Provider.of<BookingService>(context, listen: false);
      await bookingService.cancelReservation(widget.reservationId);
    } catch (e) {
      debugPrint('Error canceling on timeout: $e');
    }
    
    if (mounted) {
      Navigator.popUntil(context, (route) => route.isFirst);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Đã hết thời gian xác nhận, ghế được trả lại")),
      );
    }
  }

  /// Xác nhận đặt chỗ - chuyển status thành BOOKED
  Future<void> _confirmReservation() async {
    if (_isBooking) return;
    
    setState(() => _isBooking = true);
    _timer?.cancel();
    
    try {
      final bookingService = Provider.of<BookingService>(context, listen: false);
      
      // Cập nhật status từ PROCESSING → BOOKED
      await bookingService.updateStatus(widget.reservationId, "BOOKED");
      
      if (mounted) {
        _showSuccessBottomSheet(context);
      }
    } catch (e) {
      setState(() => _isBooking = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Lỗi xác nhận: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }

  /// Hủy đặt chỗ
  Future<void> _cancel() async {
    _timer?.cancel();
    
    try {
      final bookingService = Provider.of<BookingService>(context, listen: false);
      await bookingService.cancelReservation(widget.reservationId);
    } catch (e) {
      debugPrint('Error canceling: $e');
    }
    
    if (mounted) {
      Navigator.pop(context);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Đã hủy đặt chỗ")),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final minutes = _secondsLeft ~/ 60;
    final seconds = _secondsLeft % 60;
    final timeDisplay = '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
    
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) async {
        if (!didPop) {
          await _cancel();
        }
      },
      child: Scaffold(
        backgroundColor: const Color(0xFFF5F7FA),
        appBar: AppBar(
          title: const Text(
            "Xác nhận đặt chỗ",
            style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black),
          ),
          backgroundColor: Colors.white,
          elevation: 0,
          centerTitle: true,
          iconTheme: const IconThemeData(color: Colors.black),
          leading: IconButton(
            icon: const Icon(Icons.close),
            onPressed: _cancel,
          ),
        ),
        body: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            children: [
              // Icon ghế
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: AppColors.brandColor.withAlpha(30),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.event_seat, size: 60, color: AppColors.brandColor),
              ),
              const SizedBox(height: 16),
              
              // Countdown timer
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                decoration: BoxDecoration(
                  color: _secondsLeft < 60 ? Colors.red[50] : Colors.orange[50],
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.timer, size: 18, color: _secondsLeft < 60 ? Colors.red : Colors.orange),
                    const SizedBox(width: 8),
                    Text(
                      'Còn $timeDisplay để xác nhận',
                      style: TextStyle(
                        color: _secondsLeft < 60 ? Colors.red : Colors.orange[800],
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              
              // Tiêu đề
              const Text(
                "Kiểm tra thông tin đặt chỗ",
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text(
                "Nhấn xác nhận để hoàn tất đặt chỗ",
                style: TextStyle(color: Colors.grey[600]),
              ),
              const SizedBox(height: 24),

              // Card thông tin chi tiết
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(20),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withAlpha(13),
                      blurRadius: 20,
                      offset: const Offset(0, 10),
                    ),
                  ],
                ),
                child: Column(
                  children: [
                    _buildInfoRow(Icons.location_on_rounded, "Khu vực", widget.zoneName),
                    const Divider(height: 30),
                    _buildInfoRow(Icons.chair_alt_rounded, "Mã ghế", widget.seat.seatCode),
                    const Divider(height: 30),
                    _buildInfoRow(Icons.calendar_month_rounded, "Ngày", DateFormat('EEEE, dd/MM/yyyy', 'vi').format(widget.date)),
                    const Divider(height: 30),
                    _buildInfoRow(Icons.access_time_rounded, "Khung giờ", widget.timeSlot),
                  ],
                ),
              ),
              const SizedBox(height: 32),

              // Buttons
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: _cancel,
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        side: BorderSide(color: Colors.grey[400]!),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                      child: const Text('Hủy', style: TextStyle(color: Colors.grey, fontSize: 16)),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    flex: 2,
                    child: ElevatedButton(
                      onPressed: _isBooking ? null : _confirmReservation,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.brandColor,
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                      child: _isBooking
                        ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                        : const Text('Xác nhận đặt chỗ', style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfoRow(IconData icon, String label, String value) {
    return Row(
      children: [
        Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            color: AppColors.brandColor.withAlpha(25),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(icon, color: AppColors.brandColor, size: 22),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: TextStyle(color: Colors.grey[600], fontSize: 13)),
              const SizedBox(height: 4),
              Text(value, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16)),
            ],
          ),
        ),
      ],
    );
  }

  void _showSuccessBottomSheet(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isDismissible: false,
      enableDrag: false,
      backgroundColor: Colors.transparent,
      builder: (context) => Container(
        padding: const EdgeInsets.all(32),
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.green[50],
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.check_circle, color: Colors.green, size: 60),
            ),
            const SizedBox(height: 20),
            const Text(
              "Đặt chỗ thành công!",
              style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            Text(
              "Ghế ${widget.seat.seatCode} đã được đặt\ncho ${DateFormat('dd/MM/yyyy').format(widget.date)} - ${widget.timeSlot}",
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey[600], fontSize: 15),
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () {
                  Navigator.popUntil(context, (route) => route.isFirst);
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.brandColor,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                child: const Text('Về trang chủ', style: TextStyle(color: Colors.white, fontSize: 16)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
