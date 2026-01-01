import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/services/auth_service.dart';
import 'package:slib/views/authentication/on_boarding_screen.dart';
import 'package:slib/views/home/widgets/profile_info_screen.dart' as screen;

class MenuScreen extends StatefulWidget {
  final UserProfile? user;
  const MenuScreen({super.key, this.user});

  @override
  State<MenuScreen> createState() => _MenuScreenState();
}

class _MenuScreenState extends State<MenuScreen> {
  // Trạng thái các nút gạt (Switch)
  bool _isBiometricEnabled = true;
  bool _isNotificationEnabled = true;
  bool _isDarkMode = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA), 
      appBar: AppBar(
        title: const Text(
          "Cài đặt & Tài khoản",
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18, color: Colors.black87),
        ),
        backgroundColor: Colors.white,
        centerTitle: true,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
        child: Column(
          children: [
            // 1. Profile Header (Avatar chữ cái)
            _buildModernProfileHeader(),

            const SizedBox(height: 30),

            // 2. Nhóm Cài đặt Ứng dụng
            _buildSectionTitle("Ứng dụng"),
            const SizedBox(height: 10),
            _buildSettingsGroup([
              _buildSwitchTile(
                icon: Icons.fingerprint,
                iconColor: Colors.purple,
                title: "Đăng nhập sinh trắc học",
                subtitle: "Sử dụng FaceID/Vân tay",
                value: _isBiometricEnabled,
                onChanged: (val) => setState(() => _isBiometricEnabled = val),
              ),
              _buildDivider(),
              _buildSwitchTile(
                icon: Icons.notifications_none_rounded,
                iconColor: Colors.orange,
                title: "Thông báo đẩy",
                subtitle: "Nhắc nhở lịch & Check-in",
                value: _isNotificationEnabled,
                onChanged: (val) =>
                    setState(() => _isNotificationEnabled = val),
              ),
              _buildDivider(),
              _buildSwitchTile(
                icon: Icons.dark_mode_outlined,
                iconColor: Colors.blueGrey,
                title: "Chế độ tối",
                subtitle: "Giao diện nền tối",
                value: _isDarkMode,
                onChanged: (val) => setState(() => _isDarkMode = val),
              ),
            ]),

            const SizedBox(height: 30),

            // 3. Nhóm Tài khoản (Đã bỏ Đổi mật khẩu)
            _buildSectionTitle("Tài khoản"),
            const SizedBox(height: 10),
            _buildSettingsGroup([
              _buildNavTile(
                icon: Icons.person_outline_rounded,
                iconColor: Colors.blue,
                title: "Thông tin cá nhân",
                onTap: () {
                  if (widget.user != null) {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) =>
                            screen.ProfileInfoScreen(user: widget.user!),
                      ),
                    );
                  }
                },
              ),
              _buildDivider(),
              _buildNavTile(
                icon: Icons.history_rounded,
                iconColor: Colors.indigo,
                title: "Lịch sử giao dịch",
                onTap: () {
                   // TODO: Navigate to Transaction History
                }, 
              ),
            ]),

            const SizedBox(height: 30),

            // 4. Nhóm Hỗ trợ
            _buildSectionTitle("Hỗ trợ"),
            const SizedBox(height: 10),
            _buildSettingsGroup([
              _buildNavTile(
                icon: Icons.help_outline_rounded,
                iconColor: Colors.green,
                title: "Hướng dẫn sử dụng",
                onTap: () {},
              ),
              _buildDivider(),
              _buildNavTile(
                icon: Icons.info_outline_rounded,
                iconColor: Colors.grey,
                title: "Về SLIB Ecosystem",
                trailingText: "v1.0.0",
                onTap: () {},
              ),
            ]),

            const SizedBox(height: 40),

            // 5. Nút Đăng xuất
            SizedBox(
              width: double.infinity,
              child: TextButton(
                onPressed: _showLogoutDialog,
                style: TextButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  backgroundColor: const Color(0xFFFFEBEE), 
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                ),
                child: const Text(
                  "Đăng xuất",
                  style: TextStyle(
                    color: Colors.red,
                    fontWeight: FontWeight.bold,
                    fontSize: 16,
                  ),
                ),
              ),
            ),

            const SizedBox(height: 20),
            Center(
              child: Text(
                "Powered by FPT University",
                style: TextStyle(color: Colors.grey[400], fontSize: 12, fontWeight: FontWeight.w500),
              ),
            ),
            const SizedBox(height: 30),
          ],
        ),
      ),
    );
  }

  // --- WIDGETS CON ---

  Widget _buildModernProfileHeader() {
    // Lấy chữ cái đầu của tên
    String firstLetter = (widget.user?.fullName.isNotEmpty ?? false) 
        ? widget.user!.fullName[0].toUpperCase() 
        : "S";

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [Colors.white, const Color(0xFFFFF3E0)], 
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: Colors.orange.withOpacity(0.1),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
        border: Border.all(color: Colors.white, width: 2),
      ),
      child: Row(
        children: [
          // Avatar chữ cái
          Container(
            padding: const EdgeInsets.all(4),
            decoration: const BoxDecoration(
              color: Colors.white,
              shape: BoxShape.circle,
              boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 5)],
            ),
            child: CircleAvatar(
              radius: 30,
              backgroundColor: AppColors.brandColor.withOpacity(0.1),
              child: Text(
                firstLetter,
                style: const TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  color: AppColors.brandColor,
                ),
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
                  widget.user?.fullName ?? "Sinh viên FPT",
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 6),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppColors.brandColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    "MSSV: ${widget.user?.studentCode ?? '...'}",
                    style: const TextStyle(
                      color: AppColors.brandColor,
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.only(left: 8),
      child: Text(
        title,
        style: TextStyle(
          color: Colors.grey[600],
          fontSize: 14,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }

  Widget _buildSettingsGroup(List<Widget> children) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(color: Colors.black.withOpacity(0.03), blurRadius: 15, offset: const Offset(0, 5)),
        ],
      ),
      clipBehavior: Clip.hardEdge, 
      child: Column(children: children),
    );
  }

  Widget _buildSwitchTile({
    required IconData icon,
    required Color iconColor,
    required String title,
    required String subtitle,
    required bool value,
    required Function(bool) onChanged,
  }) {
    return SwitchListTile(
      value: value,
      onChanged: onChanged,
      activeColor: Colors.white,
      activeTrackColor: AppColors.brandColor,
      inactiveThumbColor: Colors.white,
      inactiveTrackColor: Colors.grey.shade200,
      tileColor: Colors.white,
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
      secondary: Container(
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: iconColor.withOpacity(0.1),
          shape: BoxShape.circle,
        ),
        child: Icon(icon, color: iconColor, size: 22),
      ),
      title: Text(
        title,
        style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15, color: Colors.black87),
      ),
      subtitle: Padding(
        padding: const EdgeInsets.only(top: 2),
        child: Text(
          subtitle,
          style: TextStyle(fontSize: 12, color: Colors.grey[500]),
        ),
      ),
    );
  }

  Widget _buildNavTile({
    required IconData icon,
    required Color iconColor,
    required String title,
    String? trailingText,
    required VoidCallback onTap,
  }) {
    return Material(
      color: Colors.white,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: iconColor.withOpacity(0.1),
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: iconColor, size: 22),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15, color: Colors.black87),
                ),
              ),
              if (trailingText != null)
                Text(
                  trailingText,
                  style: const TextStyle(color: Colors.grey, fontSize: 13, fontWeight: FontWeight.w500),
                ),
              if (trailingText != null) const SizedBox(width: 8),
              Icon(Icons.arrow_forward_ios_rounded, size: 16, color: Colors.grey[300]),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDivider() {
    return Divider(
      height: 1,
      thickness: 1,
      indent: 68,
      endIndent: 0,
      color: Colors.grey[100],
    );
  }

  void _showLogoutDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text("Đăng xuất?", style: TextStyle(fontWeight: FontWeight.bold)),
        content: const Text("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản không?"),
        actionsPadding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text("Hủy", style: TextStyle(color: Colors.grey)),
          ),
          ElevatedButton(
            onPressed: () async {
              Navigator.pop(context); // Đóng Dialog
              try {
                final authService = AuthService(); // Khởi tạo AuthService
                await authService.logout(); // Gọi hàm logout
                
                if (mounted) {
                  // Quay về màn hình OnBoarding
                  Navigator.of(context).pushAndRemoveUntil(
                    MaterialPageRoute(builder: (context) => const OnBoardingScreen()),
                    (route) => false,
                  );
                }
              } catch (e) {
                print("Lỗi logout: $e");
              }
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              elevation: 0,
            ),
            child: const Text("Đăng xuất", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }
}