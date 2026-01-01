import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/views/home/widgets/home_appbar.dart';
import 'widgets/reputation_card.dart';
import 'widgets/quick_actions.dart';
import 'widgets/news_list.dart';

class HomeScreen extends StatelessWidget {
  final UserProfile? user;

  const HomeScreen({super.key, this.user});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.backgroundPrimary,
      body: Stack(
        children: [
          // 1. NỀN GRADIENT CAM (Cố định ở trên)
          Container(
            height: 270, // Chiều cao đủ để chứa Header + 1 nửa Card uy tín
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [
                  AppColors.brandColor,
                  AppColors.brandColor.withOpacity(0.8),
                ],
              ),
            ),
          ),

          // 2. NỘI DUNG CUỘN
          SafeArea(
            bottom: false,
            child: SingleChildScrollView(
              child: Column(
                children: [
                  const SizedBox(height: 20),

                  // A. Header (Chào hỏi & Thông báo)
                  Padding(
                    padding: EdgeInsets.symmetric(horizontal: 16.0),
                    child: HomeAppBar(user: user),
                  ),

                  const SizedBox(height: 30),

                  // B. Card Uy tín (Nằm đè lên ranh giới Cam/Trắng)
                  Padding(
                    padding: EdgeInsets.symmetric(horizontal: 16.0),
                    child: ReputationCard(user: user),
                  ),

                  const SizedBox(height: 30),

                  // C. PHẦN NỀN TRẮNG CHỨA CÁC TÍNH NĂNG (GIẢI PHÁP UI)
                  Container(
                    width: double.infinity,
                    decoration: const BoxDecoration(
                      color: AppColors.backgroundPrimary, // Hoặc Colors.white
                      borderRadius: BorderRadius.only(
                        topLeft: Radius.circular(30),
                        topRight: Radius.circular(30),
                      ),
                    ),
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: const [
                          // 1. Truy cập nhanh
                          QuickActions(),
                          SizedBox(height: 30),

                          // 2. Gợi ý AI
                          Text(
                            "Gợi ý thông minh",
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 18,
                            ),
                          ),
                          SizedBox(height: 16),
                          AICard(), // Tách ra widget riêng bên dưới
                          SizedBox(height: 30),

                          // 3. Tin tức
                          NewsList(),
                          SizedBox(
                            height: 80,
                          ), // Padding đáy để không bị BottomBar che
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// Widget AI Card đơn giản (có thể tách ra file riêng nếu muốn)
class AICard extends StatelessWidget {
  const AICard({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        gradient: const LinearGradient(
          colors: [Color(0xFF232526), Color(0xFF414345)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: const [
          BoxShadow(
            color: Colors.black26,
            blurRadius: 10,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () {},
          borderRadius: BorderRadius.circular(20),
          child: Padding(
            padding: const EdgeInsets.all(20.0),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.1),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.auto_awesome,
                    color: Colors.amberAccent,
                    size: 28,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: const [
                      Text(
                        "Giờ vàng học tập: 14:00 - 16:00",
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                          fontSize: 16,
                        ),
                      ),
                      SizedBox(height: 4),
                      Text(
                        "Thư viện vắng khách. Đặt ngay để có chỗ đẹp!",
                        style: TextStyle(color: Colors.white70, fontSize: 13),
                      ),
                    ],
                  ),
                ),
                const Icon(
                  Icons.arrow_forward_ios,
                  color: Colors.white54,
                  size: 16,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
