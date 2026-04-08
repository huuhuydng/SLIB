import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/services/auth/auth_service.dart';
import 'package:slib/views/widgets/error_display_widget.dart';

class ActivityHistoryScreen extends StatefulWidget {
  const ActivityHistoryScreen({super.key});

  @override
  State<ActivityHistoryScreen> createState() => _ActivityHistoryScreenState();
}

class _ActivityHistoryScreenState extends State<ActivityHistoryScreen>
    with WidgetsBindingObserver {
  List<Map<String, dynamic>> _activities = [];
  List<Map<String, dynamic>> _pointTransactions = [];

  double _totalStudyHours = 0;
  int _totalVisits = 0;

  bool _isLoading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadData();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // Refresh khi app trở lại foreground
    if (state == AppLifecycleState.resumed) {
      _loadData(silent: true);
    }
  }

  Future<void> _loadData({bool silent = false}) async {
    final authService = Provider.of<AuthService>(context, listen: false);
    final user = authService.currentUser;

    if (user == null) {
      if (!silent) {
        setState(() {
          _errorMessage = 'auth';
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
      final response = await authService.authenticatedRequest('GET', url);

      if (response.statusCode == 200) {
        final data = jsonDecode(utf8.decode(response.bodyBytes));

        if (mounted) {
          setState(() {
            _activities = List<Map<String, dynamic>>.from(
              data['activities'] ?? [],
            );
            _pointTransactions = List<Map<String, dynamic>>.from(
              data['pointTransactions'] ?? [],
            );
            _totalStudyHours = (data['totalStudyHours'] ?? 0).toDouble();
            _totalVisits = (data['totalVisits'] ?? 0).toInt();
            _isLoading = false;
            _errorMessage = null;
          });
        }
      } else if (response.statusCode == 401 || response.statusCode == 403) {
        if (!silent && mounted) {
          setState(() {
            _errorMessage = 'auth';
            _isLoading = false;
          });
        }
        return;
      } else {
        throw Exception('status ${response.statusCode}');
      }
    } catch (e) {
      if (!silent && mounted) {
        setState(() {
          _errorMessage = ErrorDisplayWidget.toVietnamese(e);
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        backgroundColor: const Color(0xFFF5F7FA),
        appBar: AppBar(
          title: const Text(
            "Lịch sử hoạt động",
            style: TextStyle(
              fontWeight: FontWeight.bold,
              color: Colors.black87,
            ),
          ),
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
              Tab(text: "Hoạt động"),
              Tab(text: "Biến động điểm"),
            ],
          ),
        ),
        body: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : _errorMessage != null
            ? _errorMessage == 'auth'
                  ? ErrorDisplayWidget.auth(onRetry: _loadData)
                  : ErrorDisplayWidget(
                      message: _errorMessage!,
                      onRetry: _loadData,
                    )
            : TabBarView(children: [_buildActivityTab(), _buildPointsTab()]),
      ),
    );
  }

  Widget _buildActivityTab() {
    return RefreshIndicator(
      onRefresh: _loadData,
      child: CustomScrollView(
        slivers: [
          // Stats header
          SliverToBoxAdapter(
            child: Container(
              margin: const EdgeInsets.all(16),
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    AppColors.brandColor,
                    AppColors.brandColor.withAlpha(200),
                  ],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(20),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: _buildStatItem(
                      Icons.access_time,
                      "${_totalStudyHours}h",
                      "Tổng giờ học",
                    ),
                  ),
                  Container(
                    width: 1,
                    height: 50,
                    color: Colors.white.withAlpha(100),
                  ),
                  Expanded(
                    child: _buildStatItem(
                      Icons.school_outlined,
                      "$_totalVisits",
                      "Số lần đến",
                    ),
                  ),
                ],
              ),
            ),
          ),

          // Activity list
          if (_activities.isEmpty)
            SliverFillRemaining(
              child: ErrorDisplayWidget.empty(message: 'Chưa có hoạt động nào'),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate(
                  (context, index) => _buildActivityCard(_activities[index]),
                  childCount: _activities.length,
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildStatItem(IconData icon, String value, String label) {
    return Column(
      children: [
        Icon(icon, color: Colors.white, size: 28),
        const SizedBox(height: 8),
        Text(
          value,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 24,
            fontWeight: FontWeight.bold,
          ),
        ),
        Text(
          label,
          style: TextStyle(color: Colors.white.withAlpha(200), fontSize: 12),
        ),
      ],
    );
  }

  Widget _buildActivityCard(Map<String, dynamic> activity) {
    final type = activity['activityType'] ?? '';
    final title = activity['title'] ?? '';
    final description = activity['description'] ?? '';
    // Parse datetime từ backend (UTC) và convert sang local time
    final createdAtRaw = DateTime.tryParse(activity['createdAt'] ?? '');
    final createdAt = createdAtRaw?.toLocal() ?? DateTime.now();
    final durationMinutes = activity['durationMinutes'];

    IconData icon;
    Color color;

    switch (type) {
      case 'CHECK_IN':
        icon = Icons.login_rounded;
        color = Colors.green;
        break;
      case 'CHECK_OUT':
        icon = Icons.logout_rounded;
        color = Colors.blue;
        break;
      case 'BOOKING_SUCCESS':
        icon = Icons.event_available;
        color = AppColors.brandColor;
        break;
      case 'BOOKING_CANCEL':
        icon = Icons.event_busy;
        color = Colors.grey;
        break;
      case 'NFC_CONFIRM':
        icon = Icons.nfc;
        color = Colors.teal;
        break;
      case 'SEAT_CHECKOUT':
        icon = Icons.logout_rounded;
        color = Colors.orange;
        break;
      case 'GATE_ENTRY':
        icon = Icons.sensor_door;
        color = Colors.purple;
        break;
      case 'NO_SHOW':
        icon = Icons.warning_amber;
        color = Colors.red;
        break;
      default:
        icon = Icons.info;
        color = Colors.grey;
    }

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withAlpha(10),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: color.withAlpha(30),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(icon, color: color),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 15,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  description,
                  style: TextStyle(color: Colors.grey[600], fontSize: 13),
                ),
                const SizedBox(height: 6),
                Row(
                  children: [
                    Icon(Icons.access_time, size: 12, color: Colors.grey[400]),
                    const SizedBox(width: 4),
                    Text(
                      _formatDateTime(createdAt),
                      style: TextStyle(color: Colors.grey[400], fontSize: 11),
                    ),
                  ],
                ),
              ],
            ),
          ),
          if (durationMinutes != null)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              decoration: BoxDecoration(
                color: AppColors.brandColor.withAlpha(30),
                borderRadius: BorderRadius.circular(20),
              ),
              child: Text(
                _formatDuration(durationMinutes),
                style: TextStyle(
                  color: AppColors.brandColor,
                  fontSize: 11,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildPointsTab() {
    return RefreshIndicator(
      onRefresh: _loadData,
      child: _pointTransactions.isEmpty
          ? ErrorDisplayWidget.empty(message: 'Chưa có biến động điểm')
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: _pointTransactions.length,
              itemBuilder: (context, index) =>
                  _buildPointCard(_pointTransactions[index]),
            ),
    );
  }

  Widget _buildPointCard(Map<String, dynamic> transaction) {
    final points = transaction['points'] ?? 0;
    final title = transaction['title'] ?? '';
    final description = transaction['description'] ?? '';
    final createdAt =
        DateTime.tryParse(transaction['createdAt'] ?? '') ?? DateTime.now();

    final isPositive = points > 0;
    final color = isPositive ? Colors.green : Colors.red;
    final pointText = isPositive ? "+$points" : "$points";

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border(left: BorderSide(color: color, width: 4)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withAlpha(10),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 15,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  description,
                  style: TextStyle(color: Colors.grey[600], fontSize: 13),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 8),
                Text(
                  _formatDateTime(createdAt),
                  style: TextStyle(color: Colors.grey[400], fontSize: 11),
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Column(
            children: [
              Text(
                pointText,
                style: TextStyle(
                  color: color,
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              ),
              Text(
                "điểm",
                style: TextStyle(color: Colors.grey[500], fontSize: 12),
              ),
            ],
          ),
        ],
      ),
    );
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
