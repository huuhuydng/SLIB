import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:firebase_core/firebase_core.dart';
import 'firebase_options.dart';

// Import các file của bạn
import 'services/auth_service.dart';
import 'main_screen.dart'; // Đảm bảo import đúng đường dẫn file MainScreen của bạn
import 'views/authentication/on_boarding_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // --- FIX LỖI DUPLICATE APP (Giữ nguyên logic của bạn) ---
  try {
    if (Firebase.apps.isEmpty) {
      await Firebase.initializeApp(
        options: DefaultFirebaseOptions.currentPlatform,
      );
    } else {
      Firebase.app(); 
    }
  } catch (e) {
    print("Firebase init warning: $e");
  }
  // ----------------------------------------

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthService()),
      ],
      child: const MyApp(),
    ),
  );
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'SLIB App',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFFFF751F)),
        useMaterial3: true,
      ),
      home: const MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _handleNavigation());
  }

  Future<void> _handleNavigation() async {
    try {
      final AuthService authService = context.read<AuthService>();
      
      // 1. Chạy song song: Chờ 2 giây (để hiện Logo) VÀ Check Token
      final results = await Future.wait([
        Future.delayed(const Duration(seconds: 2)),
        authService.checkLoginStatus(),
      ]);

      // Kết quả checkLoginStatus nằm ở vị trí số 1
      final bool isLoggedIn = results[1] as bool;

      if (!mounted) return;

      // 2. Điều hướng dựa trên kết quả
      if (isLoggedIn) {
        // Đã đăng nhập -> Vào màn hình chính
        Navigator.pushReplacement(
          context, 
          MaterialPageRoute(builder: (_) => const MainScreen())
        );
      } else {
        // Chưa đăng nhập / Token hết hạn -> Ra màn hình giới thiệu
        Navigator.pushReplacement(
          context, 
          MaterialPageRoute(builder: (_) => const OnBoardingScreen())
        );
      }
    } catch (e) {
      // Nếu có lỗi bất ngờ, cứ đưa về Onboarding cho an toàn
      print("Lỗi Navigation: $e");
      if (mounted) {
        Navigator.pushReplacement(
            context, MaterialPageRoute(builder: (_) => const OnBoardingScreen()));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // Logo
            Image.asset(
              'assets/images/logo.png',
              width: 150,
              height: 150,
              // Thêm errorBuilder để tránh màn hình đỏ nếu lỡ file ảnh lỗi
              errorBuilder: (context, error, stackTrace) => const Icon(
                Icons.local_library_rounded, 
                size: 100, 
                color: Color(0xFFFF751F)
              ),
            ),
            const SizedBox(height: 20),
            // Loading
            const CircularProgressIndicator(color: Color(0xFFFF751F)),
          ],
        ),
      ),
    );
  }
}