import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/views/card/hce_screen.dart';
import 'package:slib/views/home/widgets/booking_zone.dart';
import 'package:slib/views/history/history_screen.dart';
import 'package:slib/views/map/map_screen.dart';
// Import các màn hình đích

class QuickActions extends StatelessWidget {
  const QuickActions({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("Truy cập nhanh",
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
        const SizedBox(height: 16),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            _buildItem(context, Icons.chair_alt_rounded, "Đặt chỗ", Colors.blue,
                () => Navigator.push(context, MaterialPageRoute(builder: (_) => BookingZoneScreen(zones: [])))),
            _buildItem(context, Icons.badge_outlined, "Thẻ SV", AppColors.brandColor,
                () => Navigator.push(context, MaterialPageRoute(builder: (_) => const HceCardScreen()))),
            _buildItem(context, Icons.history_rounded, "Lịch sử", Colors.purple,
                () => Navigator.push(context, MaterialPageRoute(builder: (_) => const HistoryScreen()))),
            _buildItem(context, Icons.map_rounded, "Sơ đồ", Colors.teal,
                () => Navigator.push(context, MaterialPageRoute(builder: (_) => const MapScreen()))),
          ],
        ),
      ],
    );
  }

  Widget _buildItem(BuildContext context, IconData icon, String label, Color color, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: color.withOpacity(0.1),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: color.withOpacity(0.3), width: 1),
            ),
            child: Icon(icon, color: color, size: 28),
          ),
          const SizedBox(height: 10),
          Text(label, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
        ],
      ),
    );
  }
}