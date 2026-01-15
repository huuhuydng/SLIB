import 'package:flutter/material.dart';
import 'package:provider/provider.dart'; // Import Provider
import 'package:slib/assets/colors.dart';
import 'package:slib/models/user_profile.dart';
import 'package:slib/services/auth_service.dart';
import 'package:slib/views/authentication/on_boarding_screen.dart';
import 'package:slib/views/profile/booking_history_screen.dart';
import 'package:slib/views/profile/profile_info_screen.dart';
// import 'package:slib/views/home/widgets/profile_info_screen.dart' as screen;

class SettingScreen extends StatefulWidget {
  final UserProfile? user;
  const SettingScreen({super.key, this.user});

  @override
  State<SettingScreen> createState() => _SettingScreenState();
}

class _SettingScreenState extends State<SettingScreen> {
  // Không cần khai báo Service hay biến State cục bộ nữa!
  // Tất cả đã nằm trong AuthService.

  @override
  Widget build(BuildContext context) {
    // 1. LẮNG NGHE DỮ LIỆU TỪ PROVIDER (Tự động cập nhật khi Cache/API thay đổi)
    final authService = context.watch<AuthService>();
    final currentUser = authService.currentUser ?? widget.user; // Ưu tiên lấy từ Provider
    final settings = authService.currentSetting; 

    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      appBar: AppBar(
        title: const Text(
          "Tài khoản & Cài đặt",
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 18,
            color: Colors.black87,
          ),
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
            // 1. Profile Header
            _buildModernProfileHeader(currentUser),

            const SizedBox(height: 20),

            // 3. Nhóm Cấu hình Thư viện
            _buildSectionTitle("Cấu hình Thư viện"),
            const SizedBox(height: 10),

            // CHECK NULL: 
            // - Nếu settings đã có (từ Cache hoặc API): Hiện luôn.
            // - Nếu null (lần đầu cài app chưa có cache): Hiện Loading nhỏ.
            if (settings != null)
              _buildSettingsGroup([
                _buildSwitchTile(
                  icon: Icons.nfc_rounded,
                  iconColor: Colors.deepOrange,
                  title: "Check-in NFC (HCE)",
                  subtitle: "Chạm điện thoại để vào cửa",
                  value: settings.isHceEnabled, 
                  onChanged: (val) {
                    // GỌI HÀM UPDATE CỦA AUTH SERVICE
                    context.read<AuthService>().updateSetting(
                      settings.copyWith(isHceEnabled: val),
                    );
                  },
                ),
                _buildDivider(),
                _buildSwitchTile(
                  icon: Icons.auto_awesome_rounded,
                  iconColor: Colors.purple,
                  title: "Gợi ý thông minh (AI)",
                  subtitle: "Đề xuất chỗ ngồi phù hợp",
                  value: settings.isAiRecommendEnabled,
                  onChanged: (val) {
                    context.read<AuthService>().updateSetting(
                      settings.copyWith(isAiRecommendEnabled: val),
                    );
                  },
                ),
                _buildDivider(),
                _buildSwitchTile(
                  icon: Icons.notifications_none_rounded,
                  iconColor: Colors.blue,
                  title: "Thông báo lịch đặt",
                  subtitle: "Nhắc nhở trước 15 phút",
                  value: settings.isBookingRemindEnabled,
                  onChanged: (val) {
                    context.read<AuthService>().updateSetting(
                      settings.copyWith(isBookingRemindEnabled: val),
                    );
                  },
                ),
              ])
            else
              const Padding(
                padding: EdgeInsets.all(20),
                child: Center(
                  child: CircularProgressIndicator(color: AppColors.brandColor),
                ),
              ),

            const SizedBox(height: 30),

            // 4. Nhóm Quản lý (Giữ nguyên)
            _buildSectionTitle("Quản lý cá nhân"),
            const SizedBox(height: 10),
            _buildSettingsGroup([
              _buildNavTile(
                icon: Icons.person_outline_rounded,
                iconColor: Colors.blue,
                title: "Thông tin sinh viên",
                onTap: () {
                   if (currentUser != null) {
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (context) => ProfileInfoScreen(user: currentUser)),
                    );
                  }
                },
              ),
              _buildDivider(),
              _buildNavTile(
                icon: Icons.history_edu_rounded,
                iconColor: Colors.teal,
                title: "Lịch sử đặt chỗ",
                onTap: () {
                  Navigator.push(
                      context,
                      MaterialPageRoute(builder: (context) => const BookingHistoryScreen()),
                    );
                },
              ),
              _buildDivider(),
              _buildNavTile(
                icon: Icons.warning_amber_rounded,
                iconColor: Colors.redAccent,
                title: "Lịch sử vi phạm",
                trailingText: "0 vi phạm",
                onTap: () {
                  
                },
              ),
            ]),

            const SizedBox(height: 30),

            // 5. Nhóm Hỗ trợ
            _buildSectionTitle("Hỗ trợ"),
            const SizedBox(height: 10),
            _buildSettingsGroup([
              _buildNavTile(
                icon: Icons.help_outline_rounded,
                iconColor: Colors.green,
                title: "Hướng dẫn check-in",
                onTap: () {},
              ),
            ]),

            const SizedBox(height: 40),

            // 6. Nút Đăng xuất
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
            const SizedBox(height: 30),
          ],
        ),
      ),
    );
  }

  // --- CÁC WIDGET UI (GIỮ NGUYÊN STYLE) ---

  Widget _buildModernProfileHeader(UserProfile? user) {
    String firstLetter = (user?.fullName.isNotEmpty ?? false)
        ? user!.fullName[0].toUpperCase()
        : "S";

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Colors.white, Color(0xFFFFF3E0)],
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
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  user?.fullName ?? "Sinh viên FPT",
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 6),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.brandColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    "MSSV: ${user?.studentCode ?? '...'}",
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
          BoxShadow(
            color: Colors.black.withOpacity(0.03),
            blurRadius: 15,
            offset: const Offset(0, 5),
          ),
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
        style: const TextStyle(
          fontWeight: FontWeight.w600,
          fontSize: 15,
          color: Colors.black87,
        ),
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
                  style: const TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: 15,
                    color: Colors.black87,
                  ),
                ),
              ),
              if (trailingText != null)
                Text(
                  trailingText,
                  style: const TextStyle(
                    color: Colors.grey,
                    fontSize: 13,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              if (trailingText != null) const SizedBox(width: 8),
              Icon(
                Icons.arrow_forward_ios_rounded,
                size: 16,
                color: Colors.grey[300],
              ),
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
        title: const Text(
          "Đăng xuất?",
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        content: const Text(
          "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản không?",
        ),
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
                // SỬA LẠI: Dùng context.read để gọi hàm logout của Provider
                await context.read<AuthService>().logout();

                if (mounted) {
                  Navigator.of(context).pushAndRemoveUntil(
                    MaterialPageRoute(
                      builder: (context) => const OnBoardingScreen(),
                    ),
                    (route) => false,
                  );
                }
              } catch (e) {
                print("Lỗi logout: $e");
              }
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(10),
              ),
              elevation: 0,
            ),
            child: const Text(
              "Đăng xuất",
              style: TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
    );
  }
}