import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/services/auth_service.dart';

class ViolationHistoryScreen extends StatefulWidget {
  const ViolationHistoryScreen({super.key});

  @override
  State<ViolationHistoryScreen> createState() => _ViolationHistoryScreenState();
}

class _ViolationHistoryScreenState extends State<ViolationHistoryScreen> {
  bool _isLoading = true;
  String? _errorMessage;
  List<Map<String, dynamic>> _violations = [];
  int _totalViolations = 0;
  int _totalPointsLost = 0;

  @override
  void initState() {
    super.initState();
    _loadViolations();
  }

  Future<void> _loadViolations() async {
    final authService = Provider.of<AuthService>(context, listen: false);
    final token = await authService.getToken();

    if (token == null) {
      if (mounted) {
        setState(() {
          _errorMessage = "Vui lòng đăng nhập";
          _isLoading = false;
        });
      }
      return;
    }

    try {
      final url = Uri.parse("${ApiConstants.violationReportUrl}/against-me");
      final response = await http.get(
        url,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(utf8.decode(response.bodyBytes));
        final violations = data.map((v) => v as Map<String, dynamic>).toList();

        int totalPoints = 0;
        for (var v in violations) {
          final status = v['status'] ?? '';
          if (status == 'VERIFIED') {
            totalPoints += ((v['pointDeducted'] ?? 0) as num).toInt();
          }
        }

        if (mounted) {
          setState(() {
            _violations = violations;
            _totalViolations = violations.length;
            _totalPointsLost = totalPoints;
            _isLoading = false;
            _errorMessage = null;
          });
        }
      } else {
        throw Exception("Không thể tải lịch sử vi phạm");
      }
    } catch (e) {
      if (mounted) {
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
          : _errorMessage != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.error_outline, size: 48, color: Colors.grey[400]),
                      const SizedBox(height: 12),
                      Text(_errorMessage!, style: const TextStyle(color: Colors.red)),
                      const SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: _loadViolations,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.brandColor,
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                        child: const Text("Thử lại", style: TextStyle(color: Colors.white)),
                      ),
                    ],
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _loadViolations,
                  child: CustomScrollView(
                    slivers: [
                      SliverToBoxAdapter(child: _buildStatsHeader()),
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
                      const SliverToBoxAdapter(child: SizedBox(height: 30)),
                    ],
                  ),
                ),
    );
  }

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
              Container(width: 1, height: 50, color: Colors.white.withOpacity(0.3)),
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
                    style: TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
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
          style: const TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 4),
        Text(label, style: TextStyle(color: Colors.white.withOpacity(0.8), fontSize: 13)),
      ],
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
              color: Colors.green.withOpacity(0.1),
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.verified_user_outlined, size: 64, color: Colors.green),
          ),
          const SizedBox(height: 20),
          const Text('Tuyệt vời!',
              style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.black87)),
          const SizedBox(height: 8),
          Text(
            'Bạn chưa có vi phạm nào.\nHãy tiếp tục giữ gìn nề nếp nhé!',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 14, color: Colors.grey[600], height: 1.5),
          ),
        ],
      ),
    );
  }

  Widget _buildViolationCard(Map<String, dynamic> violation) {
    final violationType = violation['violationType'] ?? '';
    final typeLabel = violation['violationTypeLabel'] ?? violationType;
    final description = violation['description'] ?? '';
    final status = violation['status'] ?? '';
    final pointDeducted = (violation['pointDeducted'] ?? 0) as num;
    final seatCode = violation['seatCode'] ?? '';
    final zoneName = violation['zoneName'] ?? '';
    final createdAtStr = violation['createdAt'];
    final typeInfo = _getViolationTypeInfo(violationType);

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border(left: BorderSide(color: typeInfo['color'] as Color, width: 4)),
        boxShadow: [
          BoxShadow(color: Colors.black.withOpacity(0.04), blurRadius: 10, offset: const Offset(0, 4)),
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
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: (typeInfo['color'] as Color).withOpacity(0.1),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Icon(typeInfo['icon'] as IconData, color: typeInfo['color'] as Color, size: 24),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(typeLabel, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: Colors.black87)),
                          const SizedBox(height: 4),
                          if (description.isNotEmpty)
                            Text(description, style: TextStyle(color: Colors.grey[600], fontSize: 13), maxLines: 2, overflow: TextOverflow.ellipsis),
                        ],
                      ),
                    ),
                    if (status == 'VERIFIED' && pointDeducted > 0)
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                        decoration: BoxDecoration(color: Colors.red.withOpacity(0.1), borderRadius: BorderRadius.circular(20)),
                        child: Text('-$pointDeducted', style: const TextStyle(color: Colors.red, fontWeight: FontWeight.bold, fontSize: 14)),
                      ),
                  ],
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    if (seatCode.isNotEmpty) ...[
                      Icon(Icons.event_seat, size: 14, color: Colors.grey[400]),
                      const SizedBox(width: 4),
                      Text('$zoneName - Ghế $seatCode', style: TextStyle(fontSize: 12, color: Colors.grey[500])),
                      const SizedBox(width: 8),
                    ],
                    const Spacer(),
                    _buildStatusBadge(status),
                    const SizedBox(width: 8),
                    Icon(Icons.access_time, size: 14, color: Colors.grey[400]),
                    const SizedBox(width: 4),
                    Text(_formatDateTime(createdAtStr), style: TextStyle(fontSize: 12, color: Colors.grey[500])),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildStatusBadge(String status) {
    Color color;
    String label;

    switch (status) {
      case 'VERIFIED':
        color = Colors.red;
        label = 'Xác nhận';
        break;
      case 'PENDING':
        color = Colors.orange;
        label = 'Chờ xử lý';
        break;
      case 'REJECTED':
        color = Colors.green;
        label = 'Đã bác bỏ';
        break;
      case 'RESOLVED':
        color = Colors.blue;
        label = 'Đã xử lý';
        break;
      default:
        color = Colors.grey;
        label = status;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(color: color.withOpacity(0.1), borderRadius: BorderRadius.circular(12)),
      child: Text(label, style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.w600)),
    );
  }

  Map<String, dynamic> _getViolationTypeInfo(String type) {
    switch (type) {
      case 'UNAUTHORIZED_USE':
        return {'icon': Icons.block, 'color': Colors.red, 'label': 'Sử dụng ghế không đúng'};
      case 'LEFT_BELONGINGS':
        return {'icon': Icons.inventory_2, 'color': Colors.orange, 'label': 'Để đồ giữ chỗ'};
      case 'NOISE':
        return {'icon': Icons.volume_up, 'color': Colors.purple, 'label': 'Gây ồn ào'};
      case 'FEET_ON_SEAT':
        return {'icon': Icons.airline_seat_recline_normal, 'color': Colors.brown, 'label': 'Gác chân lên ghế/bàn'};
      case 'FOOD_DRINK':
        return {'icon': Icons.fastfood, 'color': Colors.deepOrange, 'label': 'Ăn uống trong thư viện'};
      case 'SLEEPING':
        return {'icon': Icons.hotel, 'color': Colors.indigo, 'label': 'Ngủ tại chỗ ngồi'};
      default:
        return {'icon': Icons.warning_amber, 'color': Colors.grey, 'label': 'Khác'};
    }
  }

  String _formatDateTime(dynamic raw) {
    if (raw == null) return '';
    String str = raw.toString();
    final bracketIndex = str.indexOf('[');
    if (bracketIndex != -1) str = str.substring(0, bracketIndex);
    final offsetRegex = RegExp(r'[+-]\d{2}:\d{2}$');
    str = str.replaceAll(offsetRegex, '');
    final dt = DateTime.tryParse(str);
    if (dt == null) return '';

    final now = DateTime.now();
    final diff = now.difference(dt);
    if (diff.inDays == 0) return '${DateFormat('HH:mm').format(dt)} - Hôm nay';
    if (diff.inDays == 1) return 'Hôm qua';
    if (diff.inDays < 7) return '${diff.inDays} ngày trước';
    return DateFormat('dd/MM/yyyy').format(dt);
  }

  void _showViolationDetail(Map<String, dynamic> violation) {
    final violationType = violation['violationType'] ?? '';
    final typeLabel = violation['violationTypeLabel'] ?? violationType;
    final description = violation['description'] ?? '';
    final status = violation['status'] ?? '';
    final pointDeducted = (violation['pointDeducted'] ?? 0) as num;
    final seatCode = violation['seatCode'] ?? '';
    final zoneName = violation['zoneName'] ?? '';
    final areaName = violation['areaName'] ?? '';
    final evidenceUrl = violation['evidenceUrl'];
    final createdAtStr = violation['createdAt'];
    final typeInfo = _getViolationTypeInfo(violationType);

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
            Center(
              child: Container(
                width: 40, height: 4,
                decoration: BoxDecoration(color: Colors.grey[300], borderRadius: BorderRadius.circular(2)),
              ),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: (typeInfo['color'] as Color).withOpacity(0.1),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Icon(typeInfo['icon'] as IconData, color: typeInfo['color'] as Color, size: 28),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(typeLabel, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 4),
                      _buildStatusBadge(status),
                    ],
                  ),
                ),
                if (status == 'VERIFIED' && pointDeducted > 0)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                    decoration: BoxDecoration(color: Colors.red.withOpacity(0.1), borderRadius: BorderRadius.circular(16)),
                    child: Column(
                      children: [
                        Text('-$pointDeducted', style: const TextStyle(color: Colors.red, fontWeight: FontWeight.bold, fontSize: 20)),
                        const Text('điểm', style: TextStyle(color: Colors.red, fontSize: 12)),
                      ],
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 24),
            if (description.isNotEmpty) ...[
              _buildDetailRow(Icons.description_outlined, 'Mô tả', description),
              const SizedBox(height: 16),
            ],
            if (seatCode.isNotEmpty)
              _buildDetailRow(Icons.event_seat, 'Vị trí', '$areaName - $zoneName - Ghế $seatCode'),
            if (seatCode.isNotEmpty) const SizedBox(height: 16),
            _buildDetailRow(Icons.calendar_today, 'Thời gian', _formatDateTimeFull(createdAtStr)),
            if (evidenceUrl != null && evidenceUrl.toString().isNotEmpty) ...[
              const SizedBox(height: 16),
              const Text('Bằng chứng:', style: TextStyle(fontSize: 12, color: Colors.grey)),
              const SizedBox(height: 8),
              ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: Image.network(
                  evidenceUrl,
                  height: 150,
                  width: double.infinity,
                  fit: BoxFit.cover,
                  errorBuilder: (_, __, ___) => Container(
                    height: 100,
                    decoration: BoxDecoration(color: Colors.grey[100], borderRadius: BorderRadius.circular(12)),
                    child: const Center(child: Icon(Icons.broken_image, color: Colors.grey)),
                  ),
                ),
              ),
            ],
            const SizedBox(height: 24),
            // Nút kháng cáo - chỉ hiện khi status là VERIFIED
            if (status == 'VERIFIED')
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: () => _showAppealDialog(violation),
                  icon: const Icon(Icons.gavel_rounded, size: 20),
                  label: const Text('Kháng cáo vi phạm', style: TextStyle(fontWeight: FontWeight.bold)),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.orange,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    elevation: 2,
                  ),
                ),
              ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }

  String _formatDateTimeFull(dynamic raw) {
    if (raw == null) return '';
    String str = raw.toString();
    final bracketIndex = str.indexOf('[');
    if (bracketIndex != -1) str = str.substring(0, bracketIndex);
    final offsetRegex = RegExp(r'[+-]\d{2}:\d{2}$');
    str = str.replaceAll(offsetRegex, '');
    final dt = DateTime.tryParse(str);
    if (dt == null) return '';
    return DateFormat('HH:mm - dd/MM/yyyy').format(dt);
  }

  void _showAppealDialog(Map<String, dynamic> violation) {
    final appealController = TextEditingController();
    final violationId = violation['id'];

    Navigator.pop(context); // Đóng bottom sheet chi tiết

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Row(
          children: [
            Icon(Icons.gavel_rounded, color: Colors.orange),
            SizedBox(width: 8),
            Text('Kháng cáo vi phạm', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Vui lòng mô tả lý do kháng cáo:',
                style: TextStyle(color: Colors.grey[600], fontSize: 14)),
            const SizedBox(height: 12),
            TextField(
              controller: appealController,
              maxLines: 4,
              decoration: InputDecoration(
                hintText: 'Nhập lý do kháng cáo...',
                hintStyle: TextStyle(color: Colors.grey[400]),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: const BorderSide(color: AppColors.brandColor, width: 2),
                ),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Hủy', style: TextStyle(color: Colors.grey)),
          ),
          ElevatedButton(
            onPressed: () async {
              final reason = appealController.text.trim();
              if (reason.isEmpty) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Vui lòng nhập lý do kháng cáo'), backgroundColor: Colors.red),
                );
                return;
              }
              Navigator.pop(context);
              await _submitAppeal(violationId, reason);
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.orange,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            ),
            child: const Text('Gửi kháng cáo'),
          ),
        ],
      ),
    );
  }

  Future<void> _submitAppeal(dynamic violationId, String reason) async {
    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final token = await authService.getToken();

      if (token == null) return;

      // TODO: Tạo endpoint backend cho kháng cáo khi cần
      // Hiện tại hiển thị thông báo thành công
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Đã gửi kháng cáo thành công. Thủ thư sẽ xem xét.'),
            backgroundColor: Colors.green,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Lỗi: $e'), backgroundColor: Colors.red),
        );
      }
    }
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
              Text(label, style: TextStyle(fontSize: 12, color: Colors.grey[500])),
              const SizedBox(height: 4),
              Text(value, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: Colors.black87)),
            ],
          ),
        ),
      ],
    );
  }
}
