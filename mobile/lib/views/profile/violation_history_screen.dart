import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:slib/assets/colors.dart';

/// Model cho vi phạm
class Violation {
  final String id;
  final String type;
  final String title;
  final String description;
  final int pointsDeducted;
  final DateTime createdAt;
  final String? location;
  final String status;

  Violation({
    required this.id,
    required this.type,
    required this.title,
    required this.description,
    required this.pointsDeducted,
    required this.createdAt,
    this.location,
    required this.status,
  });
}

class ViolationHistoryScreen extends StatefulWidget {
  const ViolationHistoryScreen({super.key});

  @override
  State<ViolationHistoryScreen> createState() => _ViolationHistoryScreenState();
}

class _ViolationHistoryScreenState extends State<ViolationHistoryScreen> {
  bool _isLoading = false;
  List<Violation> _violations = [];
  int _totalViolations = 0;
  int _totalPointsLost = 0;

  @override
  void initState() {
    super.initState();
    _loadMockData();
  }

  /// Load mock data - sau này sẽ thay bằng API call
  void _loadMockData() {
    setState(() => _isLoading = true);

    // Simulate loading
    Future.delayed(const Duration(milliseconds: 500), () {
      final mockViolations = [
        Violation(
          id: '1',
          type: 'NO_SHOW',
          title: 'Không đến sau khi đặt chỗ',
          description: 'Đặt chỗ lúc 08:00 nhưng không check-in trong 30 phút',
          pointsDeducted: 10,
          createdAt: DateTime.now().subtract(const Duration(days: 2)),
          location: 'Khu A - Bàn 12',
          status: 'CONFIRMED',
        ),
        Violation(
          id: '2',
          type: 'LATE_RETURN',
          title: 'Trả sách trễ hạn',
          description: 'Trả sách trễ 5 ngày so với hạn mượn',
          pointsDeducted: 5,
          createdAt: DateTime.now().subtract(const Duration(days: 7)),
          location: 'Quầy thủ thư',
          status: 'CONFIRMED',
        ),
        Violation(
          id: '3',
          type: 'NOISE',
          title: 'Gây ồn trong thư viện',
          description: 'Nói chuyện quá lớn tiếng trong khu vực yên tĩnh',
          pointsDeducted: 15,
          createdAt: DateTime.now().subtract(const Duration(days: 14)),
          location: 'Khu B - Phòng đọc sách',
          status: 'APPEALED',
        ),
        Violation(
          id: '4',
          type: 'DAMAGE',
          title: 'Làm hư hại tài sản',
          description: 'Làm rách bìa sách khi mượn',
          pointsDeducted: 20,
          createdAt: DateTime.now().subtract(const Duration(days: 30)),
          location: 'Thư viện tầng 2',
          status: 'RESOLVED',
        ),
      ];

      setState(() {
        _violations = mockViolations;
        _totalViolations = mockViolations.length;
        _totalPointsLost = mockViolations.fold(0, (sum, v) => sum + v.pointsDeducted);
        _isLoading = false;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      appBar: AppBar(
        title: const Text(
          'Lịch sử vi phạm',
          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black87),
        ),
        backgroundColor: Colors.white,
        centerTitle: true,
        elevation: 0,
        scrolledUnderElevation: 0,
        iconTheme: const IconThemeData(color: Colors.black87),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: AppColors.brandColor))
          : RefreshIndicator(
              onRefresh: () async => _loadMockData(),
              child: CustomScrollView(
                slivers: [
                  // Stats Header
                  SliverToBoxAdapter(child: _buildStatsHeader()),

                  // Content
                  if (_violations.isEmpty)
                    SliverFillRemaining(child: _buildEmptyState())
                  else
                    SliverPadding(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      sliver: SliverList(
                        delegate: SliverChildBuilderDelegate(
                          (context, index) => _buildViolationCard(_violations[index]),
                          childCount: _violations.length,
                        ),
                      ),
                    ),

                  // Bottom padding
                  const SliverToBoxAdapter(child: SizedBox(height: 30)),
                ],
              ),
            ),
    );
  }

  /// Stats Header với tổng vi phạm và điểm bị trừ
  Widget _buildStatsHeader() {
    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            _violations.isEmpty ? Colors.green : Colors.red.shade400,
            _violations.isEmpty ? Colors.green.shade300 : Colors.red.shade300,
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: (_violations.isEmpty ? Colors.green : Colors.red).withOpacity(0.3),
            blurRadius: 15,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: _buildStatItem(
                  icon: Icons.warning_amber_rounded,
                  value: '$_totalViolations',
                  label: 'Tổng vi phạm',
                ),
              ),
              Container(
                width: 1,
                height: 50,
                color: Colors.white.withOpacity(0.3),
              ),
              Expanded(
                child: _buildStatItem(
                  icon: Icons.remove_circle_outline,
                  value: '-$_totalPointsLost',
                  label: 'Điểm bị trừ',
                ),
              ),
            ],
          ),
          if (_violations.isEmpty) ...[
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.2),
                borderRadius: BorderRadius.circular(20),
              ),
              child: const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.check_circle, color: Colors.white, size: 18),
                  SizedBox(width: 8),
                  Text(
                    'Bạn chưa có vi phạm nào!',
                    style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildStatItem({
    required IconData icon,
    required String value,
    required String label,
  }) {
    return Column(
      children: [
        Icon(icon, color: Colors.white, size: 28),
        const SizedBox(height: 8),
        Text(
          value,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 28,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          label,
          style: TextStyle(
            color: Colors.white.withOpacity(0.8),
            fontSize: 13,
          ),
        ),
      ],
    );
  }

  /// Empty state khi không có vi phạm
  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: Colors.green.withOpacity(0.1),
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.verified_user_outlined,
              size: 64,
              color: Colors.green,
            ),
          ),
          const SizedBox(height: 20),
          const Text(
            'Tuyệt vời!',
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.bold,
              color: Colors.black87,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Bạn chưa có vi phạm nào.\nHãy tiếp tục giữ gìn nề nếp nhé!',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey[600],
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }

  /// Card hiển thị từng vi phạm
  Widget _buildViolationCard(Violation violation) {
    final typeInfo = _getViolationTypeInfo(violation.type);

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border(
          left: BorderSide(color: typeInfo['color'] as Color, width: 4),
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.04),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: () => _showViolationDetail(violation),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header row
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Icon
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: (typeInfo['color'] as Color).withOpacity(0.1),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Icon(
                        typeInfo['icon'] as IconData,
                        color: typeInfo['color'] as Color,
                        size: 24,
                      ),
                    ),
                    const SizedBox(width: 12),
                    // Title and description
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            violation.title,
                            style: const TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 15,
                              color: Colors.black87,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            violation.description,
                            style: TextStyle(
                              color: Colors.grey[600],
                              fontSize: 13,
                            ),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ],
                      ),
                    ),
                    // Points badge
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                      decoration: BoxDecoration(
                        color: Colors.red.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(
                        '-${violation.pointsDeducted}',
                        style: const TextStyle(
                          color: Colors.red,
                          fontWeight: FontWeight.bold,
                          fontSize: 14,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                // Footer row
                Row(
                  children: [
                    // Location
                    if (violation.location != null) ...[
                      Icon(Icons.location_on_outlined, size: 14, color: Colors.grey[400]),
                      const SizedBox(width: 4),
                      Flexible(
                        child: Text(
                          violation.location!,
                          style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      const SizedBox(width: 8),
                    ],
                    const Spacer(),
                    // Status badge
                    _buildStatusBadge(violation.status),
                    const SizedBox(width: 8),
                    // Date
                    Icon(Icons.access_time, size: 14, color: Colors.grey[400]),
                    const SizedBox(width: 4),
                    Text(
                      _formatDateTime(violation.createdAt),
                      style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// Badge cho status vi phạm
  Widget _buildStatusBadge(String status) {
    Color color;
    String label;

    switch (status) {
      case 'CONFIRMED':
        color = Colors.red;
        label = 'Xác nhận';
        break;
      case 'APPEALED':
        color = Colors.orange;
        label = 'Đang khiếu nại';
        break;
      case 'RESOLVED':
        color = Colors.green;
        label = 'Đã xử lý';
        break;
      case 'PENDING':
        color = Colors.grey;
        label = 'Chờ xử lý';
        break;
      default:
        color = Colors.grey;
        label = status;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: color,
          fontSize: 11,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }

  /// Lấy thông tin icon và màu theo loại vi phạm
  Map<String, dynamic> _getViolationTypeInfo(String type) {
    switch (type) {
      case 'NO_SHOW':
        return {
          'icon': Icons.event_busy,
          'color': Colors.red,
          'label': 'Không đến',
        };
      case 'LATE_RETURN':
        return {
          'icon': Icons.schedule,
          'color': Colors.orange,
          'label': 'Trả trễ',
        };
      case 'NOISE':
        return {
          'icon': Icons.volume_up,
          'color': Colors.purple,
          'label': 'Gây ồn',
        };
      case 'DAMAGE':
        return {
          'icon': Icons.broken_image,
          'color': Colors.brown,
          'label': 'Hư hại',
        };
      case 'UNAUTHORIZED':
        return {
          'icon': Icons.block,
          'color': Colors.black,
          'label': 'Trái phép',
        };
      default:
        return {
          'icon': Icons.warning_amber,
          'color': Colors.grey,
          'label': 'Khác',
        };
    }
  }

  String _formatDateTime(DateTime dt) {
    final now = DateTime.now();
    final diff = now.difference(dt);

    if (diff.inDays == 0) {
      return 'Hôm nay';
    } else if (diff.inDays == 1) {
      return 'Hôm qua';
    } else if (diff.inDays < 7) {
      return '${diff.inDays} ngày trước';
    } else {
      return DateFormat('dd/MM/yyyy').format(dt);
    }
  }

  /// Hiển thị dialog chi tiết vi phạm
  void _showViolationDetail(Violation violation) {
    final typeInfo = _getViolationTypeInfo(violation.type);

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => Container(
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Handle bar
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.grey[300],
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 20),
            // Header
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: (typeInfo['color'] as Color).withOpacity(0.1),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Icon(
                    typeInfo['icon'] as IconData,
                    color: typeInfo['color'] as Color,
                    size: 28,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        violation.title,
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 4),
                      _buildStatusBadge(violation.status),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                  decoration: BoxDecoration(
                    color: Colors.red.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Column(
                    children: [
                      Text(
                        '-${violation.pointsDeducted}',
                        style: const TextStyle(
                          color: Colors.red,
                          fontWeight: FontWeight.bold,
                          fontSize: 20,
                        ),
                      ),
                      const Text(
                        'điểm',
                        style: TextStyle(color: Colors.red, fontSize: 12),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),
            // Details
            _buildDetailRow(Icons.description_outlined, 'Mô tả', violation.description),
            const SizedBox(height: 16),
            if (violation.location != null) ...[
              _buildDetailRow(Icons.location_on_outlined, 'Địa điểm', violation.location!),
              const SizedBox(height: 16),
            ],
            _buildDetailRow(
              Icons.calendar_today,
              'Thời gian',
              DateFormat('HH:mm - dd/MM/yyyy').format(violation.createdAt),
            ),
            const SizedBox(height: 24),
            // Appeal button (nếu chưa khiếu nại)
            if (violation.status == 'CONFIRMED')
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: () {
                    Navigator.pop(context);
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('Tính năng khiếu nại đang phát triển'),
                        backgroundColor: AppColors.brandColor,
                      ),
                    );
                  },
                  icon: const Icon(Icons.report_problem_outlined),
                  label: const Text('Khiếu nại vi phạm'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: Colors.orange,
                    side: const BorderSide(color: Colors.orange),
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                ),
              ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }

  Widget _buildDetailRow(IconData icon, String label, String value) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 20, color: Colors.grey[400]),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: TextStyle(
                  fontSize: 12,
                  color: Colors.grey[500],
                ),
              ),
              const SizedBox(height: 4),
              Text(
                value,
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  color: Colors.black87,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
