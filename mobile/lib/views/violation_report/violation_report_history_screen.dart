import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../services/auth/auth_service.dart';
import '../../services/report/violation_report_service.dart';
import '../../models/violation_report.dart';
import '../../views/widgets/error_display_widget.dart';

class ViolationReportHistoryScreen extends StatefulWidget {
  const ViolationReportHistoryScreen({super.key});

  @override
  State<ViolationReportHistoryScreen> createState() =>
      _ViolationReportHistoryScreenState();
}

class _ViolationReportHistoryScreenState
    extends State<ViolationReportHistoryScreen> {
  final _violationReportService = ViolationReportService();
  List<ViolationReport> _reports = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadReports();
  }

  Future<void> _loadReports() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final authService = Provider.of<AuthService>(context, listen: false);
      final token = await authService.getToken();
      if (token == null) {
        setState(() {
          _error = 'auth';
          _isLoading = false;
        });
        return;
      }

      final data = await _violationReportService.getMyReports(token);
      setState(() {
        _reports = data.map((json) => ViolationReport.fromJson(json)).toList();
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = ErrorDisplayWidget.toVietnamese(e);
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
                    child: CircularProgressIndicator(color: Color(0xFFD32F2F)),
                  )
                : _error != null
                ? _buildErrorState()
                : _reports.isEmpty
                ? _buildEmptyState()
                : RefreshIndicator(
                    onRefresh: _loadReports,
                    color: const Color(0xFFD32F2F),
                    child: ListView.builder(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 8,
                      ),
                      itemCount: _reports.length,
                      itemBuilder: (context, index) =>
                          _buildReportCard(_reports[index]),
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
          colors: [Color(0xFFFF8A80), Color(0xFFFFF7F2), Color(0xFFF5F5F5)],
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
                icon: const Icon(
                  Icons.arrow_back_ios,
                  color: Color(0xFF333333),
                  size: 20,
                ),
                onPressed: () => Navigator.pop(context),
              ),
              const Expanded(
                child: Text(
                  'Lịch sử báo cáo vi phạm',
                  style: TextStyle(
                    color: Color(0xFF1A1A1A),
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              IconButton(
                icon: const Icon(
                  Icons.refresh,
                  color: Color(0xFF333333),
                  size: 24,
                ),
                onPressed: _loadReports,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
    return ErrorDisplayWidget.empty(message: 'Chưa có báo cáo nào');
  }

  Widget _buildErrorState() {
    if (_error == 'auth') {
      return ErrorDisplayWidget.auth(onRetry: _loadReports);
    }
    return ErrorDisplayWidget(
      message: _error ?? 'Không thể tải dữ liệu',
      onRetry: _loadReports,
    );
  }

  Widget _buildReportCard(ViolationReport report) {
    final statusColor = _getStatusColor(report.status);
    final statusText = report.statusLabel;

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 10,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header: Loại vi phạm + Trạng thái
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFEBEE),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(
                  Icons.report_outlined,
                  color: Color(0xFFD32F2F),
                  size: 24,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      report.violationTypeLabel,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: Color(0xFF1A1A1A),
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Ghế ${report.seatCode}',
                      style: TextStyle(fontSize: 13, color: Colors.grey[600]),
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 5,
                ),
                decoration: BoxDecoration(
                  color: statusColor.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  statusText,
                  style: TextStyle(
                    color: statusColor,
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),

          // Mô tả
          if (report.description != null && report.description!.isNotEmpty) ...[
            const SizedBox(height: 12),
            Text(
              report.description!,
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey[700],
                height: 1.4,
              ),
              maxLines: 3,
              overflow: TextOverflow.ellipsis,
            ),
          ],

          // Khu vực + Thời gian
          const SizedBox(height: 12),
          Row(
            children: [
              if (report.areaName != null || report.zoneName != null) ...[
                Icon(
                  Icons.location_on_outlined,
                  size: 14,
                  color: Colors.grey[400],
                ),
                const SizedBox(width: 4),
                Expanded(
                  child: Text(
                    '${report.areaName ?? ''} - ${report.zoneName ?? ''}',
                    style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
              Icon(Icons.access_time, size: 14, color: Colors.grey[400]),
              const SizedBox(width: 4),
              Text(
                _formatDate(report.createdAt),
                style: TextStyle(fontSize: 12, color: Colors.grey[500]),
              ),
            ],
          ),

          // Điểm phạt (nếu có)
          if (report.pointDeducted != null && report.pointDeducted! > 0) ...[
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              decoration: BoxDecoration(
                color: const Color(0xFFFFEBEE),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(
                    Icons.remove_circle_outline,
                    size: 14,
                    color: Color(0xFFD32F2F),
                  ),
                  const SizedBox(width: 4),
                  Text(
                    'Người vi phạm bị trừ ${report.pointDeducted} điểm',
                    style: const TextStyle(
                      fontSize: 12,
                      color: Color(0xFFD32F2F),
                      fontWeight: FontWeight.w500,
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

  Color _getStatusColor(String status) {
    switch (status) {
      case 'PENDING':
        return const Color(0xFFFFA000);
      case 'VERIFIED':
        return const Color(0xFF388E3C);
      case 'RESOLVED':
        return const Color(0xFF1976D2);
      case 'REJECTED':
        return const Color(0xFFD32F2F);
      default:
        return Colors.grey;
    }
  }

  String _formatDate(DateTime date) {
    return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year} ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';
  }
}
