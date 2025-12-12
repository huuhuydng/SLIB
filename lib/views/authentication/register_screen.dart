import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:icons_plus/icons_plus.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/views/authentication/login_screen.dart';
import 'package:slib/views/authentication/otp_screen.dart'; // Đảm bảo đã import file màu


class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  DateTime? _selectedDate;
  
  // 1. Khai báo FormKey để quản lý trạng thái form
  final _formKey = GlobalKey<FormState>();

  // 2. Khai báo các Controller để lấy dữ liệu nhập vào
  final TextEditingController _firstNameController = TextEditingController();
  final TextEditingController _lastNameController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _mssvController = TextEditingController();
  final TextEditingController _dateController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  bool _obscureText = true;

  @override
  void dispose() {
    _dateController.dispose();
    super.dispose();
  }

  //Validate
  String? _validateEmail(String? value) {
    if (value == null || value.isEmpty) {
      return 'Vui lòng nhập Email';
    }
    
    final emailRegex = RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$');
    if (!emailRegex.hasMatch(value)) {
      return 'Định dạng email không hợp lệ';
    }
    
    if (!value.endsWith('@fpt.edu.vn')) {
      return 'Vui lòng sử dụng Email FPT (@fpt.edu.vn)';
    }
    return null;
  }

  String? _validatePassword(String? value) {
    if (value == null || value.isEmpty) {
      return 'Vui lòng nhập mật khẩu';
    }
    if (value.length < 8) {
      return 'Mật khẩu phải có ít nhất 8 ký tự';
    }
    if (!value.contains(RegExp(r'[A-Z]'))) {
      return 'Cần ít nhất 1 chữ in hoa';
    }
    if (!value.contains(RegExp(r'[0-9]'))) {
      return 'Cần ít nhất 1 số';
    }
    if (!value.contains(RegExp(r'[!@#\$&*~]'))) {
      return 'Cần ít nhất 1 ký tự đặc biệt (!@#\$&*~)';
    }
    return null;
  }

  String? _validateRequired(String? value, String fieldName) {
    if (value == null || value.isEmpty) {
      return 'Vui lòng nhập $fieldName';
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    final topPadding = MediaQuery.of(context).padding.top;

    return Scaffold(
      backgroundColor: Colors.white,
      body: SingleChildScrollView( 
        child: Container(
          height: size.height, 
          child: Stack(
            children: [
              Positioned(
                top: 0,
                left: 0,
                right: 0,
                height: size.height * 0.5, 
                child: Container(
                  decoration: const BoxDecoration(
                    color: AppColors.brandColor, 
                    borderRadius: BorderRadius.vertical(bottom: Radius.circular(30)),
                  ),
                  child: Column(
                    children: [
                      SizedBox(height: topPadding + 40),
                      const Text(
                        "Tạo Tài Khoản Mới",
                        style: TextStyle(
                          fontSize: 32, 
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                      const SizedBox(height: 8),
                      RichText(
                        text: TextSpan(
                          text: "Đã có tài khoản? ",
                          style: const TextStyle(fontSize: 14, color: Colors.white70),
                          children: [
                            TextSpan(
                              text: "Đăng nhập ngay",
                              style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                                decoration: TextDecoration.underline,
                                decorationColor: Colors.white,
                              ),
                              recognizer: TapGestureRecognizer()
                                ..onTap = () {
                                  Navigator.pushReplacement(
                                    context,
                                    MaterialPageRoute(builder: (context) => LoginScreen()),
                                  );
                                },
                            )
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),

              Positioned(
                top: topPadding,
                left: 10,
                child: IconButton(
                  icon: const Icon(Icons.arrow_back_ios_new, color: Colors.white, size: 22),
                  onPressed: () => Navigator.pop(context),
                ),
              ),

              
              Positioned(
                top: size.height * 0.22, 
                left: 20,
                right: 20,
                bottom: 20, 
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 30),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(24),
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
                    child: SingleChildScrollView( 
                      physics: const BouncingScrollPhysics(),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Row(
                            children: [
                              Expanded(
                                flex: 3,
                                child: _buildTextField(
                                  controller: _firstNameController,
                                  label: "Tên",
                                  icon: Icons.person_outline,
                                  validator: (val) => _validateRequired(val, "Tên"),
                                ),
                              ),
                              const SizedBox(width: 16),
                              Expanded(
                                flex: 2,
                                child: _buildTextField(
                                  controller: _lastNameController,
                                  label: "Họ",
                                  icon: null,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 16),

                          _buildTextField(
                            controller: _emailController,
                            label: "Email FPT (fpt.edu.vn)",
                            icon: Icons.email_outlined,
                            validator: _validateEmail, 
                            keyboardType: TextInputType.emailAddress,
                          ),
                          const SizedBox(height: 16),

                          _buildTextField(
                            controller: _mssvController,
                            label: "MSSV (VD: DE180295)",
                            icon: Icons.badge_outlined,
                            validator: (val) => _validateRequired(val, "MSSV"),
                          ),
                          const SizedBox(height: 16),

                          // Input Ngày sinh
                          TextFormField( 
                            controller: _dateController,
                            readOnly: true,
                            validator: (val) => _validateRequired(val, "Ngày sinh"),
                            decoration: _inputDecoration("Ngày sinh (DD/MM/YYYY)", Icons.calendar_today_outlined),
                            onTap: () async {
                              DateTime? pickedDate = await showDatePicker(
                                context: context,
                                initialDate: DateTime.now(),
                                firstDate: DateTime(1980),
                                lastDate: DateTime.now(),
                                builder: (context, child) {
                                  return Theme(
                                    data: Theme.of(context).copyWith(
                                      colorScheme: ColorScheme.light(primary: AppColors.brandColor),
                                    ),
                                    child: child!,
                                  );
                                },
                              );
                              if (pickedDate != null) {
                                setState(() {
                                  String day = pickedDate.day.toString().padLeft(2, '0');
                                  String month = pickedDate.month.toString().padLeft(2, '0');
                                  String year = pickedDate.year.toString();
                                  _dateController.text = "$day/$month/$year";
                                });
                              }
                            },
                          ),
                          const SizedBox(height: 16),

                          // Input Mật khẩu
                          TextFormField( // Đổi thành TextFormField
                            controller: _passwordController,
                            obscureText: _obscureText,
                            validator: _validatePassword, // Gọi hàm validate Pass
                            decoration: InputDecoration(
                              labelText: "Mật khẩu",
                              labelStyle: TextStyle(color: Colors.grey[600], fontSize: 14),
                              errorMaxLines: 2, // Cho phép lỗi hiện 2 dòng nếu dài
                              enabledBorder: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(12),
                                borderSide: BorderSide(color: Colors.grey[300]!),
                              ),
                              focusedBorder: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(12),
                                borderSide: const BorderSide(color: AppColors.brandColor, width: 1.5),
                              ),
                              errorBorder: OutlineInputBorder( // Viền khi lỗi
                                borderRadius: BorderRadius.circular(12),
                                borderSide: const BorderSide(color: Colors.red, width: 1),
                              ),
                              focusedErrorBorder: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(12),
                                borderSide: const BorderSide(color: Colors.red, width: 1.5),
                              ),
                              prefixIcon: const Icon(Icons.lock_outline, color: Colors.grey),
                              suffixIcon: IconButton(
                                icon: Icon(
                                  _obscureText ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                                  color: Colors.grey,
                                ),
                                onPressed: () => setState(() => _obscureText = !_obscureText),
                              ),
                              filled: true,
                              fillColor: Colors.grey[50],
                            ),
                          ),

                          const SizedBox(height: 24),

                          // NÚT ĐĂNG KÝ
                          SizedBox(
                            height: 50,
                            child: ElevatedButton(
                              onPressed: () {
                                // --- LOGIC KIỂM TRA ---
                                if (_formKey.currentState!.validate()) {
                                  print("Họ tên: ${_firstNameController.text}");
                                  print("Email: ${_emailController.text}");
                                  
                                  Navigator.push(context, MaterialPageRoute(builder: (context) => OtpScreen()));
                                }
                              },
                              style: ElevatedButton.styleFrom(
                                backgroundColor: AppColors.brandColor,
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                                elevation: 2,
                              ),
                              child: const Text(
                                "ĐĂNG KÝ",
                                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white, letterSpacing: 1),
                              ),
                            ),
                          ),

                          const SizedBox(height: 24),

                          // Divider Hoặc
                          Row(
                            children: [
                              Expanded(child: Divider(color: Colors.grey[300])),
                              Padding(
                                padding: const EdgeInsets.symmetric(horizontal: 12),
                                child: Text("Hoặc", style: TextStyle(color: Colors.grey[500], fontSize: 13)),
                              ),
                              Expanded(child: Divider(color: Colors.grey[300])),
                            ],
                          ),

                          const SizedBox(height: 20),

                          // Nút Google
                          SizedBox(
                            height: 50,
                            child: OutlinedButton(
                              onPressed: () {},
                              style: OutlinedButton.styleFrom(
                                side: BorderSide(color: Colors.grey[300]!),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                                backgroundColor: Colors.white,
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Brand(Brands.google, size: 24),
                                  const SizedBox(width: 12),
                                  const Text(
                                    "Đăng ký với Google",
                                    style: TextStyle(color: Colors.black87, fontWeight: FontWeight.w600),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ),
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

  Widget _buildTextField({
    required String label,
    IconData? icon,
    TextEditingController? controller,
    String? Function(String?)? validator,
    TextInputType keyboardType = TextInputType.text,
  }) {
    return TextFormField(
      controller: controller,
      validator: validator,
      keyboardType: keyboardType,
      autovalidateMode: AutovalidateMode.onUserInteraction, // Báo lỗi ngay khi gõ sai
      decoration: _inputDecoration(label, icon),
    );
  }

  InputDecoration _inputDecoration(String label, IconData? icon) {
    return InputDecoration(
      labelText: label,
      labelStyle: TextStyle(color: Colors.grey[600], fontSize: 14),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: Colors.grey[300]!),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: AppColors.brandColor, width: 1.5),
      ),
      errorBorder: OutlineInputBorder( // Viền đỏ khi lỗi
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: Colors.red, width: 1),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: Colors.red, width: 1.5),
      ),
      prefixIcon: icon != null ? Icon(icon, color: Colors.grey, size: 22) : null,
      filled: true,
      fillColor: Colors.grey[50],
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
    );
  }
}