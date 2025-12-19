import 'package:flutter/material.dart';
import 'package:slib/main_screen.dart';
import 'views/authentication/on_boarding_screen.dart';
import 'views/home/home_screen.dart'; 
import 'services/auth_service.dart'; 

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'SLIB App',
      theme: ThemeData(
        // Cập nhật màu chủ đạo theo SLIB (Cam)
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFFFF751F)),
        useMaterial3: true,
      ),
      home: const MyHomePage(), // Màn hình Splash
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  final AuthService _authService = AuthService();

  @override
  void initState() {
    super.initState();
    _handleNavigation();
  }

  Future<void> _handleNavigation() async {
    // 1. Bắt đầu chờ 2 giây (để giữ Logo hiển thị cho đẹp)
    final waitTask = Future.delayed(const Duration(seconds: 2));
    
    // 2. Kiểm tra trạng thái đăng nhập
    // Hàm này trả về true nếu đã login, false nếu chưa
    final checkAuthTask = _authService.checkLoginStatus(); 

    // 3. Chờ cả 2 việc trên hoàn thành (dùng Future.wait để chạy song song)
    final results = await Future.wait([waitTask, checkAuthTask]);
    final bool isLoggedIn = results[1] as bool;

    if (!mounted) return; 

    if (isLoggedIn) {
      _navigateTo(const MainScreen()); 
    } else {
      _navigateTo(const OnBoardingScreen());
    }
  }

  // Hàm chuyển trang dùng hiệu ứng Fade (Mờ dần) 
  void _navigateTo(Widget targetScreen) {
    Navigator.pushReplacement(
      context,
      PageRouteBuilder(
        transitionDuration: const Duration(milliseconds: 500),
        pageBuilder: (context, animation, secondaryAnimation) => targetScreen,
        transitionsBuilder: (context, animation, secondaryAnimation, child) {
          return FadeTransition(
            opacity: animation,
            child: child,
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white, 
      body: Center(
        // Logo
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Image.asset(
              'assets/images/logo.png',
              width: 150, 
              height: 150,
            ),
            const SizedBox(height: 20),
            const CircularProgressIndicator(
              color: Color(0xFFFF751F), // Màu cam SLIB
            ),
          ],
        ),
      ),
    );
  }
}