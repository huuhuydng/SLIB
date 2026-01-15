import 'package:flutter/material.dart';

class UpcomingBookingCard extends StatelessWidget {
  const UpcomingBookingCard({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFEFF6FF),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.blue.withOpacity(0.1)),
      ),
      child: Row(
        children: [
          // Cột ngày tháng
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(14),
              boxShadow: [
                BoxShadow(color: Colors.blue.withOpacity(0.05), blurRadius: 10)
              ],
            ),
            child: Column(
              children: const [
                Text("TH 2",
                    style: TextStyle(color: Colors.grey, fontSize: 12)),
                Text("24",
                    style: TextStyle(
                        color: Colors.blue,
                        fontWeight: FontWeight.bold,
                        fontSize: 20)),
              ],
            ),
          ),
          const SizedBox(width: 16),
          // Thông tin Booking
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  "Đặt chỗ sắp tới",
                  style: TextStyle(
                      color: Colors.blue,
                      fontSize: 12,
                      fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 4),
                const Text(
                  "Phòng Im lặng - Tầng 2",
                  style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                      color: Colors.black87),
                ),
                const SizedBox(height: 4),
                Row(
                  children: const [
                    Icon(Icons.access_time_rounded, size: 14, color: Colors.grey),
                    SizedBox(width: 4),
                    Text("14:00 - 16:00",
                        style: TextStyle(color: Colors.grey, fontSize: 13)),
                    SizedBox(width: 10),
                    Icon(Icons.chair_alt_outlined, size: 14, color: Colors.grey),
                    SizedBox(width: 4),
                    Text("Ghế A12",
                        style: TextStyle(color: Colors.grey, fontSize: 13)),
                  ],
                )
              ],
            ),
          ),
          // Nút Check-in nhanh
          IconButton(
            onPressed: () {},
            icon: const Icon(Icons.arrow_forward_ios_rounded,
                size: 18, color: Colors.blue),
            style: IconButton.styleFrom(backgroundColor: Colors.white),
          )
        ],
      ),
    );
  }
}
