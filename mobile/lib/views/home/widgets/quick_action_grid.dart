import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:slib/main_screen.dart';
import 'package:slib/views/checkin/qr_scan_screen.dart';
import 'package:slib/views/profile/booking_history_screen.dart';
import 'package:slib/views/profile/activity_history_screen.dart';
import 'package:slib/views/violation_report/violation_report_screen.dart';

class QuickActionGrid extends StatelessWidget {
  const QuickActionGrid({super.key});

  Future<void> _onCheckInPressed(BuildContext context) async {
    var status = await Permission.camera.request();
    if (status.isGranted) {
      if (context.mounted) {
        Navigator.push(
          context,
          MaterialPageRoute(builder: (context) => const QrScanScreen()),
        );
      }
    } else {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("Bạn cần cấp quyền Camera để quét mã!")),
        );
      }
    }
  }

  void _onActivityPressed(BuildContext context) {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const ActivityHistoryScreen()),
    );
  }

  void _onReportPressed(BuildContext context) {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const ViolationReportScreen()),
    );
  }

  void _onHistoryPressed(BuildContext context) {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const BookingHistoryScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        _buildActionButton(Icons.qr_code_scanner, "Check-in", Colors.orange, () { 
          _onCheckInPressed(context);
        }),
        _buildActionButton(Icons.timeline_outlined, "Hoạt động", Colors.blue, () {
          _onActivityPressed(context);
        }),
        _buildActionButton(Icons.warning_amber_rounded, "Báo cáo", Colors.amber, () {
          _onReportPressed(context);
        }),
        _buildActionButton(Icons.history, "Lịch sử", Colors.purple, () {
          _onHistoryPressed(context);
        }),
      ],
    );
  }

  Widget _buildActionButton(
      IconData icon, String label, Color color, VoidCallback onTap) {
    return Column(
      children: [
        InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(16),
          child: Container(
            width: 65,
            height: 65,
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(18),
              boxShadow: [
                BoxShadow(
                    color: Colors.grey.withOpacity(0.1),
                    blurRadius: 10,
                    offset: const Offset(0, 4))
              ],
            ),
            child: Icon(icon, color: color, size: 28),
          ),
        ),
        const SizedBox(height: 8),
        Text(label,
            style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: Colors.black87))
      ],
    );
  }
}
