import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';



class MenuScreen extends StatefulWidget {
  const MenuScreen({super.key});

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
      backgroundColor: AppColors.brandColor,
      appBar: AppBar(
        title: const Text("Cài đặt & Tài khoản", style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        centerTitle: true,
        surfaceTintColor: Colors.transparent,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            // 1. Profile Header (Thông tin sinh viên)
            _buildProfileHeader(),
            
            const SizedBox(height: 24),

            // 2. Nhóm Cài đặt Ứng dụng
            _buildSectionTitle("Ứng dụng"),
            _buildSettingsGroup([
              _buildSwitchTile(
                icon: Icons.fingerprint,
                title: "Đăng nhập sinh trắc học",
                subtitle: "Sử dụng FaceID/Vân tay để mở khóa HCE",
                value: _isBiometricEnabled,
                onChanged: (val) => setState(() => _isBiometricEnabled = val),
              ),
              _buildDivider(),
              _buildSwitchTile(
                icon: Icons.notifications_outlined,
                title: "Thông báo đẩy",
                subtitle: "Nhắc nhở lịch đặt chỗ & Check-in",
                value: _isNotificationEnabled,
                onChanged: (val) => setState(() => _isNotificationEnabled = val),
              ),
              _buildDivider(),
              _buildSwitchTile(
                icon: Icons.dark_mode_outlined,
                title: "Chế độ tối",
                subtitle: "Giao diện nền tối bảo vệ mắt",
                value: _isDarkMode,
                onChanged: (val) => setState(() => _isDarkMode = val),
              ),
            ]),

            const SizedBox(height: 24),

            // 3. Nhóm Tài khoản
            _buildSectionTitle("Tài khoản"),
            _buildSettingsGroup([
              _buildNavTile(
                icon: Icons.person_outline,
                title: "Thông tin cá nhân",
                onTap: () {},
              ),
              _buildDivider(),
              _buildNavTile(
                icon: Icons.lock_outline,
                title: "Đổi mật khẩu",
                onTap: () {},
              ),
              _buildDivider(),
              _buildNavTile(
                icon: Icons.history,
                title: "Lịch sử giao dịch",
                onTap: () {}, // Có thể link sang HistoryScreen
              ),
            ]),

            const SizedBox(height: 24),

            // 4. Nhóm Hỗ trợ
            _buildSectionTitle("Hỗ trợ"),
            _buildSettingsGroup([
              _buildNavTile(
                icon: Icons.help_outline,
                title: "Hướng dẫn sử dụng",
                onTap: () {},
              ),
              _buildDivider(),
              _buildNavTile(
                icon: Icons.info_outline,
                title: "Về SLIB Ecosystem",
                trailingText: "v1.0.0",
                onTap: () {},
              ),
            ]),

            const SizedBox(height: 30),

            // 5. Nút Đăng xuất
            SizedBox(
              width: double.infinity,
              child: TextButton(
                onPressed: () {
                  // Show logout confirmation
                  _showLogoutDialog();
                },
                style: TextButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  backgroundColor: Colors.white,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  side: BorderSide(color: Colors.grey.shade300),
                ),
                child: const Text(
                  "Đăng xuất",
                  style: TextStyle(
                    color: AppColors.error, 
                    fontWeight: FontWeight.bold, 
                    fontSize: 16
                  ),
                ),
              ),
            ),
            
            const SizedBox(height: 20),
            Text("Powered by FPT University", style: TextStyle(color: Colors.grey[400], fontSize: 12)),
            const SizedBox(height: 20),
          ],
        ),
      ),
    );
  }

  // --- WIDGETS CON (COMPONENTS) ---

  // 1. Profile Header
  Widget _buildProfileHeader() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 4))
        ],
      ),
      child: Row(
        children: [
          // Avatar
          Stack(
            children: [
              Container(
                padding: const EdgeInsets.all(3),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(color: AppColors.brandColor, width: 2),
                ),
                child: const CircleAvatar(
                  radius: 32,
                  backgroundImage: NetworkImage('https://i.pravatar.cc/300?img=11'),
                ),
              ),
              Positioned(
                bottom: 0,
                right: 0,
                child: Container(
                  padding: const EdgeInsets.all(4),
                  decoration: const BoxDecoration(color: AppColors.brandColor, shape: BoxShape.circle),
                  child: const Icon(Icons.edit, color: Colors.white, size: 12),
                ),
              )
            ],
          ),
          const SizedBox(width: 16),
          // Info
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  "Nguyễn Hữu Huy",
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.textPrimary),
                ),
                const SizedBox(height: 4),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: Colors.grey[100],
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: const Text("MSSV: DE180295", style: TextStyle(color: AppColors.textGrey, fontSize: 12, fontWeight: FontWeight.w600)),
                ),
              ],
            ),
          ),
          const Icon(Icons.arrow_forward_ios, size: 16, color: Colors.grey),
        ],
      ),
    );
  }

  // 2. Tiêu đề nhóm
  Widget _buildSectionTitle(String title) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.only(bottom: 8, left: 4),
      child: Text(
        title.toUpperCase(),
        style: const TextStyle(
          color: AppColors.textGrey,
          fontSize: 13,
          fontWeight: FontWeight.bold,
          letterSpacing: 1,
        ),
      ),
    );
  }

  // 3. Khung nhóm cài đặt
  Widget _buildSettingsGroup(List<Widget> children) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.02), blurRadius: 5)],
      ),
      child: Column(children: children),
    );
  }

  // 4. Dòng Switch (Bật/Tắt)
  Widget _buildSwitchTile({
    required IconData icon,
    required String title,
    required String subtitle,
    required bool value,
    required Function(bool) onChanged,
  }) {
    return SwitchListTile(
      value: value,
      onChanged: onChanged,
      activeThumbColor: AppColors.brandColor,
      tileColor: Colors.transparent,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      secondary: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(color: AppColors.brandColor.withOpacity(0.1), shape: BoxShape.circle),
        child: Icon(icon, color: AppColors.brandColor, size: 20),
      ),
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
      subtitle: Text(subtitle, style: TextStyle(fontSize: 12, color: Colors.grey[500])),
    );
  }

  // 5. Dòng Navigation (Chuyển trang)
  Widget _buildNavTile({
    required IconData icon,
    required String title,
    String? trailingText,
    required VoidCallback onTap,
  }) {
    return ListTile(
      onTap: onTap,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      leading: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(color: Colors.grey[100], shape: BoxShape.circle),
        child: Icon(icon, color: Colors.grey[700], size: 20),
      ),
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (trailingText != null) 
            Text(trailingText, style: const TextStyle(color: Colors.grey, fontSize: 13)),
          if (trailingText != null) const SizedBox(width: 8),
          const Icon(Icons.arrow_forward_ios, size: 14, color: Colors.grey),
        ],
      ),
    );
  }

  // 6. Đường kẻ phân cách
  Widget _buildDivider() {
    return const Divider(height: 1, thickness: 0.5, indent: 60, endIndent: 0, color: Color(0xFFEEEEEE));
  }

  // Dialog Đăng xuất
  void _showLogoutDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text("Đăng xuất?"),
        content: const Text("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản không?"),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text("Hủy", style: TextStyle(color: Colors.grey)),
          ),
          TextButton(
            onPressed: () {
              // Xử lý đăng xuất ở đây
              Navigator.pop(context);
            },
            child: const Text("Đăng xuất", style: TextStyle(color: AppColors.error, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }
}