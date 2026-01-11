import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';

class AICard extends StatelessWidget {
  const AICard({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: const LinearGradient(
          colors: [Color(0xFF1E293B), Color(0xFF334155)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: const [
          BoxShadow(
            color: Colors.black26,
            blurRadius: 15,
            offset: Offset(0, 8),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () {},
          borderRadius: BorderRadius.circular(24),
          child: Padding(
            padding: const EdgeInsets.all(20.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header AI
                Row(
                  children: [
                    const Icon(Icons.auto_awesome,
                        color: Colors.amberAccent, size: 24),
                    const SizedBox(width: 10),
                    const Text(
                      "SLIB Intelligence",
                      style: TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                          fontSize: 14),
                    ),
                    const Spacer(),
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: const Text("BETA",
                          style:
                              TextStyle(color: Colors.white70, fontSize: 10)),
                    )
                  ],
                ),
                const SizedBox(height: 16),

                // Nội dung chính
                const Text(
                  "Dự báo: 14:00 - 16:00 thư viện sẽ vắng",
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                    fontSize: 16,
                  ),
                ),
                const SizedBox(height: 6),
                const Text(
                  "Đây là thời điểm lý tưởng để bạn đặt chỗ tại khu vực A (Yên tĩnh). Tỉ lệ ồn dự kiến < 20dB.",
                  style: TextStyle(
                      color: Colors.white70, fontSize: 13, height: 1.4),
                ),

                const SizedBox(height: 16),

                // Nút hành động
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: () {},
                    style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.brandColor,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12)),
                        elevation: 0),
                    child: const Text("Đặt chỗ ngay"),
                  ),
                )
              ],
            ),
          ),
        ),
      ),
    );
  }
}
