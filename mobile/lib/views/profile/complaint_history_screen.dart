import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/complaint.dart';
import 'package:slib/services/auth/auth_service.dart';
import 'package:slib/services/complaint/complaint_service.dart';
import 'package:slib/views/profile/widgets/history_list_controls.dart';
import 'package:slib/views/widgets/error_display_widget.dart';

class ComplaintHistoryScreen extends StatefulWidget {
  const ComplaintHistoryScreen({super.key});

  @override
  State<ComplaintHistoryScreen> createState() => _ComplaintHistoryScreenState();
}

class _ComplaintHistoryScreenState extends State<ComplaintHistoryScreen> {
  static const int _collapsedLimit = 10;

  final _service = ComplaintService();
  List<Complaint> _complaints = [];
  bool _isLoading = true;
  String? _error;
  HistoryTimeFilter _selectedFilter = HistoryTimeFilter.all;
  bool _expanded = false;

  @override
  void initState() {
    super.initState();
    _loadComplaints();
  }

  Future<void> _loadComplaints() async {
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

      final data = await _service.getMyComplaints(token);
      setState(() {
        _complaints = data.map((json) => Complaint.fromJson(json)).toList();
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = ErrorDisplayWidget.toVietnamese(e);
        _isLoading = false;
      });
    }
  }

  Future<void> _pickFilter() async {
    final selected = await showHistoryFilterDialog(
      context,
      initialFilter: _selectedFilter,
    );
    if (selected == null || selected == _selectedFilter || !mounted) return;
    setState(() {
      _selectedFilter = selected;
      _expanded = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final visibleComplaints = _filteredComplaints;

    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      appBar: AppBar(
        title: const Text(
          'Lịch sử khiếu nại',
          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black87),
        ),
        backgroundColor: Colors.white,
        centerTitle: true,
        elevation: 0,
        scrolledUnderElevation: 0,
        iconTheme: const IconThemeData(color: Colors.black87),
        actions: [
          IconButton(
            icon: Icon(
              Icons.filter_list_rounded,
              color: _selectedFilter == HistoryTimeFilter.all
                  ? Colors.black87
                  : AppColors.brandColor,
            ),
            tooltip: 'Lọc theo thời gian',
            onPressed: _pickFilter,
          ),
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadComplaints,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: AppColors.brandColor),
            )
          : _error != null
          ? _buildErrorState()
          : RefreshIndicator(
              onRefresh: _loadComplaints,
              color: AppColors.brandColor,
              child: ListView.builder(
                padding: const EdgeInsets.only(bottom: 24),
                itemCount: visibleComplaints.length + 1,
                itemBuilder: (context, index) {
                  if (index == 0) {
                    return Column(
                      children: [
                        HistoryListControls(
                          isExpanded: _expanded,
                          onExpandedChanged: (expanded) {
                            setState(() {
                              _expanded = expanded;
                            });
                          },
                          totalCount: _totalAfterFilter,
                          visibleCount: visibleComplaints.length,
                        ),
                        if (visibleComplaints.isEmpty)
                          Padding(
                            padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
                            child: ErrorDisplayWidget.empty(
                              message: _complaints.isEmpty
                                  ? 'Chưa có khiếu nại nào'
                                  : 'Không có dữ liệu phù hợp trong khoảng thời gian đã chọn',
                            ),
                          ),
                      ],
                    );
                  }

                  return Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: _buildComplaintCard(visibleComplaints[index - 1]),
                  );
                },
              ),
            ),
    );
  }

  List<Complaint> get _filteredComplaints {
    final now = DateTime.now();
    final items = _complaints.where((complaint) {
      switch (_selectedFilter) {
        case HistoryTimeFilter.last7Days:
          return !complaint.createdAt.isBefore(
            now.subtract(const Duration(days: 7)),
          );
        case HistoryTimeFilter.last30Days:
          return !complaint.createdAt.isBefore(
            now.subtract(const Duration(days: 30)),
          );
        case HistoryTimeFilter.all:
          return true;
      }
    }).toList();

    if (_expanded || items.length <= _collapsedLimit) {
      return items;
    }
    return items.take(_collapsedLimit).toList();
  }

  int get _totalAfterFilter {
    final now = DateTime.now();
    return _complaints.where((complaint) {
      switch (_selectedFilter) {
        case HistoryTimeFilter.last7Days:
          return !complaint.createdAt.isBefore(
            now.subtract(const Duration(days: 7)),
          );
        case HistoryTimeFilter.last30Days:
          return !complaint.createdAt.isBefore(
            now.subtract(const Duration(days: 30)),
          );
        case HistoryTimeFilter.all:
          return true;
      }
    }).length;
  }

  Widget _buildErrorState() {
    if (_error == 'auth') {
      return ErrorDisplayWidget.auth(onRetry: _loadComplaints);
    }
    return ErrorDisplayWidget(
      message: _error ?? 'Không thể tải lịch sử khiếu nại',
      onRetry: _loadComplaints,
    );
  }

  Widget _buildComplaintCard(Complaint complaint) {
    final statusColor = _statusColor(complaint.status);

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
                  Icons.gavel_rounded,
                  color: Color(0xFFEF6C00),
                  size: 22,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      complaint.subject,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFF1A1A1A),
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      complaint.targetLabel,
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
                  complaint.statusLabel,
                  style: TextStyle(
                    color: statusColor,
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            complaint.content,
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey[700],
              height: 1.45,
            ),
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _buildMetaChip(
                icon: Icons.schedule_outlined,
                label: 'Gửi lúc ${_formatDate(complaint.createdAt)}',
              ),
              if (complaint.resolvedAt != null)
                _buildMetaChip(
                  icon: Icons.verified_outlined,
                  label: 'Xử lý ${_formatDate(complaint.resolvedAt!)}',
                ),
              if (complaint.resolvedByName != null &&
                  complaint.resolvedByName!.isNotEmpty)
                _buildMetaChip(
                  icon: Icons.person_outline,
                  label: 'Bởi ${complaint.resolvedByName}',
                ),
            ],
          ),
          if (complaint.resolutionNote != null &&
              complaint.resolutionNote!.trim().isNotEmpty) ...[
            const SizedBox(height: 12),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: statusColor.withValues(alpha: 0.08),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: statusColor.withValues(alpha: 0.25)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Ghi chú xử lý',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                      color: statusColor,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    complaint.resolutionNote!,
                    style: const TextStyle(
                      fontSize: 13,
                      color: Color(0xFF374151),
                      height: 1.4,
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

  Widget _buildMetaChip({required IconData icon, required String label}) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: Colors.grey[600]),
          const SizedBox(width: 6),
          Text(label, style: TextStyle(fontSize: 12, color: Colors.grey[700])),
        ],
      ),
    );
  }

  Color _statusColor(String status) {
    switch (status) {
      case 'ACCEPTED':
        return const Color(0xFF2E7D32);
      case 'DENIED':
        return const Color(0xFFD32F2F);
      default:
        return const Color(0xFFFFA000);
    }
  }

  String _formatDate(DateTime date) {
    return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year} ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';
  }
}
