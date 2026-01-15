import 'package:flutter/material.dart';
import 'package:icons_plus/icons_plus.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/services/auth_service.dart';
import 'package:slib/main_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  bool _isLoading = false;

  // --- XỬ LÝ ĐĂNG NHẬP GOOGLE ---
  Future<void> _handleGoogleSignIn() async {
    _showLoadingDialog();

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final result = await authService.signInWithGoogle();

      if (mounted) {
        Navigator.of(context, rootNavigator: true).pop(); 
      }

      if (result != null) {
        if (mounted) {
          // Hiển thị thông báo thành công
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text("Xin chào ${result.fullName ?? 'Sinh viên'}!"),
              backgroundColor: AppColors.brandColor,
              behavior: SnackBarBehavior.floating,
            ),
          );


          Navigator.pushAndRemoveUntil(
            context,
            MaterialPageRoute(builder: (_) => const MainScreen()),
            (route) => false,
          );
        }
      } 

      
    } catch (e) {
      if (mounted) {
        Navigator.of(context, rootNavigator: true).pop();
      }

      String errorMessage = e.toString().replaceAll("Exception: ", "");
      
      if (errorMessage.contains("fpt.edu.vn")) {
        errorMessage = "Truy cập bị từ chối: Vui lòng dùng mail @fpt.edu.vn";
      }

      _showErrorSnackBar(errorMessage);
    }
  }

  void _showLoadingDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => const Center(
        child: CircularProgressIndicator(color: AppColors.brandColor),
      ),
    );
  }

  void _showErrorSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.red),
    );
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;

    return Scaffold(
      backgroundColor: Colors.white,
      body: SingleChildScrollView(
        child: SizedBox(
          height: size.height,
          child: Stack(
            children: [
              // 1. HEADER (Màu cam + Logo)
              _buildHeader(size),

              // 2. CARD ĐĂNG NHẬP (Chỉ chứa Google)
              Positioned(
                top: size.height * 0.42, 
                left: 20,
                right: 20,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 40),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.1),
                        blurRadius: 20,
                        offset: const Offset(0, 10),
                      ),
                    ],
                  ),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Text(
                        "Chào mừng trở lại!",
                        style: TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                          color: Colors.black87,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        "Vui lòng sử dụng email nhà trường\n(@fpt.edu.vn) để tiếp tục.",
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.grey[600],
                        ),
                      ),
                      const SizedBox(height: 30),

                      // NÚT GOOGLE
                      SizedBox(
                        height: 55,
                        width: double.infinity,
                        child: OutlinedButton(
                          onPressed: _handleGoogleSignIn,
                          style: OutlinedButton.styleFrom(
                            backgroundColor: Colors.white,
                            side: BorderSide(color: Colors.grey[300]!),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                            elevation: 0,
                          ),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Brand(Brands.google, size: 28),
                              const SizedBox(width: 12),
                              const Text(
                                "Tiếp tục với Google",
                                style: TextStyle(
                                  color: Colors.black87,
                                  fontWeight: FontWeight.w600,
                                  fontSize: 16,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                      
                      const SizedBox(height: 20),
                      // Footer nhỏ để giao diện đỡ trống
                      Text(
                        "Hệ thống dành riêng cho sinh viên & giảng viên\nĐại học FPT Đà Nẵng",
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.grey[400],
                          fontStyle: FontStyle.italic,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // --- UI COMPONENTS ---
  Widget _buildHeader(Size size) {
    return Positioned(
      top: 0,
      left: 0,
      right: 0,
      height: size.height * 0.55, // Kéo dài nền cam xuống thấp hơn chút để ôm trọn card
      child: Container(
        color: AppColors.brandColor,
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: SafeArea(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            children: [
              const SizedBox(height: 50), // Khoảng cách từ trên xuống
              Image.asset(
                          "assets/images/logo_nencam.png",
                          height: 120,
                        ),
              
              const SizedBox(height: 5),
              const Text(
                "Hệ thống Thư viện SLIB",
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 26,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                  letterSpacing: 0.5,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                "Đại học FPT Đà Nẵng",
                style: TextStyle(
                  fontSize: 16, 
                  color: Colors.white70,
                  fontWeight: FontWeight.w500
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}