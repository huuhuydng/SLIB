import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../assets/colors.dart';
import '../../services/auth_service.dart';
import '../../services/support_request_service.dart';

class SupportRequestHistoryScreen extends StatefulWidget {
  const SupportRequestHistoryScreen({Key? key}) : super(key: key);

  @override
  State<SupportRequestHistoryScreen> createState() =>
      _SupportRequestHistoryScreenState();
}

class _SupportRequestHistoryScreenState
    extends State<SupportRequestHistoryScreen> {
  final SupportRequestService _service = SupportRequestService();
  List<Map<String, dynamic>> _requests = [];
  bool _isLoading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadRequests();
  }

  Future<void> _loadRequests() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final token = await authService.getToken();
      if (token == null) throw Exception('Chưa đăng nhập');

      final data = await _service.getMyRequests(token);
      setState(() {
        _requests = data.cast<Map<String, dynamic>>();
        // Sắp xếp theo thời gian mới nhất
        _requests.sort((a, b) {
          final aTime = a['createdAt'] ?? '';
          final bTime = b['createdAt'] ?? '';
          return bTime.toString().compareTo(aTime.toString());
        });
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = 'Không thể tải dữ liệu: ${e.toString()}';
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
      body: Column(
        children: [
          _buildHeader(),
          Expanded(
            child: _isLoading
                ? const Center(
                    child:
                        CircularProgressIndicator(color: AppColors.brandColor))
                : _errorMessage != null
                    ? _buildErrorState()
                    : _requests.isEmpty
                        ? _buildEmptyState()
                        : RefreshIndicator(
                            color: AppColors.brandColor,
                            onRefresh: _loadRequests,
                            child: ListView.builder(
                              padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                              itemCount: _requests.length,
                              itemBuilder: (context, index) =>
                                  _buildRequestCard(_requests[index]),
                            ),
                          ),
          ),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          colors: [Color(0xFFFFB87A), Color(0xFFFFF7F2), Color(0xFFF5F5F5)],
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
        ),
      ),
      child: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
          child: Row(
            children: [
              IconButton(
                icon: const Icon(Icons.arrow_back_ios,
                    color: Color(0xFF333333), size: 20),
                onPressed: () => Navigator.pop(context),
              ),
              const Expanded(
                child: Text(
                  'Lịch sử yêu cầu hỗ trợ',
                  style: TextStyle(
                    color: Color(0xFF1A1A1A),
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              IconButton(
                icon: const Icon(Icons.home_outlined,
                    color: Color(0xFF333333), size: 24),
                onPressed: () =>
                    Navigator.of(context).popUntil((route) => route.isFirst),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: AppColors.brandColor.withOpacity(0.1),
              shape: BoxShape.circle,
            ),
            child: Icon(Icons.support_agent_rounded,
                size: 56, color: AppColors.brandColor.withOpacity(0.5)),
          ),
          const SizedBox(height: 20),
          const Text(
            'Chưa có yêu cầu hỗ trợ nào',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Color(0xFF666666),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Các yêu cầu bạn gửi sẽ xuất hiện tại đây',
            style: TextStyle(fontSize: 14, color: Colors.grey[500]),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.error_outline, size: 48, color: Colors.red[300]),
          const SizedBox(height: 16),
          Text(
            _errorMessage ?? '',
            style: TextStyle(color: Colors.grey[600], fontSize: 14),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 16),
          ElevatedButton.icon(
            onPressed: _loadRequests,
            icon: const Icon(Icons.refresh, size: 18),
            label: const Text('Thử lại'),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.brandColor,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(20)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRequestCard(Map<String, dynamic> request) {
    final status = request['status'] as String? ?? 'PENDING';
    final statusInfo = _getStatusInfo(status);
    final description = request['description'] as String? ?? '';
    final createdAt = _parseDateTime(request['createdAt']);
    final imageUrls = (request['imageUrls'] as List<dynamic>?) ?? [];
    final adminResponse = request['adminResponse'] as String?;

    return GestureDetector(
      onTap: () => _showDetailBottomSheet(request),
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.04),
              blurRadius: 10,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header: status badge + thời gian
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                _buildStatusBadge(status, statusInfo),
                Text(
                  _formatDateTime(createdAt),
                  style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                ),
              ],
            ),
            const SizedBox(height: 12),

            // Mô tả
            Text(
              description,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontSize: 14,
                color: Color(0xFF333333),
                height: 1.5,
              ),
            ),

            // Ảnh đính kèm
            if (imageUrls.isNotEmpty) ...[
              const SizedBox(height: 10),
              Row(
                children: [
                  Icon(Icons.image_outlined, size: 14, color: Colors.grey[400]),
                  const SizedBox(width: 4),
                  Text(
                    '${imageUrls.length} ảnh đính kèm',
                    style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                  ),
                ],
              ),
            ],

            // Phản hồi từ thủ thư
            if (adminResponse != null && adminResponse.isNotEmpty) ...[
              const SizedBox(height: 10),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                decoration: BoxDecoration(
                  color: const Color(0xFFF0FFF4),
                  borderRadius: BorderRadius.circular(10),
                  border:
                      Border.all(color: Colors.green.withOpacity(0.2)),
                ),
                child: Row(
                  children: [
                    Icon(Icons.reply_rounded,
                        size: 16, color: Colors.green[600]),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        adminResponse,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 13,
                          color: Colors.green[700],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],

            // Chỉ dẫn bấm xem chi tiết
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                Text(
                  'Xem chi tiết',
                  style: TextStyle(
                    fontSize: 12,
                    color: AppColors.brandColor.withOpacity(0.7),
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const SizedBox(width: 2),
                Icon(Icons.arrow_forward_ios,
                    size: 10, color: AppColors.brandColor.withOpacity(0.7)),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatusBadge(
      String status, Map<String, dynamic> statusInfo) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: (statusInfo['color'] as Color).withOpacity(0.1),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 6,
            height: 6,
            decoration: BoxDecoration(
              color: statusInfo['color'] as Color,
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(width: 6),
          Text(
            statusInfo['label'] as String,
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: statusInfo['color'] as Color,
            ),
          ),
        ],
      ),
    );
  }

  void _showDetailBottomSheet(Map<String, dynamic> request) {
    final status = request['status'] as String? ?? 'PENDING';
    final statusInfo = _getStatusInfo(status);
    final description = request['description'] as String? ?? '';
    final createdAt = _parseDateTime(request['createdAt']);
    final updatedAt = _parseDateTime(request['updatedAt']);
    final resolvedAt = _parseDateTime(request['resolvedAt']);
    final imageUrls = (request['imageUrls'] as List<dynamic>?) ?? [];
    final adminResponse = request['adminResponse'] as String?;
    final resolvedByName = request['resolvedByName'] as String?;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => DraggableScrollableSheet(
        initialChildSize: 0.75,
        maxChildSize: 0.95,
        minChildSize: 0.5,
        builder: (_, scrollController) => Container(
          decoration: const BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
          ),
          child: Column(
            children: [
              // Drag handle
              Container(
                margin: const EdgeInsets.only(top: 12),
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.grey[300],
                  borderRadius: BorderRadius.circular(2),
                ),
              ),

              // Content
              Expanded(
                child: ListView(
                  controller: scrollController,
                  padding: const EdgeInsets.all(20),
                  children: [
                    // Header: Trạng thái
                    Row(
                      children: [
                        Icon(statusInfo['icon'] as IconData,
                            color: statusInfo['color'] as Color, size: 28),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                statusInfo['label'] as String,
                                style: TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.w700,
                                  color: statusInfo['color'] as Color,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                'Gửi lúc ${_formatDateTimeFull(createdAt)}',
                                style: TextStyle(
                                    fontSize: 12, color: Colors.grey[500]),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),

                    const SizedBox(height: 20),
                    _buildDivider(),
                    const SizedBox(height: 16),

                    // Mô tả
                    const Text(
                      'Nội dung yêu cầu',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: Color(0xFF666666),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: const Color(0xFFF9FAFB),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: const Color(0xFFE5E7EB)),
                      ),
                      child: Text(
                        description,
                        style: const TextStyle(
                          fontSize: 14,
                          color: Color(0xFF333333),
                          height: 1.6,
                        ),
                      ),
                    ),

                    // Ảnh đính kèm
                    if (imageUrls.isNotEmpty) ...[
                      const SizedBox(height: 20),
                      Text(
                        'Ảnh đính kèm (${imageUrls.length})',
                        style: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: Color(0xFF666666),
                        ),
                      ),
                      const SizedBox(height: 10),
                      SizedBox(
                        height: 100,
                        child: ListView.separated(
                          scrollDirection: Axis.horizontal,
                          itemCount: imageUrls.length,
                          separatorBuilder: (_, __) =>
                              const SizedBox(width: 10),
                          itemBuilder: (_, idx) =>
                              ClipRRect(
                            borderRadius: BorderRadius.circular(12),
                            child: Image.network(
                              imageUrls[idx].toString(),
                              width: 100,
                              height: 100,
                              fit: BoxFit.cover,
                              errorBuilder: (_, __, ___) => Container(
                                width: 100,
                                height: 100,
                                color: Colors.grey[200],
                                child: Icon(Icons.broken_image,
                                    color: Colors.grey[400]),
                              ),
                            ),
                          ),
                        ),
                      ),
                    ],

                    // Phản hồi từ thủ thư
                    if (adminResponse != null &&
                        adminResponse.isNotEmpty) ...[
                      const SizedBox(height: 20),
                      _buildDivider(),
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(8),
                            decoration: BoxDecoration(
                              color: Colors.green.withOpacity(0.1),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: Icon(Icons.support_agent_rounded,
                                color: Colors.green[600], size: 20),
                          ),
                          const SizedBox(width: 10),
                          const Text(
                            'Phản hồi từ thủ thư',
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                              color: Color(0xFF666666),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 10),
                      Container(
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF0FFF4),
                          borderRadius: BorderRadius.circular(12),
                          border:
                              Border.all(color: Colors.green.withOpacity(0.2)),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              adminResponse,
                              style: TextStyle(
                                fontSize: 14,
                                color: Colors.green[800],
                                height: 1.6,
                              ),
                            ),
                            if (resolvedByName != null) ...[
                              const SizedBox(height: 8),
                              Text(
                                '- $resolvedByName',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: Colors.green[600],
                                  fontStyle: FontStyle.italic,
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                    ],

                    // Thời gian chi tiết
                    const SizedBox(height: 20),
                    _buildDivider(),
                    const SizedBox(height: 16),
                    const Text(
                      'Thời gian',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: Color(0xFF666666),
                      ),
                    ),
                    const SizedBox(height: 10),
                    _buildTimelineItem(
                      icon: Icons.send_rounded,
                      color: AppColors.brandColor,
                      label: 'Gửi yêu cầu',
                      time: _formatDateTimeFull(createdAt),
                      isFirst: true,
                    ),
                    if (updatedAt != null && status != 'PENDING')
                      _buildTimelineItem(
                        icon: Icons.update_rounded,
                        color: Colors.blue,
                        label: 'Cập nhật',
                        time: _formatDateTimeFull(updatedAt),
                      ),
                    if (resolvedAt != null)
                      _buildTimelineItem(
                        icon: status == 'REJECTED'
                            ? Icons.cancel_rounded
                            : Icons.check_circle_rounded,
                        color: status == 'REJECTED' ? Colors.red : Colors.green,
                        label: status == 'REJECTED'
                            ? 'Đã từ chối'
                            : 'Đã giải quyết',
                        time: _formatDateTimeFull(resolvedAt),
                        isLast: true,
                      ),

                    const SizedBox(height: 30),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTimelineItem({
    required IconData icon,
    required Color color,
    required String label,
    required String time,
    bool isFirst = false,
    bool isLast = false,
  }) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Column(
          children: [
            Container(
              padding: const EdgeInsets.all(6),
              decoration: BoxDecoration(
                color: color.withOpacity(0.1),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, color: color, size: 16),
            ),
            if (!isLast)
              Container(
                width: 2,
                height: 24,
                color: Colors.grey[200],
              ),
          ],
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(top: 4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: const TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w500,
                    color: Color(0xFF333333),
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  time,
                  style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                ),
                if (!isLast) const SizedBox(height: 8),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildDivider() {
    return Divider(height: 1, color: Colors.grey[200]);
  }

  // -- Helpers --

  Map<String, dynamic> _getStatusInfo(String status) {
    switch (status) {
      case 'IN_PROGRESS':
        return {
          'label': 'Đang xử lý',
          'color': Colors.blue,
          'icon': Icons.hourglass_top_rounded,
        };
      case 'RESOLVED':
        return {
          'label': 'Đã giải quyết',
          'color': Colors.green,
          'icon': Icons.check_circle_rounded,
        };
      case 'REJECTED':
        return {
          'label': 'Đã từ chối',
          'color': Colors.red,
          'icon': Icons.cancel_rounded,
        };
      case 'PENDING':
      default:
        return {
          'label': 'Chờ xử lý',
          'color': AppColors.brandColor,
          'icon': Icons.schedule_rounded,
        };
    }
  }

  DateTime? _parseDateTime(dynamic value) {
    if (value == null) return null;
    try {
      return DateTime.parse(value.toString());
    } catch (_) {
      return null;
    }
  }

  String _formatDateTime(DateTime? dt) {
    if (dt == null) return '';
    final now = DateTime.now();
    final diff = now.difference(dt);

    if (diff.inMinutes < 1) return 'Vừa xong';
    if (diff.inMinutes < 60) return '${diff.inMinutes} phút trước';
    if (diff.inHours < 24) return '${diff.inHours} giờ trước';
    if (diff.inDays < 7) return '${diff.inDays} ngày trước';
    return DateFormat('dd/MM/yyyy').format(dt);
  }

  String _formatDateTimeFull(DateTime? dt) {
    if (dt == null) return '---';
    return DateFormat('HH:mm - dd/MM/yyyy').format(dt);
  }
}
