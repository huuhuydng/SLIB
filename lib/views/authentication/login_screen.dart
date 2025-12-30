import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:icons_plus/icons_plus.dart';
import 'package:provider/provider.dart'; // Đảm bảo bạn dùng Provider để gọi AuthService
import 'package:slib/assets/colors.dart';
import 'package:slib/services/auth_service.dart';
import 'package:slib/services/hce_bridge.dart';
import 'package:slib/views/authentication/register_screen.dart';
import 'package:slib/views/authentication/forgot_password_screen.dart';
import 'package:slib/main_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();

  bool _obscureText = true;
  bool _remember = false;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadSavedCredentials();
  }

  Future<void> _loadSavedCredentials() async {
    final authService = Provider.of<AuthService>(context, listen: false);
    final credentials = await authService.getSavedCredentials();
    if (credentials != null) {
      setState(() {
        _emailController.text = credentials['email'] ?? '';
        _passwordController.text = credentials['password'] ?? '';
        _remember = true;
      });
    }
    setState(() => _isLoading = false);
  }

  Future<void> _handleGoogleSignIn() async {
    try {
      _showLoadingDialog();

      final authService = Provider.of<AuthService>(context, listen: false);
      final result = await authService.signInWithGoogle();

      if (mounted) Navigator.pop(context); // Đóng Loading

      if (result != null) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text("Chào mừng ${result.fullName}!")),
          );

          Navigator.pushAndRemoveUntil(
            context,
            MaterialPageRoute(builder: (_) => const LoginScreen()),
            (route) => false,
          );
        }
      }
    } catch (e) {
      if (mounted) Navigator.pop(context);
      _showErrorSnackBar(e.toString().replaceAll("Exception: ", ""));
    }
  }

  // --- HÀM XỬ LÝ ĐĂNG NHẬP EMAIL/PASS ---
  Future<void> _handleEmailLogin() async {
    if (_formKey.currentState!.validate()) {
      try {
        _showLoadingDialog();

        final authService = Provider.of<AuthService>(context, listen: false);
        final result = await authService.login(
          _emailController.text.trim(),
          _passwordController.text.trim(),
        );

        if (mounted) Navigator.pop(context);

        if (result != null) {
          // Xử lý Ghi nhớ đăng nhập
          if (_remember) {
            await authService.saveCredentials(
              _emailController.text,
              _passwordController.text,
            );
          } else {
            await authService.clearSavedCredentials();
          }

          if (mounted) {
            Navigator.pushAndRemoveUntil(
              context,
              MaterialPageRoute(builder: (_) => const MainScreen()),
              (route) => false,
            );
          }
        }
      } catch (e) {
        if (mounted) Navigator.pop(context);
        _showErrorSnackBar(e.toString());
      }
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

    if (_isLoading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      backgroundColor: Colors.white,
      body: SingleChildScrollView(
        child: SizedBox(
          height: size.height,
          child: Stack(
            children: [
              _buildHeader(size),

              // 2. FORM ĐĂNG NHẬP
              Positioned(
                top: size.height * 0.32,
                left: 20,
                right: 20,
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 24,
                    vertical: 30,
                  ),
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
                  child: Form(
                    key: _formKey,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        // NÚT GOOGLE
                        SizedBox(
                          height: 50,
                          width: double.infinity,
                          child: OutlinedButton(
                            onPressed:
                                _handleGoogleSignIn, // <-- GỌI HÀM GOOGLE
                            style: OutlinedButton.styleFrom(
                              side: BorderSide(color: Colors.grey[300]!),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                            ),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Brand(Brands.google, size: 24),
                                const SizedBox(width: 10),
                                const Text(
                                  "Tiếp tục với Google",
                                  style: TextStyle(
                                    color: Colors.black87,
                                    fontWeight: FontWeight.bold,
                                    fontSize: 15,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),

                        const SizedBox(height: 24),
                        _buildDivider(),
                        const SizedBox(height: 24),

                        // INPUT EMAIL
                        TextFormField(
                          controller: _emailController,
                          decoration: _inputDecoration(
                            "Email FPT (fpt.edu.vn)",
                          ),
                          validator: (value) => (value == null || value.isEmpty)
                              ? "Vui lòng nhập email"
                              : null,
                        ),
                        const SizedBox(height: 16),

                        // INPUT PASSWORD
                        TextFormField(
                          controller: _passwordController,
                          obscureText: _obscureText,
                          decoration: _inputDecoration("Mật khẩu").copyWith(
                            suffixIcon: IconButton(
                              icon: Icon(
                                _obscureText
                                    ? Icons.visibility_off_outlined
                                    : Icons.visibility_outlined,
                                size: 20,
                              ),
                              onPressed: () =>
                                  setState(() => _obscureText = !_obscureText),
                            ),
                          ),
                          validator: (value) => (value == null || value.isEmpty)
                              ? "Vui lòng nhập mật khẩu"
                              : null,
                        ),

                        const SizedBox(height: 12),
                        _buildRememberForgot(),
                        const SizedBox(height: 20),

                        // NÚT ĐĂNG NHẬP THƯỜNG
                        SizedBox(
                          width: double.infinity,
                          height: 52,
                          child: ElevatedButton(
                            onPressed:
                                _handleEmailLogin, // <-- GỌI HÀM EMAIL/PASS
                            style: ElevatedButton.styleFrom(
                              backgroundColor: AppColors.brandColor,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                            ),
                            child: const Text(
                              "Đăng nhập",
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ),

                        const SizedBox(height: 20),
                        _buildRegisterLink(),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // --- CÁC WIDGET PHỤ TRỢ (Để code chính sạch sẽ hơn) ---

  Widget _buildHeader(Size size) {
    return Positioned(
      top: 0,
      left: 0,
      right: 0,
      height: size.height * 0.45,
      child: Container(
        color: AppColors.brandColor,
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: SafeArea(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Image.asset("assets/images/logo_nencam.png", height: 80),
              const SizedBox(height: 10),
              const Text(
                "Đăng nhập vào tài khoản\ncủa bạn",
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 26,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                  height: 1.2,
                ),
              ),
              const SizedBox(height: 3),
              const Text(
                "Nhập email và mật khẩu để tiếp tục",
                style: TextStyle(fontSize: 14, color: Colors.white70),
              ),
              const SizedBox(height: 80),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDivider() {
    return Row(
      children: [
        Expanded(child: Divider(color: Colors.grey[300])),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Text(
            "Hoặc",
            style: TextStyle(color: Colors.grey[500], fontSize: 13),
          ),
        ),
        Expanded(child: Divider(color: Colors.grey[300])),
      ],
    );
  }

  Widget _buildRememberForgot() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Row(
          children: [
            SizedBox(
              width: 24,
              height: 24,
              child: Checkbox(
                value: _remember,
                activeColor: AppColors.brandColor,
                onChanged: (val) => setState(() => _remember = val!),
              ),
            ),
            const SizedBox(width: 8),
            Text(
              "Ghi nhớ đăng nhập",
              style: TextStyle(color: Colors.grey[600], fontSize: 13),
            ),
          ],
        ),
        TextButton(
          onPressed: () => Navigator.push(
            context,
            MaterialPageRoute(builder: (_) => const ForgotPasswordScreen()),
          ),
          child: const Text(
            "Quên mật khẩu ?",
            style: TextStyle(
              color: AppColors.brandColor,
              fontWeight: FontWeight.bold,
              fontSize: 13,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildRegisterLink() {
    return RichText(
      text: TextSpan(
        text: "Chưa có tài khoản? ",
        style: TextStyle(color: Colors.grey[600], fontSize: 14),
        children: [
          TextSpan(
            text: "Đăng kí ngay",
            style: const TextStyle(
              color: AppColors.brandColor,
              fontWeight: FontWeight.bold,
            ),
            recognizer: TapGestureRecognizer()
              ..onTap = () => Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => RegisterScreen()),
              ),
          ),
        ],
      ),
    );
  }

  InputDecoration _inputDecoration(String hint) {
    return InputDecoration(
      hintText: hint,
      hintStyle: TextStyle(color: Colors.grey[400], fontSize: 14),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: Colors.grey[200]!),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: AppColors.brandColor, width: 1.5),
      ),
      filled: true,
      fillColor: Colors.white,
    );
  }
}
