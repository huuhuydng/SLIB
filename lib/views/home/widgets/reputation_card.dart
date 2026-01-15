import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/user_profile.dart';

class ReputationCard extends StatelessWidget {
  final UserProfile? user;
  const ReputationCard({super.key, this.user});

  @override
  Widget build(BuildContext context) {
    // Nếu user chưa có dữ liệu → hiển thị loading
    if (user == null) {
      return const Center(
        child: CircularProgressIndicator(color: AppColors.brandColor),
      );
    }

    final int score = user!.reputationScore;
    String rank;
    String message;

    if (score >= 90) {
      rank = "Hạng: Gương mẫu";
      message = "Tuyệt vời! Bạn đang giữ kỷ lục check-in rất tốt.";
    } else if (score >= 80) {
      rank = "Hạng: Khá";
      message = "Bạn đang làm tốt! Hãy cố gắng hơn nữa.";
    } else if (score >= 60) {
      rank = "Hạng: Trung bình";
      message = "Cần cải thiện thêm để nâng cao uy tín.";
    } else {
      rank = "Hạng: Kém";
      message = "Hãy tuân thủ quy định thư viện!";
    }

    return Card(
      elevation: 8,
      shadowColor: AppColors.brandColor.withOpacity(0.3),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      color: Colors.white,
      surfaceTintColor: Colors.white,
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
                  CircularProgressIndicator(
                    value: 1.0 - score / 100,
                    strokeWidth: 8,
                    color: AppColors.brandColor.withOpacity(0.1),
                  ),
                  CircularProgressIndicator(
                    value: score / 100,
                    strokeWidth: 8,
                    strokeCap: StrokeCap.round,
                    color: AppColors.brandColor,
                  ),
                  Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          "$score",
                          style: const TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.bold,
                            color: AppColors.brandColor,
                          ),
                        ),
                        const Text(
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
                    children: [
                      Text(
                        rank,
                        style: const TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 18,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(width: 5),
                      const Icon(
                        Icons.verified,
                        color: AppColors.success,
                        size: 20,
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    message,
                    style: const TextStyle(
                      fontSize: 14,
                      color: AppColors.textGrey,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
