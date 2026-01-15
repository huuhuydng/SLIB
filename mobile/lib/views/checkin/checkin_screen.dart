import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';

/// Màn hình check-in - Cho phép người dùng check-in vào thư viện bằng QR code
class CheckinScreen extends StatefulWidget {
  const CheckinScreen({super.key});

  @override
  State<CheckinScreen> createState() => _CheckinScreenState();
}

class _CheckinScreenState extends State<CheckinScreen> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Check-in'),
        backgroundColor: AppColors.brandColor,
        foregroundColor: Colors.white,
      ),
      body: const Center(
        child: Text('Check-in Screen - QR Scanner'),
      ),
    );
  }
}
