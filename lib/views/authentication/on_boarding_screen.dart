import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart'; 
import 'login_screen.dart';
// import 'register_screen.dart'; // Đã bỏ màn hình đăng ký

class OnBoardingScreen extends StatefulWidget {
  const OnBoardingScreen({super.key});

  @override
  State<OnBoardingScreen> createState() => _OnBoardingScreenState();
}

class _OnBoardingScreenState extends State<OnBoardingScreen> {
  final PageController _pageController = PageController();
  int _currentIndex = 0;

  final List<Map<String, String>> onboardingData = [
    {
      "image": "assets/images/on_boarding_1.png",
      "title": "Thư viện thông minh\ntrong tầm tay!",
      "desc": "Khám phá SLIB – nơi kết nối và quản lý mọi hoạt động thư viện chỉ với một ứng dụng duy nhất."
    },
    {
      "image": "assets/images/on_boarding_2.png",
      "title": "Check-in siêu tốc\nvới một chạm",
      "desc": "Dùng điện thoại như thẻ từ. Vào/ra thư viện tiện lợi và hiện đại bằng công nghệ HCE không tiếp xúc."
    },
    {
      "image": "assets/images/on_boarding_3.png",
      "title": "Đặt chỗ chính xác\ntừng vị trí",
      "desc": "Xem sơ đồ chỗ trống theo thời gian thực và giữ chỗ ngồi yêu thích của bạn chỉ trong vài giây."
    },
    {
      "image": "assets/images/on_boarding_4.png",
      "title": "Trợ lý AI hỗ trợ\nhọc tập hiệu quả",
      "desc": "Nhận gợi ý giờ vàng học tập và giải đáp thắc mắc 24/7 cùng Chatbot AI thông minh."
    }
  ];

  void _nextPage() {
    if (_currentIndex < onboardingData.length - 1) {
      _pageController.nextPage(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    } else {
      // 👉 SỬA ĐỔI: Chuyển thẳng sang LoginScreen
      // Dùng pushReplacement để người dùng không back lại onboarding được nữa
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (context) => const LoginScreen(),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;

    return Scaffold(
      backgroundColor: AppColors.backgroundPrimary, 
      body: SafeArea(
        child: Column(
          children: [
            // Phần Slider ảnh và nội dung
            Expanded(
              flex: 3,
              child: PageView.builder(
                controller: _pageController,
                itemCount: onboardingData.length,
                onPageChanged: (index) {
                  setState(() {
                    _currentIndex = index;
                  });
                },
                itemBuilder: (context, index) {
                  return SingleChildScrollView(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const SizedBox(height: 20),
                        Image.asset(
                          onboardingData[index]["image"]!,
                          height: size.height * 0.35, 
                          fit: BoxFit.contain,
                        ),
                        SizedBox(height: size.height * 0.05),
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 30),
                          child: Text(
                            onboardingData[index]["title"]!,
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              fontSize: 26,
                              fontWeight: FontWeight.bold,
                              color: AppColors.textPrimary,
                              height: 1.2,
                            ),
                          ),
                        ),
                        const SizedBox(height: 16),
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 40),
                          child: Text(
                            onboardingData[index]["desc"]!,
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              fontSize: 15,
                              color: Colors.grey,
                              height: 1.5, 
                            ),
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),

            // Phần Footer (Chấm tròn + Nút bấm)
            Expanded(
              flex: 1,
              child: Column(
                children: [
                  const Spacer(),
                
                  // Chấm tròn chỉ số trang
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: List.generate(
                      onboardingData.length,
                      (index) => AnimatedContainer(
                        duration: const Duration(milliseconds: 300),
                        margin: const EdgeInsets.symmetric(horizontal: 4),
                        width: _currentIndex == index ? 24 : 8, 
                        height: 8,
                        decoration: BoxDecoration(
                          color: _currentIndex == index
                              ? AppColors.brandColor 
                              : AppColors.brandColor.withOpacity(0.2), 
                          borderRadius: BorderRadius.circular(4),
                        ),
                      ),
                    ),
                  ),
                  
                  const Spacer(),

                  // Nút Tiếp tục / Bắt đầu ngay
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    child: SizedBox(
                      width: double.infinity,
                      height: 52, 
                      child: ElevatedButton(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.brandColor,
                          elevation: 2,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12), 
                          ),
                        ),
                        onPressed: _nextPage,
                        child: Text(
                          _currentIndex == onboardingData.length - 1
                              ? "Bắt đầu ngay"
                              : "Tiếp tục",
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w600,
                            color: Colors.white,
                          ),
                        ),
                      ),
                    ),
                  ),

                  // 👉 SỬA ĐỔI: Đã xóa phần RichText "Bạn đã có tài khoản?" ở đây
                  
                  const SizedBox(height: 30), // Padding đáy cho thoáng
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}