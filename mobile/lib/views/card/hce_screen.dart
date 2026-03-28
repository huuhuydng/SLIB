import 'package:barcode_widget/barcode_widget.dart'; // Import thư viện Barcode
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/services/auth/auth_service.dart';

class HceCardScreen extends StatelessWidget {
  const HceCardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    // Lấy thông tin user từ Provider (Real-time)
    final authService = context.watch<AuthService>();
    final user = authService.currentUser;

    // Dữ liệu hiển thị (Nếu chưa login thì dùng data mẫu)
    final String studentName = user?.fullName.toUpperCase() ?? "";
    final String studentCode = user?.studentCode ?? "";

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        centerTitle: true,
        title: const Text(
          "Thẻ của tôi",
          style: TextStyle(
            color: Colors.black,
            fontWeight: FontWeight.bold,
            fontSize: 20,
          ),
        ),
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 20),
          child: Column(
            children: [
              // 1. THẺ SINH VIÊN (CARD CHÍNH)
              Container(
                width: double.infinity,
                height: 540, // Tăng nhẹ chiều cao để chứa barcode
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(30),
                  // Gradient Cam FPT
                  gradient: const LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Color(0xFFF39F59),
                      Color(0xFFF08132),
                      Color(0xFFE67E46),
                    ],
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.1),
                      blurRadius: 20,
                      offset: const Offset(0, 10),
                    ),
                  ],
                ),
                child: Stack(
                  children: [
                    Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 24,
                        vertical: 30,
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.center,
                        children: [
                          // Logo FPT
                          Image.asset(
                            "assets/images/fpt_logo.png",
                            width: 100,
                            height: 40,
                            fit: BoxFit.contain,
                            // Xử lý nếu ảnh lỗi hoặc chưa có
                            errorBuilder: (context, error, stackTrace) => 
                                const Text("FPT UNIVERSITY", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                          ),

                          const SizedBox(height: 10),

                          const Text(
                            "STUDENT ID CARD",
                            style: TextStyle(
                              color: Colors.white70,
                              fontSize: 12,
                              fontWeight: FontWeight.w500,
                              letterSpacing: 2,
                            ),
                          ),

                          const SizedBox(height: 30),

                          // B. AVATAR (TRÒN)
                          Container(
                            width: 140,
                            height: 140,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              border: Border.all(
                                color: Colors.white.withOpacity(0.3),
                                width: 1,
                              ),
                              color: Colors.white.withOpacity(0.2),
                            ),
                            child: Center(
                              child: CircleAvatar(
                                radius: 68,
                                backgroundColor: Colors.white,
                                backgroundImage: user?.avtUrl != null && user!.avtUrl!.isNotEmpty
                                    ? NetworkImage(user.avtUrl!)
                                    : null,
                                child: (user?.avtUrl == null || user!.avtUrl!.isEmpty)
                                    ? const Icon(
                                        Icons.person,
                                        size: 100,
                                        color: Colors.grey,
                                      )
                                    : null,
                              ),
                            ),
                          ),

                          const SizedBox(height: 20),

                          // C. TÊN SINH VIÊN
                          Text(
                            studentName,
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 22,
                              fontWeight: FontWeight.bold,
                            ),
                          ),

                          const SizedBox(height: 12),

                          // D. MSSV (BADGE)
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 20,
                              vertical: 6,
                            ),
                            decoration: BoxDecoration(
                              color: const Color(0xFFD35400).withOpacity(0.8),
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: Text(
                              studentCode,
                              style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                                fontSize: 14,
                              ),
                            ),
                          ),

                          const Spacer(),

                          // E. BARCODE THẬT (CODE 128)
                          // Barcode cần nền trắng để máy quét đọc được
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: BarcodeWidget(
                              barcode: Barcode.code128(), // Loại barcode phổ biến nhất cho thẻ
                              data: studentCode, // Dữ liệu là MSSV
                              height: 60,
                              width: double.infinity,
                              drawText: false, // Không cần vẽ text MSSV ở dưới vì đã có Badge ở trên
                              color: Colors.black,
                            ),
                          ),
                          const SizedBox(height: 10),
                        ],
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 30),

              // 2. NÚT CHỨC NĂNG
              TextButton.icon(
                onPressed: () {
                  _showQrBackup(context, studentCode);
                },
                icon: const Icon(Icons.qr_code_2, color: Colors.black54),
                label: const Text(
                  "Mở rộng mã Barcode",
                  style: TextStyle(
                    color: Colors.black54,
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),        
            ],
          ),
        ),
      ),
    );
  }

  // Dialog hiển thị mã dự phòng to hơn
  void _showQrBackup(BuildContext context, String studentCode) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return Container(
          padding: const EdgeInsets.all(30),
          height: 300,
          child: Column(
            children: [
              const Text(
                "Mã Barcode của bạn",
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 10),
              Text(
                "Đưa mã này vào máy quét tại cổng",
                style: TextStyle(fontSize: 14, color: Colors.grey[600]),
              ),
              const SizedBox(height: 30),
              
              // Barcode Lớn
              BarcodeWidget(
                barcode: Barcode.code128(),
                data: studentCode,
                height: 100,
                width: double.infinity,
                drawText: true, // Show text MSSV bên dưới cho dễ đối chiếu
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
            ],
          ),
        );
      },
    );
  }
}