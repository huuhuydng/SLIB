import 'package:flutter/material.dart';
import 'package:slib/assets/colors.dart';



class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.backgroundPrimary,
      appBar: AppBar(
        title: const Text("Lịch sử hoạt động", style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        centerTitle: true,
        elevation: 0,
        bottom: TabBar(
          controller: _tabController,
          labelColor: AppColors.brandColor,
          unselectedLabelColor: Colors.grey,
          indicatorColor: AppColors.brandColor,
          indicatorWeight: 3,
          tabs: const [
            Tab(text: "Hoạt động"),
            Tab(text: "Biến động điểm"),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _buildActivityTab(),
          _buildReputationTab(),
        ],
      ),
    );
  }

  // --- TAB 1: LỊCH SỬ HOẠT ĐỘNG (Activity Log) ---
  Widget _buildActivityTab() {
    // Mock Data
    final List<Map<String, dynamic>> activities = [
      {
        "type": "checkout",
        "title": "Check-out thành công",
        "time": "11:05 - Hôm nay",
        "detail": "Khu Yên Tĩnh - Ghế A15",
        "duration": "2 giờ 05 phút"
      },
      {
        "type": "checkin",
        "title": "Check-in vào cửa",
        "time": "09:00 - Hôm nay",
        "detail": "Khu Yên Tĩnh - Ghế A15",
        "duration": null
      },
      {
        "type": "booking",
        "title": "Đặt chỗ thành công",
        "time": "08:30 - Hôm nay",
        "detail": "Đã đặt ghế A15 (09:00 - 11:00)",
        "duration": null
      },
      {
        "type": "checkout",
        "title": "Check-out thành công",
        "time": "16:00 - Hôm qua",
        "detail": "Khu Thảo Luận - Ghế B02",
        "duration": "1 giờ 30 phút"
      },
    ];

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: activities.length + 1, // +1 cho header thống kê
      itemBuilder: (context, index) {
        if (index == 0) return _buildSummaryCard(); // Header thống kê
        
        final item = activities[index - 1];
        return _buildActivityItem(item);
      },
    );
  }

  // Widget: Card thống kê tổng quan (Header)
  Widget _buildSummaryCard() {
    return Container(
      margin: const EdgeInsets.only(bottom: 20),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [AppColors.brandColor, Colors.orange.shade300],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(color: AppColors.brandColor.withOpacity(0.3), blurRadius: 10, offset: const Offset(0, 5))
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _buildStatColumn("Tổng giờ học", "45.5h", Icons.access_time_filled),
          Container(width: 1, height: 40, color: Colors.white30),
          _buildStatColumn("Số lần đến", "12", Icons.school),
        ],
      ),
    );
  }

  Widget _buildStatColumn(String label, String value, IconData icon) {
    return Column(
      children: [
        Icon(icon, color: Colors.white70, size: 20),
        const SizedBox(height: 8),
        Text(value, style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold)),
        Text(label, style: const TextStyle(color: Colors.white70, fontSize: 12)),
      ],
    );
  }

  // Widget: Từng dòng hoạt động
  Widget _buildActivityItem(Map<String, dynamic> item) {
    IconData icon;
    Color color;
    
    // Logic chọn icon/màu dựa trên loại hoạt động
    switch (item['type']) {
      case 'checkin':
        icon = Icons.login_rounded;
        color = AppColors.success;
        break;
      case 'checkout':
        icon = Icons.logout_rounded;
        color = Colors.orange;
        break;
      case 'booking':
      default:
        icon = Icons.calendar_today_rounded;
        color = Colors.blue;
        break;
    }

    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 5)],
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(color: color.withOpacity(0.1), shape: BoxShape.circle),
            child: Icon(icon, color: color, size: 20),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(item['title'], style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                const SizedBox(height: 4),
                Text(item['detail'], style: const TextStyle(color: AppColors.textGrey, fontSize: 13)),
                const SizedBox(height: 4),
                Row(
                  children: [
                    Icon(Icons.access_time, size: 12, color: Colors.grey[400]),
                    const SizedBox(width: 4),
                    Text(item['time'], style: TextStyle(color: Colors.grey[400], fontSize: 12)),
                    if (item['duration'] != null) ...[
                      const SizedBox(width: 10),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(color: Colors.green.shade50, borderRadius: BorderRadius.circular(4)),
                        child: Text("Thời lượng: ${item['duration']}", style: TextStyle(color: Colors.green.shade700, fontSize: 10, fontWeight: FontWeight.bold)),
                      )
                    ]
                  ],
                )
              ],
            ),
          ),
        ],
      ),
    );
  }

  // --- TAB 2: LỊCH SỬ ĐIỂM UY TÍN (Reputation Log) ---
  Widget _buildReputationTab() {
    final List<Map<String, dynamic>> logs = [
      {
        "score": -10,
        "reason": "Không check-in (No-show)",
        "date": "05/12/2025 - 09:30",
        "detail": "Bạn đã đặt ghế A15 nhưng không đến check-in trong thời gian quy định."
      },
      {
        "score": 5,
        "reason": "Thưởng: Tuần học chăm chỉ",
        "date": "01/12/2025 - 08:00",
        "detail": "Hoàn thành 10 giờ học trong tuần."
      },
      {
        "score": -5,
        "reason": "Check-out trễ",
        "date": "28/11/2025 - 17:30",
        "detail": "Bạn rời khỏi thư viện nhưng quên check-out quá 30 phút."
      },
    ];

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: logs.length,
      itemBuilder: (context, index) {
        final log = logs[index];
        bool isNegative = log['score'] < 0;

        return Container(
          margin: const EdgeInsets.only(bottom: 16),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(12),
            border: Border(left: BorderSide(color: isNegative ? AppColors.error : AppColors.success, width: 4)),
            boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 5)],
          ),
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(log['reason'], style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                      const SizedBox(height: 4),
                      Text(log['detail'], style: const TextStyle(color: AppColors.textGrey, fontSize: 13)),
                      const SizedBox(height: 8),
                      Text(log['date'], style: TextStyle(color: Colors.grey[400], fontSize: 12)),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                Column(
                  children: [
                    Text(
                      "${isNegative ? '' : '+'}${log['score']}",
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: isNegative ? AppColors.error : AppColors.success
                      ),
                    ),
                    const Text("điểm", style: TextStyle(fontSize: 12, color: Colors.grey)),
                  ],
                )
              ],
            ),
          ),
        );
      },
    );
  }
}