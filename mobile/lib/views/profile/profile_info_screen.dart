import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart'; // Thư viện QR
import 'package:url_launcher/url_launcher.dart'; // Thư viện mở Web
import 'package:slib/assets/colors.dart';
import 'package:slib/models/user_profile.dart';

class ProfileInfoScreen extends StatelessWidget {
  final UserProfile user;

  const ProfileInfoScreen({super.key, required this.user});

  // Hàm mở trang FAP
  Future<void> _openFAP() async {
    final Uri url = Uri.parse('https://fap.fpt.edu.vn');
    if (!await launchUrl(url, mode: LaunchMode.externalApplication)) {
      // Xử lý nếu không mở được (hiếm khi xảy ra)
      debugPrint('Không thể mở FAP');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      appBar: AppBar(
        title: const Text(
          "Hồ sơ sinh viên",
          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black87),
        ),
        backgroundColor: Colors.white,
        centerTitle: true,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.black87),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            // 1. THẺ SINH VIÊN ĐIỆN TỬ & QR CODE THẬT
            _buildDigitalStudentCard(),

            const SizedBox(height: 25),

            // 2. THÔNG TIN CHI TIẾT (+ Link FAP)
            _buildSectionTitle("Thông tin học vấn"),
            const SizedBox(height: 10),
            _buildInfoGroup([
              _buildInfoRow(Icons.email_outlined, "Email", "${user.email}"),
              _buildDivider(),
              InkWell(
                onTap: _openFAP, // Gọi hàm mở web
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
                  child: Row(
                    children: [
                      const Icon(Icons.language, color: Colors.orange, size: 22),
                      const SizedBox(width: 16),
                      const Expanded(
                        child: Text(
                          "Kiểm tra trên FAP",
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.bold,
                            color: Colors.orange, // Màu nổi bật
                          ),
                        ),
                      ),
                      Icon(Icons.open_in_new, size: 16, color: Colors.orange.withOpacity(0.7)),
                    ],
                  ),
                ),
              ),
            ]),

            const SizedBox(height: 25),

            // 3. THỐNG KÊ
            _buildSectionTitle("Thống kê hoạt động"),
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(child: _buildStatCard(Icons.bookmark_added, Colors.blue, "32", "Lượt đặt chỗ")),
                const SizedBox(width: 15),
                Expanded(child: _buildStatCard(Icons.warning_amber, Colors.orange, "0", "Vi phạm")),
              ],
            ),
            const SizedBox(height: 15),
            Row(
              children: [
                Expanded(child: _buildStatCard(Icons.access_time, Colors.green, "124h", "Giờ học tập")),
                const SizedBox(width: 15),
                Expanded(child: _buildStatCard(Icons.star_border, Colors.purple, "${user.reputationScore}", "Điểm uy tín")),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDigitalStudentCard() {
    String firstLetter = user.fullName.isNotEmpty ? user.fullName[0].toUpperCase() : "S";

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [AppColors.brandColor, const Color(0xFFFF9052)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: AppColors.brandColor.withOpacity(0.4),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        children: [
          // Avatar & Name
          Container(
            padding: const EdgeInsets.all(4),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              border: Border.all(color: Colors.white.withOpacity(0.5), width: 2),
            ),
            child: CircleAvatar(
              radius: 40,
              backgroundColor: Colors.white,
              child: Text(
                firstLetter,
                style: TextStyle(
                  fontSize: 32,
                  fontWeight: FontWeight.bold,
                  color: AppColors.brandColor,
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),
          Text(
            user.fullName.toUpperCase(),
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: Colors.white,
              letterSpacing: 1,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 4),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.2),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Text(
              user.studentCode,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w600,
                letterSpacing: 1.5,
              ),
            ),
          ),
          const SizedBox(height: 24),

          // --- QR CODE THẬT ---
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Column(
              children: [
                // Widget tạo QR Code
                QrImageView(
                  data: user.studentCode, // Dữ liệu là MSSV
                  version: QrVersions.auto,
                  size: 140.0,
                  backgroundColor: Colors.white,
                  // Có thể thêm logo FPT vào giữa QR nếu muốn (tùy chọn)
                  // embeddedImage: const AssetImage('assets/images/fpt_logo.png'),
                ),
                const SizedBox(height: 8),
                Text(
                  "Mã QR sinh viên",
                  style: TextStyle(fontSize: 10, color: Colors.grey[600]),
                ),
              ],
            ),
          )
        ],
      ),
    );
  }

  // ... (Giữ nguyên các widget _buildSectionTitle, _buildInfoGroup, _buildDivider, _buildInfoRow, _buildStatCard cũ)
  
  Widget _buildSectionTitle(String title) {
    return SizedBox(
      width: double.infinity,
      child: Text(title, style: TextStyle(color: Colors.grey[600], fontSize: 14, fontWeight: FontWeight.w600)),
    );
  }

  Widget _buildInfoGroup(List<Widget> children) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.03), blurRadius: 15, offset: const Offset(0, 5))],
      ),
      // Cần Clip để hiệu ứng InkWell không bị tràn ra ngoài bo góc
      clipBehavior: Clip.hardEdge, 
      child: Column(children: children),
    );
  }

  Widget _buildInfoRow(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      child: Row(
        children: [
          Icon(icon, color: Colors.grey[400], size: 22),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: TextStyle(fontSize: 12, color: Colors.grey[500])),
                const SizedBox(height: 2),
                Text(value, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w500, color: Colors.black87)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDivider() {
    return Divider(height: 1, thickness: 1, indent: 58, endIndent: 0, color: Colors.grey[100]);
  }

  Widget _buildStatCard(IconData icon, Color color, String value, String label) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.03), blurRadius: 10, offset: const Offset(0, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(color: color.withOpacity(0.1), shape: BoxShape.circle),
            child: Icon(icon, color: color, size: 20),
          ),
          const SizedBox(height: 12),
          Text(value, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.black87)),
          const SizedBox(height: 4),
          Text(label, style: TextStyle(fontSize: 12, color: Colors.grey[500])),
        ],
      ),
    );
  }
}