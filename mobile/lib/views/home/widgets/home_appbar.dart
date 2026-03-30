import 'package:flutter/material.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/views/home/widgets/notification_bell_button.dart';

class HomeAppBar extends StatelessWidget {
  final UserProfile? user;
  const HomeAppBar({super.key, this.user});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 1. Dùng Expanded để giới hạn chiều rộng, giúp text tự xuống dòng
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                "Xin chào ${user?.fullName ?? 'Bạn'},",
                style: const TextStyle(
                  fontSize: 26,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 4),
              const Text(
                "Hôm nay bạn muốn học ở đâu?",
                style: TextStyle(color: Colors.white70, fontSize: 20),
              ),
            ],
          ),
        ),

        // Tạo khoảng cách nhỏ giữa Text và Icon để không bị dính sát
        const SizedBox(width: 12),

        const NotificationBellButton(),
      ],
    );
  }
}
