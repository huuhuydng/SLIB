import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/core/utils/snackbar_guard.dart';
import 'package:slib/services/auth/auth_service.dart';
import 'package:slib/views/profile/widgets/history_list_controls.dart';
import 'package:slib/views/widgets/error_display_widget.dart';

class ViolationHistoryScreen extends StatefulWidget {
  const ViolationHistoryScreen({super.key});

  @override
  State<ViolationHistoryScreen> createState() => _ViolationHistoryScreenState();
}

class _ViolationHistoryScreenState extends State<ViolationHistoryScreen>
    with SingleTickerProviderStateMixin {
  static const int _collapsedLimit = 10;

  late TabController _tabController;

  bool _isLoading = true;
  String? _errorMessage;

  // Tab 1: Auto penalties from point_transactions
  List<Map<String, dynamic>> _penalties = [];

  // Tab 2: Reported violations
  List<Map<String, dynamic>> _violations = [];
  HistoryTimeFilter _selectedFilter = HistoryTimeFilter.all;
  final Map<int, bool> _expandedTabs = {0: false, 1: false};

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadAll();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadAll() async {
    if (mounted) setState(() => _isLoading = true);
    await Future.wait([_loadPenalties(), _loadViolations()]);
    if (mounted) setState(() => _isLoading = false);
  }

  Future<void> _loadPenalties() async {
    final authService = Provider.of<AuthService>(context, listen: false);
    final user = authService.currentUser;
    if (user == null) {
      if (mounted) setState(() => _errorMessage = 'auth');
      return;
    }

    try {
      final url = Uri.parse("${ApiConstants.activityUrl}/penalties/${user.id}");
      final response = await authService.authenticatedRequest('GET', url);

      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(utf8.decode(response.bodyBytes));
        final penalties = data.map((v) => v as Map<String, dynamic>).toList();

        if (mounted) {
          setState(() {
            _penalties = penalties;
            _errorMessage = null;
          });
        }
      } else if (response.statusCode == 401 || response.statusCode == 403) {
        if (mounted) setState(() => _errorMessage = 'auth');
      }
    } catch (e) {
      if (mounted) {
        setState(() => _errorMessage = ErrorDisplayWidget.toVietnamese(e));
      }
    }
  }

  Future<void> _loadViolations() async {
    final authService = Provider.of<AuthService>(context, listen: false);

    try {
      final url = Uri.parse("${ApiConstants.violationReportUrl}/against-me");
      final response = await authService.authenticatedRequest(
        'GET',
        url,
        headers: {'Content-Type': 'application/json'},
      );

      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(utf8.decode(response.bodyBytes));
        final violations = data.map((v) => v as Map<String, dynamic>).toList();

        if (mounted) {
          setState(() {
            _violations = violations;
            _errorMessage = null;
          });
        }
      } else if (response.statusCode == 401 || response.statusCode == 403) {
        if (mounted) setState(() => _errorMessage = 'auth');
      }
    } catch (e) {
      if (mounted) {
        setState(() => _errorMessage = ErrorDisplayWidget.toVietnamese(e));
      }
    }
  }

  bool _matchesTimeFilter(DateTime dateTime) {
    final now = DateTime.now();
    switch (_selectedFilter) {
      case HistoryTimeFilter.last7Days:
        return !dateTime.isBefore(now.subtract(const Duration(days: 7)));
      case HistoryTimeFilter.last30Days:
        return !dateTime.isBefore(now.subtract(const Duration(days: 30)));
      case HistoryTimeFilter.all:
        return true;
    }
  }

  DateTime? _parseDate(dynamic value) {
    if (value == null) return null;
    if (value is DateTime) return value;
    if (value is String && value.isNotEmpty) {
      return DateTime.tryParse(value);
    }
    return null;
  }

  List<Map<String, dynamic>> get _filteredPenaltyItems {
    return _penalties.where((penalty) {
      final createdAt = _parseDate(penalty['createdAt']);
      return createdAt == null ? true : _matchesTimeFilter(createdAt);
    }).toList();
  }

  List<Map<String, dynamic>> get _visiblePenalties {
    final items = _filteredPenaltyItems;
    if (_expandedTabs[0] == true || items.length <= _collapsedLimit) {
      return items;
    }
    return items.take(_collapsedLimit).toList();
  }

  int get _filteredPenaltyCount => _filteredPenaltyItems.length;

  int get _filteredPenaltyPoints {
    return _filteredPenaltyItems.fold<int>(0, (sum, item) {
      return sum + (((item['points'] ?? 0) as num).toInt().abs());
    });
  }

  List<Map<String, dynamic>> get _filteredViolationItems {
    return _violations.where((violation) {
      final createdAt = _parseDate(violation['createdAt']);
      return createdAt == null ? true : _matchesTimeFilter(createdAt);
    }).toList();
  }

  List<Map<String, dynamic>> get _visibleViolations {
    final items = _filteredViolationItems;
    if (_expandedTabs[1] == true || items.length <= _collapsedLimit) {
      return items;
    }
    return items.take(_collapsedLimit).toList();
  }

  int get _filteredViolationCountAll => _filteredViolationItems.length;

  int get _filteredVerifiedViolationCount {
    return _filteredViolationItems
        .where((violation) => violation['status'] == 'VERIFIED')
        .length;
  }

  int get _filteredViolationPoints {
    return _filteredViolationItems.fold<int>(0, (sum, item) {
      if (item['status'] != 'VERIFIED') return sum;
      return sum + (((item['pointDeducted'] ?? 0) as num).toInt());
    });
  }

  Future<void> _pickFilter() async {
    final selected = await showHistoryFilterDialog(
      context,
      initialFilter: _selectedFilter,
    );
    if (selected == null || selected == _selectedFilter || !mounted) return;
    setState(() {
      _selectedFilter = selected;
      _expandedTabs.updateAll((_, __) => false);
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
        actions: [
          IconButton(
            onPressed: _pickFilter,
            icon: Icon(
              Icons.filter_list_rounded,
              color: _selectedFilter == HistoryTimeFilter.all
                  ? Colors.black87
                  : AppColors.brandColor,
            ),
            tooltip: 'Lọc theo thời gian',
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          labelColor: AppColors.brandColor,
          unselectedLabelColor: Colors.grey,
          indicatorColor: AppColors.brandColor,
          indicatorWeight: 3,
          labelStyle: const TextStyle(
            fontWeight: FontWeight.w600,
            fontSize: 14,
          ),
          unselectedLabelStyle: const TextStyle(
            fontWeight: FontWeight.w500,
            fontSize: 14,
          ),
          tabs: [
            Tab(
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.auto_fix_high, size: 18),
                  const SizedBox(width: 6),
                  const Text('Tự động'),
                  if (_penalties.isNotEmpty) ...[
                    const SizedBox(width: 6),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 6,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.red.withAlpha(30),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Text(
                        '${_penalties.length}',
                        style: const TextStyle(
                          fontSize: 11,
                          color: Colors.red,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ),
            Tab(
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.report_outlined, size: 18),
                  const SizedBox(width: 6),
                  const Text('Bị báo cáo'),
                  if (_violations.isNotEmpty) ...[
                    const SizedBox(width: 6),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 6,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.red.withAlpha(30),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Text(
                        '${_violations.length}',
                        style: const TextStyle(
                          fontSize: 11,
                          color: Colors.red,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: AppColors.brandColor),
            )
          : _errorMessage != null
          ? _errorMessage == 'auth'
                ? ErrorDisplayWidget.auth(onRetry: _loadAll)
                : ErrorDisplayWidget(message: _errorMessage!, onRetry: _loadAll)
          : TabBarView(
              controller: _tabController,
              children: [_buildPenaltiesTab(), _buildViolationsTab()],
            ),
    );
  }

  // ===================== TAB 1: AUTO PENALTIES =====================

  Widget _buildPenaltiesTab() {
    final visiblePenalties = _visiblePenalties;

    return RefreshIndicator(
      color: AppColors.brandColor,
      onRefresh: _loadAll,
      child: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(
            child: HistoryListControls(
              isExpanded: _expandedTabs[0] == true,
              onExpandedChanged: (expanded) {
                setState(() {
                  _expandedTabs[0] = expanded;
                });
              },
              totalCount: _filteredPenaltyCount,
              visibleCount: visiblePenalties.length,
            ),
          ),
          SliverToBoxAdapter(child: _buildPenaltyStatsHeader()),
          if (visiblePenalties.isEmpty)
            const SliverFillRemaining(
              child: _EmptyState(
                icon: Icons.verified_user_outlined,
                title: 'Tuyệt vời!',
                subtitle:
                    'Bạn chưa có vi phạm tự động nào.\nHãy tiếp tục giữ gìn nề nếp nhé!',
              ),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate(
                  (context, index) =>
                      _buildPenaltyCard(visiblePenalties[index]),
                  childCount: visiblePenalties.length,
                ),
              ),
            ),
          const SliverToBoxAdapter(child: SizedBox(height: 30)),
        ],
      ),
    );
  }

  Widget _buildPenaltyStatsHeader() {
    final bool clean = _filteredPenaltyCount == 0;
    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: clean
              ? [Colors.green, Colors.green.shade300]
              : [Colors.red.shade600, Colors.red.shade400],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
      ),
      child: clean
          ? const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  Icons.verified_user_outlined,
                  color: Colors.white,
                  size: 28,
                ),
                SizedBox(width: 12),
                Text(
                  'Chưa có vi phạm tự động!',
                  style: TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w600,
                    fontSize: 16,
                  ),
                ),
              ],
            )
          : Row(
              children: [
                Expanded(
                  child: _buildStatItem(
                    Icons.gpp_bad_outlined,
                    '$_filteredPenaltyCount',
                    'Lần vi phạm',
                  ),
                ),
                Container(
                  width: 1,
                  height: 50,
                  color: Colors.white.withAlpha(100),
                ),
                Expanded(
                  child: _buildStatItem(
                    Icons.remove_circle_outline,
                    '-$_filteredPenaltyPoints',
                    'Điểm bị trừ',
                  ),
                ),
              ],
            ),
    );
  }

  Widget _buildPenaltyCard(Map<String, dynamic> penalty) {
    final type = penalty['transactionType'] ?? '';
    final title = penalty['title'] ?? '';
    final description = penalty['description'] ?? '';
    final points = ((penalty['points'] ?? 0) as num).toInt();
    final createdAtStr = penalty['createdAt'];
    final appealStatus = penalty['appealStatus'];
    final typeInfo = _getPenaltyTypeInfo(type);

    return GestureDetector(
      onTap: () => _showPenaltyDetail(penalty),
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border(left: BorderSide(color: typeInfo.color, width: 4)),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withAlpha(10),
              blurRadius: 10,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: typeInfo.color.withAlpha(30),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(typeInfo.icon, color: typeInfo.color, size: 24),
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
                      if (description.isNotEmpty)
                        Text(
                          description,
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
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 6,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.red.withAlpha(25),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    '$points',
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
            const Divider(height: 1),
            const SizedBox(height: 12),
            Row(
              children: [
                _buildBadge(typeInfo.label, typeInfo.color),
                if (appealStatus != null) ...[
                  const SizedBox(width: 6),
                  _buildBadge(
                    _getAppealLabel(appealStatus),
                    _getAppealColor(appealStatus),
                  ),
                ],
                const Spacer(),
                Icon(Icons.access_time, size: 14, color: Colors.grey[400]),
                const SizedBox(width: 4),
                Text(
                  _formatDateTime(createdAtStr),
                  style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  void _showPenaltyDetail(Map<String, dynamic> penalty) {
    final type = penalty['transactionType'] ?? '';
    final title = penalty['title'] ?? '';
    final description = penalty['description'] ?? '';
    final points = ((penalty['points'] ?? 0) as num).toInt();
    final balanceAfter = penalty['balanceAfter'];
    final createdAtStr = penalty['createdAt'];
    final appealStatus = penalty['appealStatus'];
    final appealNote = penalty['appealResolutionNote'];
    final typeInfo = _getPenaltyTypeInfo(type);

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => Container(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.of(context).size.height * 0.85,
        ),
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
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

              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: typeInfo.color.withAlpha(30),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Icon(typeInfo.icon, color: typeInfo.color, size: 28),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          title,
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 6),
                        Row(
                          children: [
                            _buildBadge(typeInfo.label, typeInfo.color),
                            if (appealStatus != null) ...[
                              const SizedBox(width: 6),
                              _buildBadge(
                                _getAppealLabel(appealStatus),
                                _getAppealColor(appealStatus),
                              ),
                            ],
                          ],
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 10,
                    ),
                    decoration: BoxDecoration(
                      color: Colors.red.withAlpha(25),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Column(
                      children: [
                        Text(
                          '$points',
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

              if (description.isNotEmpty) ...[
                _buildDetailRow(
                  Icons.description_outlined,
                  'Mô tả',
                  description,
                ),
                const SizedBox(height: 16),
              ],
              _buildDetailRow(
                Icons.calendar_today,
                'Thời gian',
                _formatDateTimeFull(createdAtStr),
              ),
              if (balanceAfter != null) ...[
                const SizedBox(height: 16),
                _buildDetailRow(
                  Icons.account_balance_wallet_outlined,
                  'Điểm còn lại',
                  '$balanceAfter điểm',
                ),
              ],

              // Appeal result note
              if (appealNote != null && appealNote.toString().isNotEmpty) ...[
                const SizedBox(height: 20),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: _getAppealColor(appealStatus ?? '').withAlpha(15),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(
                      color: _getAppealColor(appealStatus ?? '').withAlpha(50),
                    ),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Phản hồi kháng cáo:',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: _getAppealColor(appealStatus ?? ''),
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        appealNote.toString(),
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.grey[800],
                          height: 1.4,
                        ),
                      ),
                    ],
                  ),
                ),
              ],

              const SizedBox(height: 24),

              // Appeal button: only show when no existing appeal
              if (appealStatus == null)
                SizedBox(
                  width: double.infinity,
                  height: 48,
                  child: ElevatedButton.icon(
                    onPressed: () {
                      Navigator.pop(ctx);
                      _showPenaltyAppealDialog(penalty);
                    },
                    icon: const Icon(Icons.gavel_rounded, size: 20),
                    label: const Text(
                      'Kháng cáo vi phạm',
                      style: TextStyle(fontWeight: FontWeight.bold),
                    ),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.brandColor,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      elevation: 0,
                    ),
                  ),
                ),

              if (appealStatus == 'PENDING')
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Colors.orange.withAlpha(20),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.hourglass_top_rounded,
                        size: 18,
                        color: Colors.orange,
                      ),
                      SizedBox(width: 8),
                      Text(
                        'Kháng cáo đang chờ xử lý',
                        style: TextStyle(
                          color: Colors.orange,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),

              if (appealStatus == null) ...[
                const SizedBox(height: 16),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Colors.blue.withAlpha(15),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: Colors.blue.withAlpha(50)),
                  ),
                  child: Row(
                    children: [
                      const Icon(
                        Icons.info_outline,
                        size: 18,
                        color: Colors.blue,
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          'Nếu bạn cho rằng vi phạm này không chính xác, bạn có thể kháng cáo để thủ thư xem xét lại.',
                          style: TextStyle(
                            fontSize: 13,
                            color: Colors.grey[700],
                            height: 1.4,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],

              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }

  void _showPenaltyAppealDialog(Map<String, dynamic> penalty) {
    final appealController = TextEditingController();
    final penaltyId = penalty['id'];
    final title = penalty['title'] ?? '';

    showDialog(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: AppColors.brandColor.withAlpha(30),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(
                Icons.gavel_rounded,
                color: AppColors.brandColor,
                size: 20,
              ),
            ),
            const SizedBox(width: 12),
            const Expanded(
              child: Text(
                'Kháng cáo vi phạm',
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
              ),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Vui lòng mô tả lý do bạn cho rằng vi phạm "$title" không chính xác:',
              style: TextStyle(color: Colors.grey[600], fontSize: 14),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: appealController,
              maxLines: 4,
              decoration: InputDecoration(
                hintText: 'Nhập lý do kháng cáo...',
                hintStyle: TextStyle(color: Colors.grey[400]),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: const BorderSide(
                    color: AppColors.brandColor,
                    width: 2,
                  ),
                ),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogCtx),
            child: const Text('Hủy', style: TextStyle(color: Colors.grey)),
          ),
          ElevatedButton(
            onPressed: () async {
              final reason = appealController.text.trim();
              if (reason.isEmpty) {
                SnackbarGuard.show(
                  context,
                  key: 'violation_appeal_empty_reason',
                  message: 'Vui lòng nhập lý do kháng cáo',
                  backgroundColor: Colors.red,
                );
                return;
              }
              Navigator.pop(dialogCtx);
              await _submitPenaltyAppeal(penaltyId?.toString(), title, reason);
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.brandColor,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            child: const Text('Gửi kháng cáo'),
          ),
        ],
      ),
    );
  }

  Future<void> _submitPenaltyAppeal(
    String? penaltyId,
    String title,
    String reason,
  ) async {
    try {
      final authService = Provider.of<AuthService>(context, listen: false);

      final body = jsonEncode({
        'subject': 'Kháng cáo: $title',
        'content': reason,
        if (penaltyId != null) 'pointTransactionId': penaltyId,
      });

      final url = Uri.parse('${ApiConstants.domain}/slib/complaints');
      final response = await authService.authenticatedRequest(
        'POST',
        url,
        headers: {'Content-Type': 'application/json'},
        body: body,
      );

      if (!mounted) return;

      if (response.statusCode == 201 || response.statusCode == 200) {
        SnackbarGuard.show(
          context,
          key: 'violation_appeal_success',
          message: 'Đã gửi kháng cáo thành công. Thủ thư sẽ xem xét.',
          backgroundColor: Colors.green,
        );
        _loadAll();
      } else {
        final errorBody = utf8.decode(response.bodyBytes);
        String msg;
        try {
          msg = jsonDecode(errorBody)['message'] ?? errorBody;
        } catch (_) {
          msg = errorBody;
        }
        SnackbarGuard.show(
          context,
          key: 'violation_appeal_error',
          message: 'Lỗi: $msg',
          backgroundColor: Colors.red,
        );
      }
    } catch (e) {
      if (mounted) {
        SnackbarGuard.show(
          context,
          key: 'violation_appeal_exception',
          message: 'Lỗi: $e',
          backgroundColor: Colors.red,
        );
      }
    }
  }

  // ===================== TAB 2: REPORTED VIOLATIONS =====================

  Widget _buildViolationsTab() {
    final visibleViolations = _visibleViolations;

    return RefreshIndicator(
      color: AppColors.brandColor,
      onRefresh: _loadAll,
      child: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(
            child: HistoryListControls(
              isExpanded: _expandedTabs[1] == true,
              onExpandedChanged: (expanded) {
                setState(() {
                  _expandedTabs[1] = expanded;
                });
              },
              totalCount: _filteredViolationCountAll,
              visibleCount: visibleViolations.length,
            ),
          ),
          SliverToBoxAdapter(child: _buildViolationStatsHeader()),
          if (visibleViolations.isEmpty)
            const SliverFillRemaining(
              child: _EmptyState(
                icon: Icons.shield_outlined,
                title: 'Không có báo cáo!',
                subtitle:
                    'Bạn chưa bị báo cáo vi phạm nào.\nHãy tiếp tục giữ gìn nề nếp nhé!',
              ),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate(
                  (context, index) =>
                      _buildViolationCard(visibleViolations[index]),
                  childCount: visibleViolations.length,
                ),
              ),
            ),
          const SliverToBoxAdapter(child: SizedBox(height: 30)),
        ],
      ),
    );
  }

  Widget _buildViolationStatsHeader() {
    final bool clean = _filteredViolationCountAll == 0;
    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: clean
              ? [Colors.green, Colors.green.shade300]
              : [AppColors.brandColor, AppColors.brandColor.withAlpha(200)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
      ),
      child: clean
          ? const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.shield_outlined, color: Colors.white, size: 28),
                SizedBox(width: 12),
                Text(
                  'Chưa bị báo cáo vi phạm!',
                  style: TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w600,
                    fontSize: 16,
                  ),
                ),
              ],
            )
          : Row(
              children: [
                Expanded(
                  child: _buildStatItem(
                    Icons.warning_amber_rounded,
                    '$_filteredVerifiedViolationCount',
                    'Vi phạm xác nhận',
                  ),
                ),
                Container(
                  width: 1,
                  height: 50,
                  color: Colors.white.withAlpha(100),
                ),
                Expanded(
                  child: _buildStatItem(
                    Icons.remove_circle_outline,
                    '-$_filteredViolationPoints',
                    'Điểm bị trừ',
                  ),
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
    final appealStatus = violation['appealStatus'];
    final typeInfo = _getViolationTypeInfo(violationType);
    final statusInfo = _getStatusInfo(status);

    return GestureDetector(
      onTap: () => _showViolationDetail(violation),
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border(left: BorderSide(color: typeInfo.color, width: 4)),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withAlpha(10),
              blurRadius: 10,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: typeInfo.color.withAlpha(30),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(typeInfo.icon, color: typeInfo.color, size: 24),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        typeLabel,
                        style: const TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 15,
                        ),
                      ),
                      const SizedBox(height: 4),
                      if (description.isNotEmpty)
                        Text(
                          description,
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
                if (status == 'VERIFIED' && pointDeducted > 0)
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 6,
                    ),
                    decoration: BoxDecoration(
                      color: Colors.red.withAlpha(25),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      '-$pointDeducted',
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
            const Divider(height: 1),
            const SizedBox(height: 12),
            Row(
              children: [
                if (seatCode.isNotEmpty) ...[
                  Icon(Icons.event_seat, size: 14, color: Colors.grey[400]),
                  const SizedBox(width: 4),
                  Text(
                    '$zoneName - $seatCode',
                    style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                  ),
                ],
                const Spacer(),
                _buildBadge(statusInfo.label, statusInfo.color),
                if (appealStatus != null) ...[
                  const SizedBox(width: 6),
                  _buildBadge(
                    _getAppealLabel(appealStatus),
                    _getAppealColor(appealStatus),
                  ),
                ],
                const SizedBox(width: 8),
                Icon(Icons.access_time, size: 14, color: Colors.grey[400]),
                const SizedBox(width: 4),
                Text(
                  _formatDateTime(createdAtStr),
                  style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  // ===================== DETAIL BOTTOM SHEET (VIOLATIONS) =====================

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
    final appealStatus = violation['appealStatus'];
    final appealNote = violation['appealResolutionNote'];
    final typeInfo = _getViolationTypeInfo(violationType);
    final statusInfo = _getStatusInfo(status);

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => Container(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.of(context).size.height * 0.85,
        ),
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
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

              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: typeInfo.color.withAlpha(30),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Icon(typeInfo.icon, color: typeInfo.color, size: 28),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          typeLabel,
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 6),
                        Row(
                          children: [
                            _buildBadge(statusInfo.label, statusInfo.color),
                            if (appealStatus != null) ...[
                              const SizedBox(width: 6),
                              _buildBadge(
                                _getAppealLabel(appealStatus),
                                _getAppealColor(appealStatus),
                              ),
                            ],
                          ],
                        ),
                      ],
                    ),
                  ),
                  if (status == 'VERIFIED' && pointDeducted > 0)
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 10,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.red.withAlpha(25),
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: Column(
                        children: [
                          Text(
                            '-$pointDeducted',
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

              if (description.isNotEmpty) ...[
                _buildDetailRow(
                  Icons.description_outlined,
                  'Mô tả',
                  description,
                ),
                const SizedBox(height: 16),
              ],
              if (seatCode.isNotEmpty) ...[
                _buildDetailRow(
                  Icons.event_seat,
                  'Vị trí',
                  '$areaName - $zoneName - Ghế $seatCode',
                ),
                const SizedBox(height: 16),
              ],
              _buildDetailRow(
                Icons.calendar_today,
                'Thời gian',
                _formatDateTimeFull(createdAtStr),
              ),

              if (evidenceUrl != null && evidenceUrl.toString().isNotEmpty) ...[
                const SizedBox(height: 16),
                Text(
                  'Bằng chứng:',
                  style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                ),
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
                      decoration: BoxDecoration(
                        color: Colors.grey[100],
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Center(
                        child: Icon(Icons.broken_image, color: Colors.grey),
                      ),
                    ),
                  ),
                ),
              ],

              if (appealNote != null && appealNote.toString().isNotEmpty) ...[
                const SizedBox(height: 20),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: _getAppealColor(appealStatus ?? '').withAlpha(15),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(
                      color: _getAppealColor(appealStatus ?? '').withAlpha(50),
                    ),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Phản hồi kháng cáo:',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: _getAppealColor(appealStatus ?? ''),
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        appealNote.toString(),
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.grey[800],
                          height: 1.4,
                        ),
                      ),
                    ],
                  ),
                ),
              ],

              const SizedBox(height: 24),

              if (status == 'VERIFIED' && appealStatus == null)
                SizedBox(
                  width: double.infinity,
                  height: 48,
                  child: ElevatedButton.icon(
                    onPressed: () {
                      Navigator.pop(ctx);
                      _showAppealDialog(violation);
                    },
                    icon: const Icon(Icons.gavel_rounded, size: 20),
                    label: const Text(
                      'Kháng cáo vi phạm',
                      style: TextStyle(fontWeight: FontWeight.bold),
                    ),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.brandColor,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      elevation: 0,
                    ),
                  ),
                ),

              if (appealStatus == 'PENDING')
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Colors.orange.withAlpha(20),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.hourglass_top_rounded,
                        size: 18,
                        color: Colors.orange,
                      ),
                      SizedBox(width: 8),
                      Text(
                        'Kháng cáo đang chờ xử lý',
                        style: TextStyle(
                          color: Colors.orange,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),

              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }

  // ===================== APPEAL DIALOG =====================

  void _showAppealDialog(Map<String, dynamic> violation) {
    final appealController = TextEditingController();
    final violationId = violation['id'];
    final typeLabel =
        violation['violationTypeLabel'] ?? violation['violationType'] ?? '';

    showDialog(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: AppColors.brandColor.withAlpha(30),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(
                Icons.gavel_rounded,
                color: AppColors.brandColor,
                size: 20,
              ),
            ),
            const SizedBox(width: 12),
            const Expanded(
              child: Text(
                'Kháng cáo vi phạm',
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
              ),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Vui lòng mô tả lý do bạn cho rằng vi phạm này không chính xác:',
              style: TextStyle(color: Colors.grey[600], fontSize: 14),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: appealController,
              maxLines: 4,
              decoration: InputDecoration(
                hintText: 'Nhập lý do kháng cáo...',
                hintStyle: TextStyle(color: Colors.grey[400]),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: const BorderSide(
                    color: AppColors.brandColor,
                    width: 2,
                  ),
                ),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogCtx),
            child: const Text('Hủy', style: TextStyle(color: Colors.grey)),
          ),
          ElevatedButton(
            onPressed: () async {
              final reason = appealController.text.trim();
              if (reason.isEmpty) {
                SnackbarGuard.show(
                  context,
                  key: 'reported_violation_appeal_empty_reason',
                  message: 'Vui lòng nhập lý do kháng cáo',
                  backgroundColor: Colors.red,
                );
                return;
              }
              Navigator.pop(dialogCtx);
              await _submitAppeal(violationId?.toString(), typeLabel, reason);
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.brandColor,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            child: const Text('Gửi kháng cáo'),
          ),
        ],
      ),
    );
  }

  Future<void> _submitAppeal(
    String? violationId,
    String typeLabel,
    String reason,
  ) async {
    try {
      final authService = Provider.of<AuthService>(context, listen: false);

      final body = jsonEncode({
        'subject': 'Kháng cáo: $typeLabel',
        'content': reason,
        if (violationId != null) 'violationReportId': violationId,
      });

      final url = Uri.parse('${ApiConstants.domain}/slib/complaints');
      final response = await authService.authenticatedRequest(
        'POST',
        url,
        headers: {'Content-Type': 'application/json'},
        body: body,
      );

      if (!mounted) return;

      if (response.statusCode == 201 || response.statusCode == 200) {
        SnackbarGuard.show(
          context,
          key: 'reported_violation_appeal_success',
          message: 'Đã gửi kháng cáo thành công. Thủ thư sẽ xem xét.',
          backgroundColor: Colors.green,
        );
        _loadAll();
      } else {
        final errorBody = utf8.decode(response.bodyBytes);
        String msg;
        try {
          msg = jsonDecode(errorBody)['message'] ?? errorBody;
        } catch (_) {
          msg = errorBody;
        }
        SnackbarGuard.show(
          context,
          key: 'reported_violation_appeal_error',
          message: 'Lỗi: $msg',
          backgroundColor: Colors.red,
        );
      }
    } catch (e) {
      if (mounted) {
        SnackbarGuard.show(
          context,
          key: 'reported_violation_appeal_exception',
          message: 'Lỗi: $e',
          backgroundColor: Colors.red,
        );
      }
    }
  }

  // ===================== SHARED WIDGETS =====================

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

  Widget _buildBadge(String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withAlpha(25),
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
                style: TextStyle(fontSize: 12, color: Colors.grey[500]),
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

  // ===================== TYPE HELPERS =====================

  _PenaltyTypeInfo _getPenaltyTypeInfo(String type) {
    return switch (type) {
      'NO_SHOW_PENALTY' => _PenaltyTypeInfo(
        Icons.event_busy,
        Colors.red,
        'Không đến',
      ),
      'LATE_CHECKIN_PENALTY' => _PenaltyTypeInfo(
        Icons.schedule,
        Colors.orange,
        'Check-in trễ',
      ),
      'CHECK_OUT_LATE_PENALTY' => _PenaltyTypeInfo(
        Icons.timer_off,
        Colors.deepOrange,
        'Quên check-out',
      ),
      'PENALTY' => _PenaltyTypeInfo(
        Icons.gpp_bad,
        Colors.red.shade700,
        'Vi phạm',
      ),
      _ => _PenaltyTypeInfo(Icons.warning_amber, Colors.grey, 'Khác'),
    };
  }

  _ViolationTypeInfo _getViolationTypeInfo(String type) {
    return switch (type) {
      'UNAUTHORIZED_USE' => _ViolationTypeInfo(Icons.block, Colors.red),
      'LEFT_BELONGINGS' => _ViolationTypeInfo(Icons.inventory_2, Colors.orange),
      'NOISE' => _ViolationTypeInfo(Icons.volume_up, Colors.purple),
      'FEET_ON_SEAT' => _ViolationTypeInfo(
        Icons.airline_seat_recline_normal,
        Colors.brown,
      ),
      'FOOD_DRINK' => _ViolationTypeInfo(Icons.fastfood, Colors.deepOrange),
      'SLEEPING' => _ViolationTypeInfo(Icons.hotel, Colors.indigo),
      _ => _ViolationTypeInfo(Icons.warning_amber, Colors.grey),
    };
  }

  _StatusInfo _getStatusInfo(String status) {
    return switch (status) {
      'VERIFIED' => _StatusInfo('Xác nhận', Colors.red),
      'PENDING' => _StatusInfo('Chờ xử lý', Colors.orange),
      'REJECTED' => _StatusInfo('Đã bác bỏ', Colors.green),
      'RESOLVED' => _StatusInfo('Đã xử lý', Colors.blue),
      _ => _StatusInfo(status, Colors.grey),
    };
  }

  String _getAppealLabel(String status) {
    return switch (status) {
      'PENDING' => 'Đang kháng cáo',
      'ACCEPTED' => 'Kháng cáo thành công',
      'DENIED' => 'Kháng cáo bị từ chối',
      _ => status,
    };
  }

  Color _getAppealColor(String status) {
    return switch (status) {
      'PENDING' => Colors.orange,
      'ACCEPTED' => Colors.green,
      'DENIED' => Colors.red,
      _ => Colors.grey,
    };
  }

  String _formatDateTime(dynamic raw) {
    if (raw == null) return '';
    String str = raw.toString();
    final bracketIndex = str.indexOf('[');
    if (bracketIndex != -1) str = str.substring(0, bracketIndex);
    str = str.replaceAll(RegExp(r'[+-]\d{2}:\d{2}$'), '');
    final dt = DateTime.tryParse(str);
    if (dt == null) return '';

    final now = DateTime.now();
    final diff = now.difference(dt);
    if (diff.inDays == 0) return '${DateFormat('HH:mm').format(dt)} - Hôm nay';
    if (diff.inDays == 1) return 'Hôm qua';
    if (diff.inDays < 7) return '${diff.inDays} ngày trước';
    return DateFormat('dd/MM/yyyy').format(dt);
  }

  String _formatDateTimeFull(dynamic raw) {
    if (raw == null) return '';
    String str = raw.toString();
    final bracketIndex = str.indexOf('[');
    if (bracketIndex != -1) str = str.substring(0, bracketIndex);
    str = str.replaceAll(RegExp(r'[+-]\d{2}:\d{2}$'), '');
    final dt = DateTime.tryParse(str);
    if (dt == null) return '';
    return DateFormat('HH:mm - dd/MM/yyyy').format(dt);
  }
}

class _PenaltyTypeInfo {
  final IconData icon;
  final Color color;
  final String label;
  const _PenaltyTypeInfo(this.icon, this.color, this.label);
}

class _ViolationTypeInfo {
  final IconData icon;
  final Color color;
  const _ViolationTypeInfo(this.icon, this.color);
}

class _StatusInfo {
  final String label;
  final Color color;
  const _StatusInfo(this.label, this.color);
}

class _EmptyState extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  const _EmptyState({
    required this.icon,
    required this.title,
    required this.subtitle,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: Colors.green.withAlpha(25),
              shape: BoxShape.circle,
            ),
            child: Icon(icon, size: 64, color: Colors.green),
          ),
          const SizedBox(height: 20),
          Text(
            title,
            style: const TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.bold,
              color: Colors.black87,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            subtitle,
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
}
