import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/services/auth_service.dart';

class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen>
    with SingleTickerProviderStateMixin, WidgetsBindingObserver {
  late TabController _tabController;

  List<Map<String, dynamic>> _activities = [];
  List<Map<String, dynamic>> _pointTransactions = [];

  double _totalStudyHours = 0;
  int _totalVisits = 0;
  int _totalPointsEarned = 0;
  int _totalPointsLost = 0;

  bool _isLoading = true;
  String? _errorMessage;

  Timer? _refreshTimer;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    WidgetsBinding.instance.addObserver(this);
    _loadData();
    // Auto-refresh mỗi 10 giây
    _refreshTimer = Timer.periodic(const Duration(seconds: 10), (_) {
      _loadData(silent: true);
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    WidgetsBinding.instance.removeObserver(this);
    _refreshTimer?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _loadData(silent: true);
    }
  }

  Future<void> _loadData({bool silent = false}) async {
    final authService = Provider.of<AuthService>(context, listen: false);
    final user = authService.currentUser;

    if (user == null) {
      if (!silent && mounted) {
        setState(() {
          _errorMessage = "Vui lòng đăng nhập";
          _isLoading = false;
        });
      }
      return;
    }

    if (!silent && mounted) {
      setState(() {
        _isLoading = true;
      });
    }

    try {
      final url = Uri.parse("${ApiConstants.activityUrl}/history/${user.id}");
      final response = await http.get(url);

      if (response.statusCode == 200) {
        final data = jsonDecode(utf8.decode(response.bodyBytes));

        if (mounted) {
          setState(() {
            _activities =
                List<Map<String, dynamic>>.from(data['activities'] ?? []);
            _pointTransactions =
                List<Map<String, dynamic>>.from(data['pointTransactions'] ?? []);
            _totalStudyHours = (data['totalStudyHours'] ?? 0).toDouble();
            _totalVisits = (data['totalVisits'] ?? 0).toInt();
            _totalPointsEarned = (data['totalPointsEarned'] ?? 0).toInt();
            _totalPointsLost = (data['totalPointsLost'] ?? 0).toInt();
            _isLoading = false;
            _errorMessage = null;
          });
        }
      } else {
        throw Exception("Không thể tải lịch sử hoạt động");
      }
    } catch (e) {
      if (!silent && mounted) {
        setState(() {
          _errorMessage = e.toString();
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.backgroundPrimary,
      appBar: AppBar(
        title: const Text("Lịch sử hoạt động",
            style: TextStyle(fontWeight: FontWeight.bold)),
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
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _errorMessage != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.error_outline, size: 48, color: Colors.grey[400]),
                      const SizedBox(height: 12),
                      Text(
                        _errorMessage!,
                        style: const TextStyle(color: Colors.red),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: _loadData,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.brandColor,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                        child: const Text("Thử lại",
                            style: TextStyle(color: Colors.white)),
                      ),
                    ],
                  ),
                )
              : TabBarView(
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
    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _activities.length + 1, // +1 cho header thống kê
        itemBuilder: (context, index) {
          if (index == 0) return _buildSummaryCard();
          final item = _activities[index - 1];
          return _buildActivityItem(item);
        },
      ),
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
          BoxShadow(
              color: AppColors.brandColor.withAlpha(77),
              blurRadius: 10,
              offset: const Offset(0, 5))
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _buildStatColumn(
              "Tổng giờ học", "${_totalStudyHours}h", Icons.access_time_filled),
          Container(width: 1, height: 40, color: Colors.white30),
          _buildStatColumn(
              "Số lần đến", "$_totalVisits", Icons.school),
        ],
      ),
    );
  }

  Widget _buildStatColumn(String label, String value, IconData icon) {
    return Column(
      children: [
        Icon(icon, color: Colors.white70, size: 20),
        const SizedBox(height: 8),
        Text(value,
            style: const TextStyle(
                color: Colors.white,
                fontSize: 22,
                fontWeight: FontWeight.bold)),
        Text(label,
            style: const TextStyle(color: Colors.white70, fontSize: 12)),
      ],
    );
  }

  // Widget: Từng dòng hoạt động (dữ liệu từ API)
  Widget _buildActivityItem(Map<String, dynamic> item) {
    final type = item['activityType'] ?? '';
    final title = item['title'] ?? '';
    final description = item['description'] ?? '';
    final durationMinutes = item['durationMinutes'];
    // Cũng check title để match vi phạm nếu activityType không khớp
    final isViolationByTitle = title.toString().toLowerCase().contains('vi phạm') ||
                                title.toString().toLowerCase().contains('báo cáo vi phạm');
    final createdAt = _parseDateTime(item['createdAt']);

    IconData icon;
    Color color;

    switch (type) {
      case 'CHECK_IN':
        icon = Icons.login_rounded;
        color = AppColors.success;
        break;
      case 'CHECK_OUT':
        icon = Icons.logout_rounded;
        color = Colors.orange;
        break;
      case 'BOOKING_SUCCESS':
        icon = Icons.event_available;
        color = Colors.blue;
        break;
      case 'BOOKING_CANCEL':
        icon = Icons.event_busy;
        color = Colors.grey;
        break;
      case 'NFC_CONFIRM':
        icon = Icons.nfc;
        color = Colors.teal;
        break;
      case 'GATE_ENTRY':
        icon = Icons.sensor_door;
        color = Colors.purple;
        break;
      case 'NO_SHOW':
        icon = Icons.warning_amber;
        color = Colors.red;
        break;
      case 'VIOLATION':
        icon = Icons.error_rounded;
        color = Colors.red;
        break;
      default:
        if (isViolationByTitle) {
          icon = Icons.error_rounded;
          color = Colors.red;
        } else {
          icon = Icons.info;
          color = Colors.grey;
        }
    }

    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
              color: Colors.black.withAlpha(13), blurRadius: 5)
        ],
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
                color: color.withAlpha(26), shape: BoxShape.circle),
            child: Icon(icon, color: color, size: 20),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title,
                    style: const TextStyle(
                        fontWeight: FontWeight.bold, fontSize: 15)),
                const SizedBox(height: 4),
                Text(description,
                    style: const TextStyle(
                        color: AppColors.textGrey, fontSize: 13)),
                const SizedBox(height: 4),
                Row(
                  children: [
                    Icon(Icons.access_time,
                        size: 12, color: Colors.grey[400]),
                    const SizedBox(width: 4),
                    Text(_formatDateTime(createdAt),
                        style: TextStyle(
                            color: Colors.grey[400], fontSize: 12)),
                    if (durationMinutes != null) ...[
                      const SizedBox(width: 10),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                            color: Colors.green.shade50,
                            borderRadius: BorderRadius.circular(4)),
                        child: Text(
                            "Thời lượng: ${_formatDuration(durationMinutes)}",
                            style: TextStyle(
                                color: Colors.green.shade700,
                                fontSize: 10,
                                fontWeight: FontWeight.bold)),
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
    return RefreshIndicator(
      onRefresh: _loadData,
      child: _pointTransactions.isEmpty
          ? ListView(
              children: const [
                SizedBox(height: 120),
                Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.stars_outlined,
                          size: 60, color: Colors.grey),
                      SizedBox(height: 10),
                      Text("Chưa có biến động điểm",
                          style: TextStyle(color: Colors.grey)),
                    ],
                  ),
                ),
              ],
            )
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: _pointTransactions.length + 1, // +1 cho header thống kê
              itemBuilder: (context, index) {
                if (index == 0) return _buildPointsSummaryCard();
                final log = _pointTransactions[index - 1];
                return _buildPointItem(log);
              },
            ),
    );
  }

  // Widget: Card thống kê điểm
  Widget _buildPointsSummaryCard() {
    return Container(
      margin: const EdgeInsets.only(bottom: 20),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [Colors.deepPurple, Colors.purple.shade300],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.deepPurple.withAlpha(77),
            blurRadius: 10,
            offset: const Offset(0, 5),
          )
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _buildPointStatColumn(
            "Điểm nhận",
            "+$_totalPointsEarned",
            Icons.trending_up,
            Colors.greenAccent,
          ),
          Container(width: 1, height: 40, color: Colors.white30),
          _buildPointStatColumn(
            "Điểm trừ",
            "-$_totalPointsLost",
            Icons.trending_down,
            Colors.redAccent,
          ),
        ],
      ),
    );
  }

  Widget _buildPointStatColumn(
      String label, String value, IconData icon, Color iconColor) {
    return Column(
      children: [
        Icon(icon, color: iconColor, size: 20),
        const SizedBox(height: 8),
        Text(value,
            style: const TextStyle(
                color: Colors.white,
                fontSize: 22,
                fontWeight: FontWeight.bold)),
        Text(label,
            style: const TextStyle(color: Colors.white70, fontSize: 12)),
      ],
    );
  }

  // Widget: Từng dòng biến động điểm
  Widget _buildPointItem(Map<String, dynamic> log) {
    final points = log['points'] ?? 0;
    final title = log['title'] ?? '';
    final description = log['description'] ?? '';
    final transactionType = log['transactionType'] ?? '';
    final createdAt = _parseDateTime(log['createdAt']);
    bool isNegative = points < 0;

    // Xác định icon và color dựa trên transactionType
    IconData icon;
    Color iconColor;
    if (title.toString().startsWith('Vi phạm') || transactionType == 'VIOLATION_PENALTY') {
      icon = Icons.error_rounded;
      iconColor = Colors.red;
    } else if (transactionType == 'NO_SHOW_PENALTY' || title.toString().contains('No-show')) {
      icon = Icons.event_busy_rounded;
      iconColor = Colors.red.shade700;
    } else if (transactionType == 'CHECK_OUT_LATE_PENALTY') {
      icon = Icons.timer_off_rounded;
      iconColor = Colors.orange.shade800;
    } else if (isNegative) {
      icon = Icons.remove_circle_outline;
      iconColor = AppColors.error;
    } else {
      icon = Icons.star_rounded;
      iconColor = AppColors.success;
    }

    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border(
            left: BorderSide(
                color: isNegative ? AppColors.error : AppColors.success,
                width: 4)),
        boxShadow: [
          BoxShadow(
              color: Colors.black.withAlpha(13), blurRadius: 5)
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          children: [
            // Icon phân biệt loại
            Container(
              padding: const EdgeInsets.all(8),
              margin: const EdgeInsets.only(right: 12),
              decoration: BoxDecoration(
                color: iconColor.withAlpha(26),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, color: iconColor, size: 20),
            ),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title,
                      style: const TextStyle(
                          fontWeight: FontWeight.bold, fontSize: 16)),
                  const SizedBox(height: 4),
                  Text(description,
                      style: const TextStyle(
                          color: AppColors.textGrey, fontSize: 13),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis),
                  const SizedBox(height: 8),
                  Text(_formatDateTime(createdAt),
                      style:
                          TextStyle(color: Colors.grey[400], fontSize: 12)),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Column(
              children: [
                Text(
                  "${isNegative ? '' : '+'}$points",
                  style: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: isNegative ? AppColors.error : AppColors.success),
                ),
                const Text("điểm",
                    style: TextStyle(fontSize: 12, color: Colors.grey)),
              ],
            )
          ],
        ),
      ),
    );
  }

  // --- HELPER FUNCTIONS ---

  /// Parse datetime từ backend Java
  /// Backend dùng spring.jackson.time-zone=Asia/Ho_Chi_Minh
  /// → time đã ở timezone Việt Nam, KHÔNG cần .toLocal()
  DateTime _parseDateTime(dynamic raw) {
    if (raw == null) return DateTime.now();
    String str = raw.toString();
    // Loại bỏ timezone name suffix [Asia/Ho_Chi_Minh] nếu có
    final bracketIndex = str.indexOf('[');
    if (bracketIndex != -1) {
      str = str.substring(0, bracketIndex);
    }
    // Loại bỏ offset (+07:00) để Dart parse thành local time trực tiếp
    // vì backend đã convert sang Asia/Ho_Chi_Minh rồi
    final offsetRegex = RegExp(r'[+-]\d{2}:\d{2}$');
    str = str.replaceAll(offsetRegex, '');
    final parsed = DateTime.tryParse(str);
    return parsed ?? DateTime.now();
  }

  String _formatDateTime(DateTime dt) {
    final now = DateTime.now();
    final diff = now.difference(dt);

    if (diff.inDays == 0) {
      return "${DateFormat('HH:mm').format(dt)} - Hôm nay";
    } else if (diff.inDays == 1) {
      return "${DateFormat('HH:mm').format(dt)} - Hôm qua";
    } else {
      return DateFormat('dd/MM/yyyy - HH:mm').format(dt);
    }
  }

  String _formatDuration(int minutes) {
    final hours = minutes ~/ 60;
    final mins = minutes % 60;
    if (hours > 0) {
      return "$hours giờ ${mins.toString().padLeft(2, '0')} phút";
    }
    return "$mins phút";
  }
}