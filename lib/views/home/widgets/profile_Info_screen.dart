import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/services/auth_service.dart';

class ProfileInfoScreen extends StatefulWidget {
  final UserProfile user;
  const ProfileInfoScreen({super.key, required this.user});

  @override
  State<ProfileInfoScreen> createState() => _ProfileInfoScreenState();
}

class _ProfileInfoScreenState extends State<ProfileInfoScreen> {
  // Định nghĩa bảng màu theo nhận diện SLIB và FPT [cite: 11, 13]
  static const Color brandColor = Color(0xFFFF751F); // Màu cam chủ đạo
  static const Color textPrimary = Color(0xFF1A1A1A);
  static const Color textSecondary = Color(0xFF4A5568);
  static const Color bgSecondary = Color(0xFFFFF7F2);
  static const Color borderPrimary = Color(0xFFE2E8F0);

  final _formKey = GlobalKey<FormState>();
  late TextEditingController _fullNameController;
  late TextEditingController _emailController;
  late TextEditingController _studentCodeController;
  late TextEditingController _roleController;
  late UserProfile _currentUser;

  bool _isEditing = false;

  @override
  void initState() {
    super.initState();
    _currentUser = widget.user;
    _fullNameController = TextEditingController(text: _currentUser.fullName);
    _emailController = TextEditingController(text: _currentUser.email);
    _studentCodeController = TextEditingController(
      text: _currentUser.studentCode,
    );
    _roleController = TextEditingController(text: _currentUser.role);
  }

  @override
  void dispose() {
    _fullNameController.dispose();
    _emailController.dispose();
    _studentCodeController.dispose();
    _roleController.dispose();
    super.dispose();
  }

  Future<void> _saveChanges() async {
    if (_formKey.currentState!.validate()) {
      try {
        final updatedUser = UserProfile(
          id: _currentUser.id,
          fullName: _fullNameController.text,
          email: _emailController.text,
          studentCode: _studentCodeController.text,
          role: _roleController.text,
          reputationScore: _currentUser.reputationScore,
        );
        
        final auth = Provider.of<AuthService>(context, listen: false);
        await auth.updateProfile(updatedUser);

        setState(() {
          _isEditing = false;
          _currentUser = updatedUser;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("Cập nhật thông tin thành công")),
        );
      } catch (e) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text("Lỗi cập nhật: $e")));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text(
          'Thông tin cá nhân',
          style: TextStyle(color: textPrimary, fontWeight: FontWeight.bold),
        ),
        backgroundColor: Colors.white,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios, color: textPrimary, size: 20),
          onPressed: () => Navigator.pop(context),
        ),
        actions: [
          IconButton(
            icon: Icon(_isEditing ? Icons.save : Icons.edit, color: brandColor),
            onPressed: () {
              if (_isEditing) {
                _saveChanges();
              } else {
                setState(() => _isEditing = true);
              }
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            children: [
              // 1. THẺ SINH VIÊN ẢO (Virtual ID Card - Read only)
              _buildVirtualIDCard(),
              const SizedBox(height: 32),

              // 2. THẺ ĐIỂM UY TÍN (Reputation Score) [cite: 20, 49]
              _buildReputationCard(),
              const SizedBox(height: 24),

              // 3. DANH SÁCH THÔNG TIN CHI TIẾT
              Align(
                alignment: Alignment.centerLeft,
                child: Padding(
                  padding: const EdgeInsets.only(left: 8, bottom: 12),
                  child: Text(
                    "CHI TIẾT TÀI KHOẢN",
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      color: Colors.grey[600],
                      letterSpacing: 1.1,
                    ),
                  ),
                ),
              ),
              _buildEditableField(
                "Họ tên",
                _fullNameController,
                Icons.person_outline,
              ),
              const SizedBox(height: 12),
              _buildEditableField(
                "Email",
                _emailController,
                Icons.email_outlined,
              ),
              const SizedBox(height: 12),
              _buildEditableField(
                "MSSV",
                _studentCodeController,
                Icons.badge_outlined,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _roleController,
                enabled: false, // ❌ Không cho chỉnh sửa
                style: const TextStyle(
                  fontWeight: FontWeight.w500,
                  fontSize: 15,
                ),
                decoration: InputDecoration(
                  labelText: "Vai trò",
                  prefixIcon: const Icon(
                    Icons.admin_panel_settings_outlined,
                    color: brandColor,
                    size: 20,
                  ),
                  filled: true,
                  fillColor: Colors.grey[100], // nền xám nhạt để phân biệt
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 12,
                  ),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: borderPrimary),
                  ),
                  disabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide(
                      color: borderPrimary.withOpacity(0.4),
                    ),
                  ),
                ),
              ),

              const SizedBox(height: 30),
            ],
          ),
        ),
      ),
    );
  }

  // WIDGET THẺ SINH VIÊN
  Widget _buildVirtualIDCard() {
    return Container(
      width: double.infinity,
      height: 220,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Colors.white, brandColor.withOpacity(0.12), Colors.white],
        ),
        boxShadow: [
          BoxShadow(
            color: brandColor.withOpacity(0.15),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
        border: Border.all(color: brandColor.withOpacity(0.15), width: 1),
      ),
      child: Stack(
        children: [
          // Decor tròn mờ
          Positioned(
            right: -50,
            top: -50,
            child: Container(
              width: 180,
              height: 180,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: brandColor.withOpacity(0.08),
              ),
            ),
          ),
          Positioned(
            left: -40,
            bottom: -40,
            child: Container(
              width: 140,
              height: 140,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: brandColor.withOpacity(0.05),
              ),
            ),
          ),

          Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: const [
                    Text(
                      "FPT UNIVERSITY",
                      style: TextStyle(
                        color: textPrimary,
                        fontWeight: FontWeight.w700,
                        letterSpacing: 1.2,
                        fontSize: 13,
                      ),
                    ),
                    Icon(Icons.verified, color: brandColor, size: 22),
                  ],
                ),

                const Spacer(),

                // Body
                Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    // Avatar
                    Container(
                      width: 72,
                      height: 90,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(
                          color: brandColor.withOpacity(0.4),
                          width: 1.5,
                        ),
                        image: const DecorationImage(
                          image: NetworkImage("https://i.pravatar.cc/300"),
                          fit: BoxFit.cover,
                        ),
                      ),
                    ),
                    const SizedBox(width: 16),

                    // Info
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            "${_currentUser.fullName.toUpperCase()}",
                            style: TextStyle(
                              color: Color.fromARGB(255, 255, 145, 0),
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                              letterSpacing: 1,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            "${_currentUser.studentCode}",
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                              letterSpacing: 1,
                            ),
                          ),
                          const SizedBox(height: 10),

                          // Email
                          Text(
                            "${_currentUser.email}",
                            style: TextStyle(
                              fontFamily: 'Roboto',
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                              letterSpacing: 1,
                            ),
                          ),
                          const SizedBox(height: 10),
                        ],
                      ),
                    ),
                  ],
                ),

                const Spacer(),

                // Footer
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: const [
                    Text(
                      "STUDENT ID",
                      style: TextStyle(
                        fontSize: 11,
                        letterSpacing: 2,
                        color: textSecondary,
                      ),
                    ),
                    Text(
                      "Active",
                      style: TextStyle(
                        fontSize: 11,
                        color: Color.fromARGB(255, 46, 152, 4),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // Widget hiển thị điểm uy tín (Reputation Score) [cite: 20, 71]
  Widget _buildReputationCard() {
    Color scoreColor = _currentUser.reputationScore >= 80
        ? Colors.green
        : (_currentUser.reputationScore >= 50 ? brandColor : Colors.red);

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: bgSecondary,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: brandColor.withOpacity(0.1)),
      ),
      child: Row(
        children: [
          Stack(
            alignment: Alignment.center,
            children: [
              SizedBox(
                width: 55,
                height: 55,
                child: CircularProgressIndicator(
                  value: _currentUser.reputationScore / 100,
                  backgroundColor: Colors.white,
                  color: scoreColor,
                  strokeWidth: 5,
                ),
              ),
              Text(
                "${_currentUser.reputationScore}",
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: scoreColor,
                ),
              ),
            ],
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  "Điểm uy tín",
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                ),
                const SizedBox(height: 2),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // Widget cho các ô nhập liệu thông tin cá nhân
  Widget _buildEditableField(
    String label,
    TextEditingController controller,
    IconData icon,
  ) {
    return TextFormField(
      controller: controller,
      enabled: _isEditing,
      style: const TextStyle(fontWeight: FontWeight.w500, fontSize: 15),
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: Icon(icon, color: brandColor, size: 20),
        filled: true,
        fillColor: _isEditing ? Colors.white : Colors.grey[50],
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 16,
          vertical: 12,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: borderPrimary),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: borderPrimary),
        ),
        disabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: borderPrimary.withOpacity(0.4)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: brandColor, width: 1.5),
        ),
      ),
      validator: (val) =>
          val == null || val.isEmpty ? "Vui lòng nhập $label" : null,
    );
  }
}
