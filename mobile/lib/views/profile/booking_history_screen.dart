import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/upcoming_booking.dart';
import 'package:slib/services/app/history_preferences_service.dart';
import 'package:slib/services/auth/auth_service.dart';
import 'package:slib/views/home/widgets/booking_action_dialog.dart';
import 'package:slib/views/profile/widgets/history_list_controls.dart';
import 'package:slib/views/widgets/error_display_widget.dart';

class BookingHistoryScreen extends StatefulWidget {
  final int initialTab;

  const BookingHistoryScreen({super.key, this.initialTab = 0});

  @override
  State<BookingHistoryScreen> createState() => _BookingHistoryScreenState();
}

class _BookingHistoryScreenState extends State<BookingHistoryScreen> {
  static const int _collapsedLimit = 10;
  static const String _bookingHideScope = 'booking_history';

  final _historyPreferences = HistoryPreferencesService();

  List<Map<String, dynamic>> _activeBookings = [];
  List<Map<String, dynamic>> _completedBookings = [];
  List<Map<String, dynamic>> _cancelledBookings = [];
  Set<String> _hiddenBookingIds = <String>{};

  bool _isLoading = true;
  String? _errorMessage;
  String? _currentUserId;
  HistoryTimeFilter _selectedFilter = HistoryTimeFilter.all;
  final Map<int, bool> _expandedTabs = {0: false, 1: false, 2: false};

  @override
  void initState() {
    super.initState();
    _loadBookings();
  }

  Future<void> _loadBookings() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    final authService = Provider.of<AuthService>(context, listen: false);
    final user = authService.currentUser;

    if (user == null) {
      setState(() {
        _errorMessage = 'auth';
        _isLoading = false;
      });
      return;
    }

    _currentUserId = user.id;

    try {
      _hiddenBookingIds = await _historyPreferences.loadHiddenIds(
        scope: _bookingHideScope,
        userId: user.id,
      );

      final url = Uri.parse("${ApiConstants.bookingUrl}/user/${user.id}");
      final response = await authService.authenticatedRequest('GET', url);

      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(utf8.decode(response.bodyBytes));
        final now = DateTime.now();

        _activeBookings = [];
        _completedBookings = [];
        _cancelledBookings = [];

        for (final booking in data) {
          final status = (booking['status'] ?? '').toString().toUpperCase();
          final endTime = DateTime.parse(booking['endTime']);
          final startTime = DateTime.parse(booking['startTime']);

          final parsedBooking = {
            'reservationId': booking['reservationId']?.toString() ?? '',
            'seatId': booking['seatId'] ?? 0,
            'seatCode': booking['seatCode'] ?? 'N/A',
            'zoneId': booking['zoneId'] ?? 0,
            'zoneName': booking['zoneName'] ?? 'N/A',
            'areaId': booking['areaId'] ?? 0,
            'areaName': booking['areaName'] ?? 'N/A',
            'startTime': startTime,
            'endTime': endTime,
            'actualEndTime': booking['actualEndTime'] != null
                ? DateTime.tryParse(booking['actualEndTime'])
                : null,
            'status': status,
            'cancellationReason': booking['cancellationReason']?.toString(),
            'cancelledByStaff': booking['cancelledByStaff'] == true,
            'layoutChanged': booking['layoutChanged'] == true,
            'layoutChangeTitle': booking['layoutChangeTitle']?.toString(),
            'layoutChangeMessage': booking['layoutChangeMessage']?.toString(),
            'layoutChangedAt': booking['layoutChangedAt'] != null
                ? DateTime.tryParse(booking['layoutChangedAt'].toString())
                : null,
            'canCancel': booking['canCancel'] == true,
            'canChangeSeat': booking['canChangeSeat'] == true,
            'date': DateFormat('dd/MM/yyyy').format(startTime),
            'time':
                '${DateFormat('HH:mm').format(startTime)} - ${DateFormat('HH:mm').format(endTime)}',
          };

          if (status == 'CANCEL' ||
              status == 'CANCELLED' ||
              status == 'EXPIRED') {
            _cancelledBookings.add(parsedBooking);
          } else if (status == 'COMPLETED' || endTime.isBefore(now)) {
            _completedBookings.add(parsedBooking);
          } else {
            _activeBookings.add(parsedBooking);
          }
        }

        _activeBookings.sort(
          (a, b) => (a['startTime'] as DateTime).compareTo(b['startTime']),
        );
        _completedBookings.sort(
          (a, b) => (b['startTime'] as DateTime).compareTo(a['startTime']),
        );
        _cancelledBookings.sort(
          (a, b) => (b['startTime'] as DateTime).compareTo(a['startTime']),
        );

        if (!mounted) return;
        setState(() => _isLoading = false);
      } else if (response.statusCode == 401 || response.statusCode == 403) {
        setState(() {
          _errorMessage = 'auth';
          _isLoading = false;
        });
      } else {
        setState(() {
          _errorMessage = ErrorDisplayWidget.toVietnamese(
            'status ${response.statusCode}',
          );
          _isLoading = false;
        });
      }
    } catch (e) {
      setState(() {
        _errorMessage = ErrorDisplayWidget.toVietnamese(e);
        _isLoading = false;
      });
    }
  }

  void _showActionDialog(Map<String, dynamic> booking) {
    final upcomingBooking = UpcomingBooking(
      reservationId: booking['reservationId'],
      status: booking['status'],
      seatId: booking['seatId'] ?? 0,
      seatCode: booking['seatCode'],
      zoneId: booking['zoneId'] ?? 0,
      zoneName: booking['zoneName'],
      areaId: booking['areaId'] ?? 0,
      areaName: booking['areaName'],
      startTime: booking['startTime'],
      endTime: booking['endTime'],
      dayOfWeek: '',
      dayOfMonth: (booking['startTime'] as DateTime).day,
      timeRange: booking['time'],
      layoutChanged: booking['layoutChanged'] == true,
      layoutChangeTitle: booking['layoutChangeTitle']?.toString(),
      layoutChangeMessage: booking['layoutChangeMessage']?.toString(),
      layoutChangedAt: booking['layoutChangedAt'] as DateTime?,
      canCancel: booking['canCancel'] == true,
      canChangeSeat: booking['canChangeSeat'] == true,
    );

    BookingActionDialog.show(context, upcomingBooking, _loadBookings);
  }

  Future<void> _hideBooking(Map<String, dynamic> booking) async {
    final reservationId = booking['reservationId']?.toString();
    final userId = _currentUserId;
    if (reservationId == null || reservationId.isEmpty || userId == null) {
      return;
    }

    await _historyPreferences.hideItem(
      scope: _bookingHideScope,
      userId: userId,
      itemId: reservationId,
    );

    if (!mounted) return;
    setState(() {
      _hiddenBookingIds = {..._hiddenBookingIds, reservationId};
    });

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: const Text('Đã ẩn lịch sử đặt chỗ khỏi danh sách này.'),
        action: SnackBarAction(
          label: 'Hoàn tác',
          onPressed: () async {
            await _historyPreferences.unhideItem(
              scope: _bookingHideScope,
              userId: userId,
              itemId: reservationId,
            );
            if (!mounted) return;
            setState(() {
              _hiddenBookingIds.remove(reservationId);
            });
          },
        ),
      ),
    );
  }

  Future<void> _restoreHiddenBookings() async {
    final userId = _currentUserId;
    if (userId == null || _hiddenBookingIds.isEmpty) return;

    await _historyPreferences.clearHiddenItems(
      scope: _bookingHideScope,
      userId: userId,
    );

    if (!mounted) return;
    setState(() {
      _hiddenBookingIds = <String>{};
    });

    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('Đã hiện lại các mục đã ẩn.')));
  }

  Widget _buildErrorWidget() {
    if (_errorMessage == 'auth') {
      return ErrorDisplayWidget.auth(onRetry: _loadBookings);
    }
    return ErrorDisplayWidget(message: _errorMessage!, onRetry: _loadBookings);
  }

  List<Map<String, dynamic>> _visibleBookings(
    List<Map<String, dynamic>> bookings, {
    required int tabIndex,
  }) {
    final filtered = bookings.where((booking) {
      final reservationId = booking['reservationId']?.toString() ?? '';
      if (_hiddenBookingIds.contains(reservationId)) {
        return false;
      }
      final startTime = booking['startTime'] as DateTime;
      return _matchesTimeFilter(startTime);
    }).toList();

    if (_expandedTabs[tabIndex] == true || filtered.length <= _collapsedLimit) {
      return filtered;
    }
    return filtered.take(_collapsedLimit).toList();
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

  String _emptyMessageForList(List<Map<String, dynamic>> bookings) {
    if (bookings.isEmpty) return 'Không có dữ liệu';
    if (_hiddenBookingIds.isNotEmpty) return 'Không còn mục nào sau khi lọc/ẩn';
    return 'Không có dữ liệu phù hợp trong khoảng thời gian đã chọn';
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
    return DefaultTabController(
      length: 3,
      initialIndex: widget.initialTab.clamp(0, 2),
      child: Scaffold(
        backgroundColor: const Color(0xFFF5F7FA),
        appBar: AppBar(
          title: const Text(
            'Lịch sử đặt chỗ',
            style: TextStyle(
              fontWeight: FontWeight.bold,
              color: Colors.black87,
            ),
          ),
          backgroundColor: Colors.white,
          centerTitle: true,
          elevation: 0,
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
            if (_hiddenBookingIds.isNotEmpty)
              IconButton(
                onPressed: _restoreHiddenBookings,
                icon: const Icon(Icons.visibility_outlined),
                tooltip: 'Hiện lại các mục đã ẩn',
              ),
          ],
          bottom: TabBar(
            labelColor: AppColors.brandColor,
            unselectedLabelColor: Colors.grey,
            indicatorColor: AppColors.brandColor,
            indicatorWeight: 3,
            labelStyle: const TextStyle(fontWeight: FontWeight.bold),
            tabs: const [
              Tab(text: 'Sắp tới'),
              Tab(text: 'Hoàn thành'),
              Tab(text: 'Đã huỷ'),
            ],
          ),
        ),
        body: _isLoading
            ? const Center(
                child: CircularProgressIndicator(color: AppColors.brandColor),
              )
            : _errorMessage != null
            ? _buildErrorWidget()
            : TabBarView(
                children: [
                  _buildBookingList(
                    _activeBookings,
                    tabIndex: 0,
                    isActive: true,
                  ),
                  _buildBookingList(_completedBookings, tabIndex: 1),
                  _buildBookingList(_cancelledBookings, tabIndex: 2),
                ],
              ),
      ),
    );
  }

  Widget _buildBookingList(
    List<Map<String, dynamic>> bookings, {
    required int tabIndex,
    bool isActive = false,
  }) {
    final visibleBookings = _visibleBookings(bookings, tabIndex: tabIndex);
    final totalAfterFilter = bookings.where((booking) {
      final reservationId = booking['reservationId']?.toString() ?? '';
      if (_hiddenBookingIds.contains(reservationId)) {
        return false;
      }
      return _matchesTimeFilter(booking['startTime'] as DateTime);
    }).length;

    return RefreshIndicator(
      color: AppColors.brandColor,
      onRefresh: _loadBookings,
      child: ListView.separated(
        padding: const EdgeInsets.only(bottom: 24),
        itemCount: visibleBookings.length + 1,
        separatorBuilder: (context, index) =>
            index == 0 ? const SizedBox.shrink() : const SizedBox(height: 12),
        itemBuilder: (context, index) {
          if (index == 0) {
            return Column(
              children: [
                HistoryListControls(
                  isExpanded: _expandedTabs[tabIndex] == true,
                  onExpandedChanged: (expanded) {
                    setState(() {
                      _expandedTabs[tabIndex] = expanded;
                    });
                  },
                  totalCount: totalAfterFilter,
                  visibleCount: visibleBookings.length,
                  hiddenCount: _hiddenBookingIds.length,
                  onRestoreHidden: _hiddenBookingIds.isEmpty
                      ? null
                      : _restoreHiddenBookings,
                ),
                if (visibleBookings.isEmpty)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
                    child: ErrorDisplayWidget.empty(
                      message: _emptyMessageForList(bookings),
                    ),
                  ),
              ],
            );
          }

          final booking = visibleBookings[index - 1];
          return Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: _buildBookingCard(booking, isActive: isActive),
          );
        },
      ),
    );
  }

  Widget _buildBookingCard(
    Map<String, dynamic> booking, {
    bool isActive = false,
  }) {
    final status = booking['status'] as String;
    final startTime = booking['startTime'] as DateTime;
    final endTime = booking['endTime'] as DateTime;
    final actualEndTime = booking['actualEndTime'] as DateTime?;
    final cancellationReason = booking['cancellationReason'] as String?;
    final cancelledByStaff = booking['cancelledByStaff'] == true;
    final hasLayoutWarning = booking['layoutChanged'] == true;
    final now = DateTime.now();

    Color statusColor;
    String statusText;
    IconData statusIcon;
    final isOngoing = now.isAfter(startTime) && now.isBefore(endTime);

    if (hasLayoutWarning) {
      statusColor = const Color(0xFFEA580C);
      statusText = 'Cần kiểm tra lại';
      statusIcon = Icons.warning_amber_rounded;
    } else if (status == 'CANCEL' || status == 'CANCELLED') {
      statusColor = Colors.red;
      statusText = 'Đã huỷ';
      statusIcon = Icons.cancel_outlined;
    } else if (status == 'EXPIRED') {
      statusColor = Colors.orange;
      statusText = 'Không đến';
      statusIcon = Icons.warning_amber_rounded;
    } else if (status == 'COMPLETED' || endTime.isBefore(now)) {
      statusColor = Colors.green;
      statusText = 'Hoàn thành';
      statusIcon = Icons.check_circle;
    } else if (isOngoing) {
      statusColor = Colors.teal;
      statusText = 'Đang sử dụng';
      statusIcon = Icons.timelapse;
    } else {
      statusColor = AppColors.brandColor;
      statusText = status == 'PROCESSING' ? 'Chờ xác nhận' : 'Sắp tới';
      statusIcon = Icons.event_available;
    }

    return GestureDetector(
      onTap: isActive ? () => _showActionDialog(booking) : null,
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.05),
              blurRadius: 10,
              offset: const Offset(0, 4),
            ),
          ],
          border: Border(left: BorderSide(color: statusColor, width: 4)),
        ),
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        booking['zoneName'],
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 4),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 2,
                        ),
                        decoration: BoxDecoration(
                          color: Colors.grey[100],
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          'Ghế: ${booking['seatCode']}',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: Colors.grey[700],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 6,
                  ),
                  decoration: BoxDecoration(
                    color: statusColor.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(statusIcon, size: 14, color: statusColor),
                      const SizedBox(width: 4),
                      Text(
                        statusText,
                        style: TextStyle(
                          color: statusColor,
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 15),
            const Divider(height: 1),
            const SizedBox(height: 15),
            Row(
              children: [
                Expanded(
                  child: Row(
                    children: [
                      Icon(
                        Icons.calendar_today_outlined,
                        size: 16,
                        color: Colors.grey[400],
                      ),
                      const SizedBox(width: 8),
                      Text(
                        booking['date'],
                        style: TextStyle(color: Colors.grey[700], fontSize: 13),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: Row(
                    children: [
                      Icon(
                        Icons.access_time,
                        size: 16,
                        color: Colors.grey[400],
                      ),
                      const SizedBox(width: 8),
                      Text(
                        booking['time'],
                        style: TextStyle(
                          color: Colors.grey[700],
                          fontSize: 13,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
                if (isActive)
                  Icon(Icons.chevron_right, color: Colors.grey[400]),
              ],
            ),
            if (hasLayoutWarning) ...[
              const SizedBox(height: 10),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 10,
                ),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFF7ED),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: const Color(0xFFFDBA74)),
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(
                      Icons.warning_amber_rounded,
                      size: 16,
                      color: Color(0xFFEA580C),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        (booking['layoutChangeMessage']?.toString().trim().isNotEmpty ?? false)
                            ? booking['layoutChangeMessage'].toString().trim()
                            : 'Sơ đồ thư viện vừa thay đổi. Bạn có thể mở lịch này để đổi ghế hoặc hủy mà không bị giới hạn thời hạn hủy tiêu chuẩn.',
                        style: const TextStyle(
                          fontSize: 12,
                          color: Color(0xFF9A3412),
                          height: 1.45,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
            if (status == 'COMPLETED') ...[
              const SizedBox(height: 10),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 10,
                ),
                decoration: BoxDecoration(
                  color: actualEndTime != null
                      ? const Color(0xFFE8F5E9)
                      : const Color(0xFFF3F4F6),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  children: [
                    Icon(
                      actualEndTime != null
                          ? Icons.logout_rounded
                          : Icons.schedule_rounded,
                      size: 16,
                      color: actualEndTime != null
                          ? Colors.green
                          : Colors.grey[700],
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        actualEndTime != null
                            ? 'Đã rời ghế lúc ${DateFormat('HH:mm').format(actualEndTime)}'
                            : 'Phiên học kết thúc tự động khi hết giờ',
                        style: TextStyle(
                          fontSize: 12,
                          color: actualEndTime != null
                              ? Colors.green[800]
                              : Colors.grey[700],
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
            if ((status == 'CANCEL' || status == 'CANCELLED') &&
                cancellationReason != null &&
                cancellationReason.trim().isNotEmpty) ...[
              const SizedBox(height: 10),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 10,
                ),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFF1F2),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: const Color(0xFFFECACA)),
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(
                      Icons.info_outline_rounded,
                      size: 16,
                      color: Color(0xFFDC2626),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: RichText(
                        text: TextSpan(
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.red[800],
                            height: 1.5,
                          ),
                          children: [
                            TextSpan(
                              text: cancelledByStaff
                                  ? 'Lý do thủ thư huỷ: '
                                  : 'Lý do huỷ: ',
                              style: const TextStyle(
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            TextSpan(text: cancellationReason.trim()),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
            const SizedBox(height: 10),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                IconButton(
                  onPressed: () => _hideBooking(booking),
                  icon: Icon(
                    Icons.visibility_off_outlined,
                    size: 20,
                    color: Colors.grey[700],
                  ),
                  tooltip: 'Ẩn khỏi danh sách',
                  visualDensity: VisualDensity.compact,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
