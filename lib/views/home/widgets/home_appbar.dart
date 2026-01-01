import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/user_profile.dart';

class HomeAppBar extends StatelessWidget {
  final UserProfile? user;
  const HomeAppBar({super.key, this.user});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      crossAxisAlignment: CrossAxisAlignment.start, // Căn lề trên để icon không bị lệch khi text xuống dòng nhiều
      children: [
        // 1. Dùng Expanded để giới hạn chiều rộng, giúp text tự xuống dòng
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                "Chào ${user?.fullName ?? 'Bạn'},",
                style: const TextStyle(
                  fontSize: 26, 
                  fontWeight: FontWeight.bold, 
                  color: Colors.white,
                ),
                maxLines: 2, // Giới hạn tối đa 2 dòng (tùy chọn)
                overflow: TextOverflow.ellipsis, // Nếu dài hơn 2 dòng thì hiện dấu ...
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

        // 2. Icon thông báo giữ nguyên
        Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.2), shape: BoxShape.circle),
          child: Stack(
            children: [
              const Icon(Icons.notifications_outlined, size: 28, color: Colors.white),
              Positioned(
                right: 0,
                top: 0,
                child: Container(
                  padding: const EdgeInsets.all(2),
                  decoration: BoxDecoration(
                      color: AppColors.error, // Đảm bảo AppColors đã được import đúng
                      shape: BoxShape.circle,
                      border: Border.all(color: Colors.white, width: 1.5)),
                  constraints: const BoxConstraints(minWidth: 16, minHeight: 16),
                  child: const Text('1',
                      style: TextStyle(
                          color: Colors.white,
                          fontSize: 10,
                          fontWeight: FontWeight.bold),
                      textAlign: TextAlign.center),
                ),
              )
            ],
          ),
        )
      ],
    );
  }
}