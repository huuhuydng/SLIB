import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:icons_plus/icons_plus.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/services/auth_service.dart'; 
import 'package:slib/views/authentication/register_screen.dart';
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

  final AuthService _authService = AuthService();

  bool _obscureText = true;
  bool _remember = false;



  @override
  Widget build(BuildContext context) {
    // Lấy kích thước màn hình
    final size = MediaQuery.of(context).size;

    return Scaffold(
      backgroundColor: Colors.white, // Nền dưới cùng màu trắng
      body: SingleChildScrollView(
        // Cho phép cuộn để tránh bàn phím che
        child: SizedBox(
          height: size.height, // Đảm bảo chiều cao full màn hình
          child: Stack(
            children: [
              // 1. PHẦN NỀN CAM (HEADER)
              Positioned(
                top: 0,
                left: 0,
                right: 0,
                height: size.height * 0.45, // Chiếm 45% phía trên
                child: Container(
                  color: AppColors.brandColor, // Màu cam FPT
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: SafeArea(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        // LOGO SLIB (Giả lập theo ảnh mẫu nếu chưa có asset thật)
                        Image.asset(
                          "assets/images/logo_nencam.png",
                          height: 80,
                        ),

                        const SizedBox(height: 10), // Khoảng cách nhỏ
                        // TIÊU ĐỀ
                        const Text(
                          "Đăng nhập vào tài khoản\ncủa bạn",
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            fontSize: 26,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                            height: 1.2, // Khoảng cách dòng
                          ),
                        ),

                        const SizedBox(height: 3),

                        // SUBTITLE
                        const Text(
                          "Nhập email và mật khẩu để tiếp tục",
                          style: TextStyle(fontSize: 14, color: Colors.white70),
                        ),
                        const SizedBox(
                          height: 80,
                        ), 
                      ],
                    ),
                  ),
                ),
              ),

              Positioned(
                top: size.height * 0.32, // Đè lên nền cam (vị trí 32%)
                left: 20,
                right: 20,
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 24,
                    vertical: 30,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(20), // Bo góc card
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.1), // Bóng mờ nhẹ
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
                        // NÚT GOOGLE (Ở TRÊN CÙNG)
                        SizedBox(
                          height: 50,
                          width: double.infinity,
                          child: OutlinedButton(
                            onPressed: () {
                              // Xử lý đăng nhập Google
                            },
                            style: OutlinedButton.styleFrom(
                              side: BorderSide(color: Colors.grey[300]!),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                              backgroundColor: Colors.white,
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

                        // DIVIDER HOẶC
                        Row(
                          children: [
                            Expanded(child: Divider(color: Colors.grey[300])),
                            Padding(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 12,
                              ),
                              child: Text(
                                "Hoặc",
                                style: TextStyle(
                                  color: Colors.grey[500],
                                  fontSize: 13,
                                ),
                              ),
                            ),
                            Expanded(child: Divider(color: Colors.grey[300])),
                          ],
                        ),

                        const SizedBox(height: 24),

                        // INPUT EMAIL
                        TextFormField(
                          controller: _emailController,
                          decoration: _inputDecoration(
                            "Email FPT (fpt.edu.vn)",
                          ),
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return "Vui lòng nhập email";
                            }
                            return null;
                          },
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
                                color: Colors.grey,
                                size: 20,
                              ),
                              onPressed: () =>
                                  setState(() => _obscureText = !_obscureText),
                            ),
                          ),
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return "Vui lòng nhập mật khẩu";
                            }
                            return null;
                          },
                        ),

                        const SizedBox(height: 12),

                        // GHI NHỚ & QUÊN MẬT KHẨU
                        Row(
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
                                    shape: RoundedRectangleBorder(
                                      borderRadius: BorderRadius.circular(4),
                                    ),
                                    onChanged: (val) =>
                                        setState(() => _remember = val!),
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Text(
                                  "Ghi nhớ đăng nhập",
                                  style: TextStyle(
                                    color: Colors.grey[600],
                                    fontSize: 13,
                                  ),
                                ),
                              ],
                            ),
                            TextButton(
                              onPressed: () {
                                // Xử lý quên mật khẩu
                              },
                              style: TextButton.styleFrom(
                                padding: EdgeInsets.zero,
                              ),
                              child: const Text(
                                "Quên mật khẩu ?",
                                style: TextStyle(
                                  color: AppColors.brandColor, // Màu cam
                                  fontWeight: FontWeight.bold,
                                  fontSize: 13,
                                ),
                              ),
                            ),
                          ],
                        ),

                        const SizedBox(height: 20),

                        // NÚT ĐĂNG NHẬP (FULL WIDTH)
                        SizedBox(
                          width: double.infinity,
                          height: 52,
                          child: ElevatedButton(
                            onPressed: () async {
                              if (_formKey.currentState!.validate()) {
                                try {
                                  // Hiện loading
                                  showDialog(
                                    context: context,
                                    barrierDismissible: false,
                                    builder: (context) => const Center(
                                      child: CircularProgressIndicator(),
                                    ),
                                  );

                                  // Gọi API login
                                  final result = await _authService.login(
                                    _emailController.text,
                                    _passwordController.text,
                                  );

                                  // Đóng loading
                                  //mounted dùng để kiểm tra xem màn hình Login còn đang hiển thị không
                                  if (mounted) Navigator.pop(context);

                                  if (result != null) {
                                    // Đăng nhập thành công
                                    if (mounted) {
                                      Navigator.pushAndRemoveUntil(
                                        context,
                                        MaterialPageRoute(builder: (_) => const MainScreen()),
                                        (route) => false,
                                      );
                                    }
                                  }
                                } catch (e) {
                                  // Đóng loading nếu còn
                                  if (mounted) Navigator.pop(context);
                                  
                                  // Hiện lỗi
                                  if (mounted) {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      SnackBar(
                                        content: Text('Đăng nhập thất bại: $e'),
                                        backgroundColor: Colors.red,
                                      ),
                                    );
                                  }
                                }
                              }
                            },
                            style: ElevatedButton.styleFrom(
                              backgroundColor: AppColors.brandColor,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                              elevation: 2,
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

                        // CHƯA CÓ TÀI KHOẢN?
                        RichText(
                          text: TextSpan(
                            text: "Chưa có tài khoản? ",
                            style: TextStyle(
                              color: Colors.grey[600],
                              fontSize: 14,
                            ),
                            children: [
                              TextSpan(
                                text: "Đăng kí ngay",
                                style: const TextStyle(
                                  color: AppColors.brandColor,
                                  fontWeight: FontWeight.bold,
                                ),
                                recognizer: TapGestureRecognizer()
                                  ..onTap = () {
                                    Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                        builder: (context) => RegisterScreen(),
                                      ),
                                    );
                                  },
                              ),
                            ],
                          ),
                        ),
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

  // Helper để tạo Style cho Input giống nhau
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
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: Colors.red, width: 1),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: Colors.red, width: 1.5),
      ),
      filled: true,
      fillColor: Colors.white, 
    );
  }
}
