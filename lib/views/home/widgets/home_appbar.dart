import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';

class HomeAppBar extends StatelessWidget {
  const HomeAppBar({super.key});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: const [
            Text("Chào Huy,",
                style: TextStyle(fontSize: 26, fontWeight: FontWeight.bold, color: Colors.white)),
            SizedBox(height: 4),
            Text("Hôm nay bạn muốn học ở đâu?",
                style: TextStyle(color: Colors.white70, fontSize: 15)),
          ],
        ),
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
                      color: AppColors.error,
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