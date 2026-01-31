import 'package:flutter/material.dart';
import 'package:icons_plus/icons_plus.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/services/auth_service.dart';
import 'package:slib/main_screen.dart';
import 'package:slib/views/authentication/change_password_screen.dart';
import 'package:slib/views/authentication/forgot_password_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  bool _isLoading = false;
  bool _obscurePassword = true;
  bool _rememberMe = false;
  
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _formKey = GlobalKey<FormState>();

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  // --- XU LY DANG NHAP GOOGLE ---
  Future<void> _handleGoogleSignIn() async {
    _showLoadingDialog();

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final result = await authService.signInWithGoogle();

      if (mounted) {
        Navigator.of(context, rootNavigator: true).pop(); 
      }

      if (result != null) {
        _handleSuccessfulLogin(result);
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

  // --- XU LY DANG NHAP BANG MAT KHAU ---
  Future<void> _handlePasswordSignIn() async {
    if (!_formKey.currentState!.validate()) return;

    _showLoadingDialog();

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final result = await authService.signInWithPassword(
        _emailController.text.trim(),
        _passwordController.text,
      );

      if (mounted) {
        Navigator.of(context, rootNavigator: true).pop();
      }

      if (result != null) {
        _handleSuccessfulLogin(result);
      }
    } catch (e) {
      if (mounted) {
        Navigator.of(context, rootNavigator: true).pop();
      }

      String errorMessage = e.toString().replaceAll("Exception: ", "");
      _showErrorSnackBar(errorMessage);
    }
  }

  void _handleSuccessfulLogin(dynamic result) {
    if (!mounted) return;

    // Check if user needs to change password
    if (result.needsPasswordChange) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (_) => ChangePasswordScreen(isFirstLogin: true),
        ),
      );
    } else {
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
        child: Column(
          children: [
            // 1. HEADER (Màu cam + Logo + Title) - chiếm 50% màn hình
            _buildHeader(size),

            // 2. CARD ĐĂNG NHẬP
            Transform.translate(
              offset: const Offset(0, -100),
              child: Container(
                margin: const EdgeInsets.symmetric(horizontal: 20),
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 28),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(24),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.08),
                      blurRadius: 20,
                      offset: const Offset(0, 8),
                    ),
                  ],
                ),
                child: _buildLoginForm(),
              ),
            ),
          ],
        ),
      ),
    );
  }

  // --- UNIFIED LOGIN FORM ---
  Widget _buildLoginForm() {
    return Form(
      key: _formKey,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // NUT GOOGLE
          SizedBox(
            height: 52,
            width: double.infinity,
            child: OutlinedButton(
              onPressed: _handleGoogleSignIn,
              style: OutlinedButton.styleFrom(
                backgroundColor: Colors.white,
                side: BorderSide(color: Colors.grey[300]!),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(30),
                ),
                elevation: 0,
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Brand(Brands.google, size: 24),
                  const SizedBox(width: 12),
                  const Text(
                    "Tiếp tục với Google",
                    style: TextStyle(
                      color: Colors.black87,
                      fontWeight: FontWeight.w500,
                      fontSize: 15,
                    ),
                  ),
                ],
              ),
            ),
          ),
          
          const SizedBox(height: 24),
          
          // DIVIDER
          Row(
            children: [
              Expanded(child: Divider(color: Colors.grey[300], thickness: 1)),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Text(
                  "Hoặc",
                  style: TextStyle(color: Colors.grey[500], fontSize: 14),
                ),
              ),
              Expanded(child: Divider(color: Colors.grey[300], thickness: 1)),
            ],
          ),
          
          const SizedBox(height: 24),

          // EMAIL/MSSV FIELD
          TextFormField(
            controller: _emailController,
            keyboardType: TextInputType.text,
            decoration: InputDecoration(
              hintText: "Email FPT hoặc MSSV",
              hintStyle: TextStyle(color: Colors.grey[400], fontSize: 15),
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: Colors.grey[300]!),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: Colors.grey[300]!),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: AppColors.brandColor, width: 2),
              ),
            ),
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Vui lòng nhập email hoặc MSSV';
              }
              return null;
            },
          ),
          const SizedBox(height: 16),

          // PASSWORD FIELD
          TextFormField(
            controller: _passwordController,
            obscureText: _obscurePassword,
            decoration: InputDecoration(
              hintText: "Mật khẩu",
              hintStyle: TextStyle(color: Colors.grey[400], fontSize: 15),
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: Colors.grey[300]!),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: Colors.grey[300]!),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: AppColors.brandColor, width: 2),
              ),
              suffixIcon: IconButton(
                icon: Icon(
                  _obscurePassword ? Icons.visibility_off_outlined : Icons.visibility_outlined,
                  color: Colors.grey[400],
                  size: 22,
                ),
                onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
              ),
            ),
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Vui lòng nhập mật khẩu';
              }
              return null;
            },
          ),
          const SizedBox(height: 16),

          // REMEMBER ME & FORGOT PASSWORD ROW
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              // Checkbox ghi nho dang nhap
              Row(
                children: [
                  SizedBox(
                    width: 24,
                    height: 24,
                    child: Checkbox(
                      value: _rememberMe,
                      onChanged: (value) => setState(() => _rememberMe = value ?? false),
                      activeColor: AppColors.brandColor,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(4),
                      ),
                      side: BorderSide(color: Colors.grey[400]!, width: 1.5),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    "Ghi nhớ đăng nhập",
                    style: TextStyle(
                      fontSize: 13,
                      color: Colors.grey[700],
                    ),
                  ),
                ],
              ),
              // Link quen mat khau
              GestureDetector(
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => const ForgotPasswordScreen()),
                  );
                },
                child: const Text(
                  "Quên mật khẩu ?",
                  style: TextStyle(
                    fontSize: 13,
                    color: AppColors.brandColor,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 24),

          // LOGIN BUTTON
          SizedBox(
            height: 52,
            width: double.infinity,
            child: ElevatedButton(
              onPressed: _handlePasswordSignIn,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.brandColor,
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(30),
                ),
              ),
              child: const Text(
                "Đăng nhập",
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  // --- UI COMPONENTS ---
  Widget _buildHeader(Size size) {
    return Container(
      width: double.infinity,
      height: size.height * 0.5, // 50% màn hình
      padding: const EdgeInsets.symmetric(horizontal: 20),
      decoration: const BoxDecoration(
        color: AppColors.brandColor,
      ),
      child: SafeArea(
        bottom: false,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // Logo only (không có text Slib)
            Image.asset(
              "assets/images/logo_nencam.png",
              height: 120,
            ),
            // Title
            const Text(
              "Đăng nhập vào tài khoản\ncủa bạn",
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 26,
                fontWeight: FontWeight.bold,
                color: Colors.white,
                height: 1.3,
              ),
            ),
            const SizedBox(height: 12),
            // Subtitle
            Text(
              "Nhập email và mật khẩu để tiếp tục",
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 14,
                color: Colors.white.withOpacity(0.8),
              ),
            ),
            const SizedBox(height: 50),
          ],
        ),
      ),
    );
  }
}