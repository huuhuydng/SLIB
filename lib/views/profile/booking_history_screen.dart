import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';

class BookingHistoryScreen extends StatelessWidget {
  const BookingHistoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 3,
      child: Scaffold(
        backgroundColor: const Color(0xFFF5F7FA),
        appBar: AppBar(
          title: const Text("Lịch sử đặt chỗ", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black87)),
          backgroundColor: Colors.white,
          centerTitle: true,
          elevation: 0,
          iconTheme: const IconThemeData(color: Colors.black87),
          bottom: TabBar(
            labelColor: AppColors.brandColor,
            unselectedLabelColor: Colors.grey,
            indicatorColor: AppColors.brandColor,
            indicatorWeight: 3,
            labelStyle: const TextStyle(fontWeight: FontWeight.bold),
            tabs: const [
              Tab(text: "Sắp tới"),
              Tab(text: "Hoàn thành"),
              Tab(text: "Đã hủy/Phạt"), // Gộp hủy và phạt vào tab cuối
            ],
          ),
        ),
        body: TabBarView(
          children: [
            _buildHistoryList(statusGroup: "active"), // Tab 1: Sắp tới & Đang dùng
            _buildHistoryList(statusGroup: "completed"), // Tab 2: Xong
            _buildHistoryList(statusGroup: "cancelled"), // Tab 3: Hủy & Vắng
          ],
        ),
      ),
    );
  }

  Widget _buildHistoryList({required String statusGroup}) {
    // MOCK DATA: Cập nhật theo logic Smart Library (Tự động duyệt)
    final List<Map<String, dynamic>> bookings = [
      // --- TAB 1: SẮP TỚI & ĐANG DÙNG ---
      if (statusGroup == "active") ...[
        {
          "room": "Bàn tự học A-05",
          "date": "Hôm nay, 14/01",
          "time": "09:00 - 11:00",
          "seat": "A-05",
          "status": "ongoing" // Đang ngồi học (Đã check-in NFC)
        },
        {
          "room": "Phòng họp nhóm VIP",
          "date": "Ngày mai, 15/01",
          "time": "14:00 - 16:00",
          "seat": "P-VIP",
          "status": "confirmed" // Đã đặt thành công (Hệ thống tự duyệt)
        },
        {
          "room": "Khu máy tính",
          "date": "16/01/2026",
          "time": "08:00 - 10:00",
          "seat": "PC-10",
          "status": "confirmed"
        },
      ],
      
      // --- TAB 2: HOÀN THÀNH ---
      if (statusGroup == "completed") ...[
        {
          "room": "Khu máy tính",
          "date": "10/01/2026",
          "time": "08:00 - 10:00",
          "seat": "PC-02",
          "status": "completed" // Đã check-out hoặc hết giờ
        },
        {
          "room": "Bàn tự học B-12",
          "date": "08/01/2026",
          "time": "13:00 - 15:00",
          "seat": "B-12",
          "status": "completed"
        },
      ],

      // --- TAB 3: ĐÃ HỦY / VẮNG MẶT ---
      if (statusGroup == "cancelled") ...[
        {
          "room": "Phòng hội thảo",
          "date": "01/01/2026",
          "time": "09:00 - 11:00",
          "seat": "H-01",
          "status": "cancelled" // Sinh viên tự hủy trên app
        },
        {
          "room": "Bàn A-01",
          "date": "20/12/2025",
          "time": "09:00 - 11:00",
          "seat": "A-01",
          "status": "absent" // Đặt nhưng không đến (Hệ thống đánh dấu vi phạm)
        },
      ]
    ];

    if (bookings.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.history_toggle_off, size: 60, color: Colors.grey[300]),
            const SizedBox(height: 10),
            Text("Không có dữ liệu", style: TextStyle(color: Colors.grey[500])),
          ],
        ),
      );
    }

    return ListView.separated(
      padding: const EdgeInsets.all(20),
      itemCount: bookings.length,
      separatorBuilder: (context, index) => const SizedBox(height: 15),
      itemBuilder: (context, index) {
        return _buildBookingCard(bookings[index]);
      },
    );
  }

  Widget _buildBookingCard(Map<String, dynamic> booking) {
    Color statusColor;
    String statusText;
    IconData statusIcon;

    // LOGIC MÀU SẮC MỚI
    switch (booking['status']) {
      case 'confirmed': // Đã đặt (Auto)
        statusColor = AppColors.brandColor; // Màu cam
        statusText = "Sắp tới";
        statusIcon = Icons.event_available;
        break;
      case 'ongoing': // Đang dùng
        statusColor = Colors.green;
        statusText = "Đang sử dụng";
        statusIcon = Icons.timelapse;
        break;
      case 'cancelled': // Hủy
        statusColor = Colors.grey;
        statusText = "Đã hủy";
        statusIcon = Icons.cancel_presentation;
        break;
      case 'absent': // Vắng mặt
        statusColor = Colors.red;
        statusText = "Vắng mặt (Vi phạm)";
        statusIcon = Icons.warning_amber_rounded;
        break;
      default: // completed
        statusColor = Colors.blueGrey;
        statusText = "Hoàn thành";
        statusIcon = Icons.check_circle_outline;
    }

    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 4))
        ],
        // Viền màu trạng thái bên trái để dễ nhận diện
        border: Border(left: BorderSide(color: statusColor, width: 4)), 
      ),
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(booking['room'], style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 4),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(color: Colors.grey[100], borderRadius: BorderRadius.circular(6)),
                    child: Text("Ghế: ${booking['seat']}", style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: Colors.grey[700])),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: statusColor.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Row(
                  children: [
                    Icon(statusIcon, size: 14, color: statusColor),
                    const SizedBox(width: 4),
                    Text(statusText, style: TextStyle(color: statusColor, fontSize: 12, fontWeight: FontWeight.bold)),
                  ],
                ),
              )
            ],
          ),
          const SizedBox(height: 15),
          const Divider(height: 1),
          const SizedBox(height: 15),
          Row(
            children: [
              Expanded(
                child: Row(
                  children: [
                    Icon(Icons.calendar_today_outlined, size: 16, color: Colors.grey[400]),
                    const SizedBox(width: 8),
                    Text(booking['date'], style: TextStyle(color: Colors.grey[700], fontSize: 13)),
                  ],
                ),
              ),
              Expanded(
                child: Row(
                  children: [
                    Icon(Icons.access_time, size: 16, color: Colors.grey[400]),
                    const SizedBox(width: 8),
                    Text(booking['time'], style: TextStyle(color: Colors.grey[700], fontSize: 13, fontWeight: FontWeight.w500)),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}