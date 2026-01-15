import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';

/// Màn hình đặt chỗ - Cho phép người dùng đặt chỗ ngồi trong thư viện
class BookingScreen extends StatefulWidget {
  const BookingScreen({super.key});

  @override
  State<BookingScreen> createState() => _BookingScreenState();
}

class _BookingScreenState extends State<BookingScreen> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Đặt chỗ'),
        backgroundColor: AppColors.brandColor,
        foregroundColor: Colors.white,
      ),
      body: const Center(
        child: Text('Booking Screen - Đang phát triển'),
      ),
    );
  }
}
