import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/seat_status_report.dart';
import '../../services/auth/auth_service.dart';
import '../../services/report/seat_status_report_service.dart';
import '../../views/widgets/error_display_widget.dart';

class SeatStatusReportHistoryScreen extends StatefulWidget {
  const SeatStatusReportHistoryScreen({super.key});

  @override
  State<SeatStatusReportHistoryScreen> createState() =>
      _SeatStatusReportHistoryScreenState();
}

class _SeatStatusReportHistoryScreenState
    extends State<SeatStatusReportHistoryScreen> {
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Lịch sử báo cáo tình trạng ghế')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? _error == 'auth'
              ? ErrorDisplayWidget.auth(onRetry: _loadReports)
              : ErrorDisplayWidget(message: _error!, onRetry: _loadReports)
          : _reports.isEmpty
          ? ErrorDisplayWidget.empty(message: 'Chưa có báo cáo tình trạng ghế nào')
          : RefreshIndicator(
              onRefresh: _loadReports,
              child: ListView.builder(
                itemCount: _reports.length,
                itemBuilder: (context, index) {
                  final report = _reports[index];
                  return Card(
                    margin: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 8,
                    ),
                    child: ListTile(
                      title: Text(
                        'Ghế ${report.seatCode} - ${report.issueTypeLabel}',
                      ),
                      subtitle: Text(
                        report.description?.isNotEmpty == true
                            ? report.description!
                            : 'Không có mô tả',
                      ),
                      trailing: Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 6,
                        ),
                        decoration: BoxDecoration(
                          color: _statusColor(report.status).withOpacity(0.1),
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: Text(
                          report.statusLabel,
                          style: TextStyle(
                            color: _statusColor(report.status),
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
    );
  }
}
