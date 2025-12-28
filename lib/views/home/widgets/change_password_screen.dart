import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/services/auth_service.dart';
import 'package:slib/views/authentication/login_screen.dart';

class ChangePasswordScreen extends StatefulWidget {
  final UserProfile? user;
  const ChangePasswordScreen({super.key, this.user});

  @override
  State<ChangePasswordScreen> createState() => _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends State<ChangePasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _oldPasswordController = TextEditingController();
  final _newPasswordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();

  bool _isLoading = false;
  bool _obscureOld = true;
  bool _obscureNew = true;
  bool _obscureConfirm = true;

  final _authService = AuthService();

  @override
  void dispose() {
    _oldPasswordController.dispose();
    _newPasswordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
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

  Future<void> _changePassword() async {
    if (_formKey.currentState!.validate()) {
      setState(() => _isLoading = true);
      try {
        // 1. Lấy email đã lưu
        final savedEmail = widget.user?.email;

        if (savedEmail == null) {
          Navigator.pushReplacement(
            context,
            MaterialPageRoute(
              builder: (context) => const LoginScreen(),
            ),
          );
          // Không có email → không cho đổi
          throw Exception(
            "Không tìm thấy email đã lưu để xác thực mật khẩu cũ. Vui lòng đăng nhập lại.",
          );
        }

        // 2. Xác thực mật khẩu cũ bằng cách login
        await _authService.login(savedEmail, _oldPasswordController.text);

        // 3. Nếu login ok → tiến hành reset mật khẩu bằng JWT hiện tại
        final token = await _authService.getToken();
        if (token == null) throw Exception("Chưa đăng nhập");

        await _authService.resetPasswordWithToken(
          token,
          _newPasswordController.text,
        );

        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("Đổi mật khẩu thành công")),
        );
        Navigator.pop(context);
      } catch (e) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text("Lỗi đổi mật khẩu: $e")));
      } finally {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.backgroundSecondary,
      appBar: AppBar(
        title: const Text(
          "Thiết lập mật khẩu",
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
        ),
        centerTitle: true,
        backgroundColor: Colors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, size: 20),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  "Đổi mật khẩu mới",
                  style: TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.bold,
                    color: AppColors.textPrimary,
                  ),
                ),
                const SizedBox(height: 8),
                const Text(
                  "Hãy đảm bảo mật khẩu mới của bạn có tính bảo mật cao để bảo vệ tài khoản.",
                  style: TextStyle(
                    color: AppColors.textSecondary,
                    fontSize: 14,
                  ),
                ),
                const SizedBox(height: 32),

                // Card trắng
                Container(
                  padding: const EdgeInsets.all(20),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.03),
                        blurRadius: 15,
                        offset: const Offset(0, 5),
                      ),
                    ],
                  ),
                  child: Column(
                    children: [
                      _buildPasswordField(
                        "Mật khẩu hiện tại",
                        _oldPasswordController,
                        obscureText: _obscureOld,
                        toggleObscure: () =>
                            setState(() => _obscureOld = !_obscureOld),
                        validator: (val) {
                          if (val == null || val.isEmpty) {
                            return "Vui lòng nhập mật khẩu cũ";
                          }
                          return null;
                        },
                      ),
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: 10),
                        child: Divider(color: AppColors.borderPrimary),
                      ),
                      _buildPasswordField(
                        "Mật khẩu mới",
                        _newPasswordController,
                        obscureText: _obscureNew,
                        toggleObscure: () =>
                            setState(() => _obscureNew = !_obscureNew),
                        validator: _validatePassword,
                      ),
                      const SizedBox(height: 16),
                      _buildPasswordField(
                        "Xác nhận mật khẩu",
                        _confirmPasswordController,
                        obscureText: _obscureConfirm,
                        toggleObscure: () =>
                            setState(() => _obscureConfirm = !_obscureConfirm),
                        validator: (val) {
                          if (val == null || val.isEmpty) {
                            return "Vui lòng nhập lại mật khẩu mới";
                          }
                          if (val != _newPasswordController.text) {
                            return "Mật khẩu xác nhận không khớp";
                          }
                          return null;
                        },
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 40),

                // Nút bấm chính
                SizedBox(
                  width: double.infinity,
                  height: 54,
                  child: ElevatedButton(
                    onPressed: _isLoading ? null : _changePassword,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.brandColor, // Màu cam chủ đạo
                      foregroundColor: Colors.white,
                      elevation: 0,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14),
                      ),
                    ),
                    child: _isLoading
                        ? const SizedBox(
                            height: 24,
                            width: 24,
                            child: CircularProgressIndicator(
                              color: Colors.white,
                              strokeWidth: 2,
                            ),
                          )
                        : const Text(
                            "Xác nhận thay đổi",
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildPasswordField(
    String label,
    TextEditingController controller, {
    String? Function(String?)? validator,
    bool obscureText = true,
    VoidCallback? toggleObscure,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: 8),
        TextFormField(
          controller: controller,
          obscureText: obscureText,
          style: const TextStyle(fontSize: 15),
          decoration: InputDecoration(
            isDense: true,
            hintText: "••••••••",
            hintStyle: const TextStyle(color: AppColors.textThird),
            filled: true,
            fillColor: AppColors.greyLight.withOpacity(0.5),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide.none,
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(
                color: AppColors.borderPrimary,
                width: 0.5,
              ),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(
                color: AppColors.brandColor,
                width: 1.5,
              ),
            ),
            errorBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: AppColors.error, width: 0.5),
            ),
            prefixIcon: const Icon(Icons.lock_outline_rounded, size: 20),
            prefixIconColor: AppColors.textSecondary,
            suffixIcon: IconButton(
              icon: Icon(
                obscureText
                    ? Icons.visibility_outlined
                    : Icons.visibility_off_outlined,
                size: 20,
                color: AppColors.textThird,
              ),
              onPressed: toggleObscure,
            ),
          ),
          validator: validator,
        ),
      ],
    );
  }
}
