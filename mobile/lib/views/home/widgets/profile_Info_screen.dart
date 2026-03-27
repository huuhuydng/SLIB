import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/user_profile.dart';

class ProfileInfoScreen extends StatelessWidget {
  final UserProfile user;

  const ProfileInfoScreen({super.key, required this.user});

  // Hàm helper để hiển thị tên Role tiếng Việt đẹp
  String _getRoleDisplay(String role) {
    switch (role.toLowerCase()) {
      case 'student':
        return 'Sinh viên';
      case 'librarian':
        return 'Thủ thư';
      case 'admin':
        return 'Quản trị viên';
      default:
        return 'Khách';
    }
  }

  @override
  Widget build(BuildContext context) {
    // Lấy chữ cái đầu của tên để làm Avatar mặc định
    String firstLetter = user.fullName.isNotEmpty ? user.fullName[0].toUpperCase() : "S";

    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA), // Nền xám nhạt
      appBar: AppBar(
        title: const Text("Thông tin cá nhân", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
        centerTitle: true,
        backgroundColor: Colors.white,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, size: 20, color: Colors.black),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            // 1. Avatar (Dùng chữ cái đầu tên)
            Center(
              child: Container(
                padding: const EdgeInsets.all(4),
                decoration: BoxDecoration(
                  color: Colors.white,
                  shape: BoxShape.circle,
                  boxShadow: [
                    BoxShadow(color: Colors.black12, blurRadius: 10, offset: const Offset(0, 5))
                  ],
                ),
                child: CircleAvatar(
                  radius: 50,
                  backgroundColor: AppColors.brandColor.withOpacity(0.1),
                  backgroundImage: user.avtUrl != null && user.avtUrl!.isNotEmpty
                      ? NetworkImage(user.avtUrl!)
                      : null,
                  child: (user.avtUrl == null || user.avtUrl!.isEmpty)
                      ? Text(
                          firstLetter,
                          style: const TextStyle(
                            fontSize: 40,
                            fontWeight: FontWeight.bold,
                            color: AppColors.brandColor,
                          ),
                        )
                      : null,
                ),
              ),
            ),
            const SizedBox(height: 12),
            
            // Tên người dùng
            Text(
              user.fullName,
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.black87),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 4),
            Text(
              "FPT University Da Nang",
              style: TextStyle(fontSize: 14, color: Colors.grey[600]),
            ),

            const SizedBox(height: 30),

            // 2. Form thông tin (Read-only)
            _buildInfoCard([
              _buildTextField("Họ và tên", user.fullName, Icons.person_outline),
              const Divider(height: 24),
              _buildTextField("Mã sinh viên", user.studentCode, Icons.badge_outlined),
              const Divider(height: 24),
              _buildTextField(
                "Email nhà trường", 
                user.email ?? "Chưa cập nhật", // Handle null email
                Icons.email_outlined
              ),
              const Divider(height: 24),
              _buildTextField(
                "Vai trò", 
                _getRoleDisplay(user.role), // Gọi hàm hiển thị Role
                Icons.security_outlined
              ),
              const Divider(height: 24),
              _buildTextField(
                "Điểm uy tín", 
                "${user.reputationScore} điểm", 
                Icons.star_outline, 
                isHighlight: true
              ),
            ]),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoCard(List<Widget> children) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(color: Colors.black.withOpacity(0.03), blurRadius: 15, offset: const Offset(0, 5))
        ],
      ),
      child: Column(children: children),
    );
  }

  Widget _buildTextField(String label, String value, IconData icon, {bool isHighlight = false}) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            color: Colors.grey[100], 
            borderRadius: BorderRadius.circular(10)
          ),
          child: Icon(
            icon, 
            color: isHighlight ? AppColors.brandColor : Colors.grey[600], 
            size: 20
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: TextStyle(fontSize: 13, color: Colors.grey[500])),
              const SizedBox(height: 4),
              Text(
                value,
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: isHighlight ? AppColors.brandColor : Colors.black87,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}