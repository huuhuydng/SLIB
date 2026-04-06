import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/seat_status_report.dart';
import 'package:slib/models/violation_report.dart';
import 'package:slib/services/auth/auth_service.dart';
import 'package:slib/services/report/seat_status_report_service.dart';
import 'package:slib/services/report/violation_report_service.dart';
import 'package:slib/views/widgets/error_display_widget.dart';

class ReportHistoryScreen extends StatelessWidget {
  const ReportHistoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        backgroundColor: const Color(0xFFF5F7FA),
        appBar: AppBar(
          title: const Text(
            'Lịch sử báo cáo',
            style: TextStyle(
              fontWeight: FontWeight.bold,
              color: Colors.black87,
            ),
          ),
          backgroundColor: Colors.white,
          centerTitle: true,
          elevation: 0,
          scrolledUnderElevation: 0,
          iconTheme: const IconThemeData(color: Colors.black87),
          bottom: TabBar(
            labelColor: AppColors.brandColor,
            unselectedLabelColor: Colors.grey,
            indicatorColor: AppColors.brandColor,
            indicatorWeight: 3,
            labelStyle: const TextStyle(fontWeight: FontWeight.bold),
            tabs: const [
              Tab(text: 'Vi phạm'),
              Tab(text: 'Ghế ngồi'),
            ],
          ),
        ),
        body: const TabBarView(
          children: [
            _ViolationReportHistoryTab(),
            _SeatStatusReportHistoryTab(),
          ],
        ),
      ),
    );
  }
}

class _ViolationReportHistoryTab extends StatefulWidget {
  const _ViolationReportHistoryTab();

  @override
  State<_ViolationReportHistoryTab> createState() =>
      _ViolationReportHistoryTabState();
}

class _ViolationReportHistoryTabState
    extends State<_ViolationReportHistoryTab> {
  final _service = ViolationReportService();
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

      final data = await _service.getMyReports(token);
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
    if (_isLoading) {
      return const Center(
        child: CircularProgressIndicator(color: AppColors.brandColor),
      );
    }

    if (_error != null) {
      if (_error == 'auth') {
        return ErrorDisplayWidget.auth(onRetry: _loadReports);
      }
      return ErrorDisplayWidget(message: _error!, onRetry: _loadReports);
    }

    if (_reports.isEmpty) {
      return ErrorDisplayWidget.empty(message: 'Chưa có báo cáo vi phạm nào');
    }

    return RefreshIndicator(
      onRefresh: _loadReports,
      color: AppColors.brandColor,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _reports.length,
        itemBuilder: (context, index) => _buildReportCard(_reports[index]),
      ),
    );
  }

  Widget _buildReportCard(ViolationReport report) {
    final statusColor = _statusColor(report.status);

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
                  report.statusLabel,
                  style: TextStyle(
                    color: statusColor,
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
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
              ] else
                const Spacer(),
              Icon(Icons.access_time, size: 14, color: Colors.grey[400]),
              const SizedBox(width: 4),
              Text(
                _formatDate(report.createdAt),
                style: TextStyle(fontSize: 12, color: Colors.grey[500]),
              ),
            ],
          ),
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

  Color _statusColor(String status) {
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

class _SeatStatusReportHistoryTab extends StatefulWidget {
  const _SeatStatusReportHistoryTab();

  @override
  State<_SeatStatusReportHistoryTab> createState() =>
      _SeatStatusReportHistoryTabState();
}

class _SeatStatusReportHistoryTabState
    extends State<_SeatStatusReportHistoryTab> {
  final _service = SeatStatusReportService();
  List<SeatStatusReport> _reports = [];
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

      final data = await _service.getMyReports(token);
      setState(() {
        _reports = data.map((json) => SeatStatusReport.fromJson(json)).toList();
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
    if (_isLoading) {
      return const Center(
        child: CircularProgressIndicator(color: AppColors.brandColor),
      );
    }

    if (_error != null) {
      if (_error == 'auth') {
        return ErrorDisplayWidget.auth(onRetry: _loadReports);
      }
      return ErrorDisplayWidget(message: _error!, onRetry: _loadReports);
    }

    if (_reports.isEmpty) {
      return ErrorDisplayWidget.empty(
        message: 'Chưa có báo cáo tình trạng ghế nào',
      );
    }

    return RefreshIndicator(
      onRefresh: _loadReports,
      color: AppColors.brandColor,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _reports.length,
        itemBuilder: (context, index) => _buildReportCard(_reports[index]),
      ),
    );
  }

  Widget _buildReportCard(SeatStatusReport report) {
    final statusColor = _statusColor(report.status);

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
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFF3E0),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(
                  Icons.chair_alt_outlined,
                  color: Color(0xFFEF6C00),
                  size: 24,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      report.issueTypeLabel,
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
                  report.statusLabel,
                  style: TextStyle(
                    color: statusColor,
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
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
              ] else
                const Spacer(),
              Icon(Icons.access_time, size: 14, color: Colors.grey[400]),
              const SizedBox(width: 4),
              Text(
                _formatDate(report.createdAt),
                style: TextStyle(fontSize: 12, color: Colors.grey[500]),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Color _statusColor(String status) {
    switch (status) {
      case 'VERIFIED':
        return Colors.orange;
      case 'RESOLVED':
        return Colors.green;
      case 'REJECTED':
        return Colors.red;
      default:
        return Colors.blueGrey;
    }
  }

  String _formatDate(DateTime date) {
    return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year} ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';
  }
}
