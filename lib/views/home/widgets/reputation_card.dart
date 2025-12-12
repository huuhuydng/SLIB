import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';

class ReputationCard extends StatelessWidget {
  const ReputationCard({super.key});

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 8,
      shadowColor: AppColors.brandColor.withOpacity(0.3),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      color: Colors.white,
      surfaceTintColor: Colors.white, // Đảm bảo nền trắng không bị ám màu
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Row(
          children: [
            // 1. Vòng tròn điểm số
            SizedBox(
              width: 80,
              height: 80,
              child: Stack(
                fit: StackFit.expand,
                children: [
                  // Vòng nền mờ
                  CircularProgressIndicator(
                    value: 1.0,
                    strokeWidth: 8,
                    color: AppColors.brandColor.withOpacity(0.1),
                  ),
                  // Vòng tiến độ thực tế (90%)
                  const CircularProgressIndicator(
                    value: 0.9,
                    strokeWidth: 8,
                    strokeCap: StrokeCap.round,
                    color: AppColors.brandColor,
                  ),
                  // Số điểm ở giữa
                  Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: const [
                        Text(
                          "90",
                          style: TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.bold,
                            color: AppColors.brandColor,
                          ),
                        ),
                        Text(
                          "/100",
                          style: TextStyle(fontSize: 12, color: Colors.grey),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 20),
            
            // 2. Thông tin hạng
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: const [
                      Text(
                        "Hạng: Gương mẫu",
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 18,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      SizedBox(width: 5),
                      Icon(Icons.verified, color: AppColors.success, size: 20)
                    ],
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    "Tuyệt vời! Bạn đang giữ kỷ lục check-in rất tốt.",
                    style: TextStyle(fontSize: 14, color: AppColors.textGrey),
                  ),
                ],
              ),
            )
          ],
        ),
      ),
    );
  }
}