import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/services/notification/notification_service.dart';
import 'package:slib/views/notification/notification_screen.dart';

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

        // 2. Icon thông báo với unread count từ NotificationService
        Consumer<NotificationService>(
          builder: (context, notificationService, child) {
            final unreadCount = notificationService.unreadCount;
            
            return GestureDetector(
              onTap: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => const NotificationScreen(),
                  ),
                );
              },
              child: Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.2), 
                  shape: BoxShape.circle,
                ),
                child: Stack(
                  children: [
                    const Icon(
                      Icons.notifications_outlined, 
                      size: 28, 
                      color: Colors.white,
                    ),
                    if (unreadCount > 0)
                      Positioned(
                        right: 0,
                        top: 0,
                        child: Container(
                          padding: const EdgeInsets.all(2),
                          decoration: BoxDecoration(
                            color: AppColors.error,
                            shape: BoxShape.circle,
                            border: Border.all(color: Colors.white, width: 1.5),
                          ),
                          constraints: const BoxConstraints(
                            minWidth: 16, 
                            minHeight: 16,
                          ),
                          child: Text(
                            unreadCount > 99 ? '99+' : '$unreadCount',
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 10,
                              fontWeight: FontWeight.bold,
                            ),
                            textAlign: TextAlign.center,
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            );
          },
        ),
      ],
    );
  }
}