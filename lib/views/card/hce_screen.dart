import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';

class HceCardScreen extends StatelessWidget {
  const HceCardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white, // Nền trắng theo thiết kế
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(
            Icons.arrow_back_ios_new,
            color: Colors.black,
            size: 20,
          ),
          onPressed: () => Navigator.pop(context),
        ),
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
                height: 520,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(30),
                  // Gradient Cam FPT
                  gradient: const LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Color(0xFFF39F59), // Cam nhạt hơn chút ở trên
                      Color(0xFFF08132), // Cam đậm dần
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
                    // Nội dung chi tiết thẻ
                    Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 24,
                        vertical: 30,
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.center,
                        children: [
                          Image.asset(
                            "assets/images/fpt_logo.png",
                            width: 100,
                            height: 40,
                            fit: BoxFit.contain,
                          ),

                          const SizedBox(height: 10),

                          const Text(
                            "FPT UNIVERSITY",
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 22,
                              fontWeight: FontWeight.bold,
                              letterSpacing: 1,
                            ),
                          ),
                          const SizedBox(height: 4),
                          const Text(
                            "STUDENT ID CARD",
                            style: TextStyle(
                              color: Colors.white70,
                              fontSize: 12,
                              fontWeight: FontWeight.w500,
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
                              color: Colors.white.withOpacity(
                                0.2,
                              ), // Nền mờ sau avatar
                            ),
                            child: const Center(
                              child: CircleAvatar(
                                radius: 68,
                                backgroundColor: Colors
                                    .white, // Nền trắng của avatar silhouette
                                child: Icon(
                                  Icons.person,
                                  size: 100,
                                  color: Colors.grey,
                                ), // Thay bằng Image.network
                              ),
                            ),
                          ),

                          const SizedBox(height: 20),

                          // C. TÊN SINH VIÊN
                          const Text(
                            "NGUYỄN HỮU HUY",
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 24,
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
                              color: const Color(
                                0xFFD35400,
                              ).withOpacity(0.8), // Cam đậm hơn nền
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: const Text(
                              "DE180295",
                              style: TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                                fontSize: 14,
                              ),
                            ),
                          ),

                          const SizedBox(height: 30),

                          // E. BARCODE GIẢ LẬP
                          Container(
                            height: 50,
                            width: double.infinity,
                            // Vẽ các vạch đen ngẫu nhiên để giả lập Barcode
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: List.generate(40, (index) {
                                return Container(
                                  width: index % 3 == 0
                                      ? 4
                                      : (index % 2 == 0 ? 2 : 6),
                                  height: 50,
                                  color: Colors.black87,
                                  margin: const EdgeInsets.symmetric(
                                    horizontal: 1.5,
                                  ),
                                );
                              }),
                            ),
                          ),

                          const Spacer(),
                        
                        ],
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 30),

              // 2. NÚT CHỨC NĂNG DƯỚI CÙNG
              TextButton.icon(
                onPressed: () {
                  _showQrBackup(context);
                },
                icon: const Icon(Icons.qr_code_2, color: Colors.black54),
                label: const Text(
                  "Mã Barcode dự phòng",
                  style: TextStyle(
                    color: Colors.black54,
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),

              // Thanh gạch ngang dưới đáy (giống iPhone home indicator)
              const SizedBox(height: 20),
              Container(
                width: 130,
                height: 5,
                decoration: BoxDecoration(
                  color: Colors.black,
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // Dialog hiển thị mã dự phòng (Giữ nguyên logic cũ)
  void _showQrBackup(BuildContext context) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return Container(
          padding: const EdgeInsets.all(30),
          height: 350,
          child: Column(
            children: [
              const Text(
                "Mã Barcode dự phòng",
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 20),
              // Barcode lớn
              Container(
                height: 80,
                width: double.infinity,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: List.generate(50, (index) {
                    return Container(
                      width: index % 3 == 0 ? 3 : (index % 2 == 0 ? 1 : 5),
                      color: Colors.black,
                      margin: const EdgeInsets.symmetric(horizontal: 1),
                    );
                  }),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
