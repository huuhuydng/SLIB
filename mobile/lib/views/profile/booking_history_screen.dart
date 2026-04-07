import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/core/constants/api_constants.dart';
import 'package:slib/models/upcoming_booking.dart';
import 'package:slib/services/auth/auth_service.dart';
import 'package:slib/views/home/widgets/booking_action_dialog.dart';
import 'package:slib/views/profile/widgets/history_summary_card.dart';
import 'package:slib/views/widgets/error_display_widget.dart';

class BookingHistoryScreen extends StatefulWidget {
  final int initialTab;
  const BookingHistoryScreen({super.key, this.initialTab = 0});

  @override
  State<BookingHistoryScreen> createState() => _BookingHistoryScreenState();
}

class _BookingHistoryScreenState extends State<BookingHistoryScreen> {
  List<Map<String, dynamic>> _activeBookings = [];
  List<Map<String, dynamic>> _completedBookings = [];
  List<Map<String, dynamic>> _cancelledBookings = [];

  bool _isLoading = true;
  String? _errorMessage;

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

    try {
      final url = Uri.parse("${ApiConstants.bookingUrl}/user/${user.id}");
      final response = await authService.authenticatedRequest('GET', url);

      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(utf8.decode(response.bodyBytes));
        final now = DateTime.now();

        _activeBookings = [];
        _completedBookings = [];
        _cancelledBookings = [];

        for (var booking in data) {
          final status = (booking['status'] ?? '').toString().toUpperCase();
          final endTime = DateTime.parse(booking['endTime']);
          final startTime = DateTime.parse(booking['startTime']);

          final parsedBooking = {
            'reservationId': booking['reservationId'],
            'seatCode': booking['seatCode'] ?? 'N/A',
            'zoneName': booking['zoneName'] ?? 'N/A',
            'areaName': booking['areaName'] ?? 'N/A',
            'startTime': startTime,
            'endTime': endTime,
            'status': status,
            'date': DateFormat('dd/MM/yyyy').format(startTime),
            'time':
                '${DateFormat('HH:mm').format(startTime)} - ${DateFormat('HH:mm').format(endTime)}',
          };

          if (status == 'CANCEL' || status == 'CANCELLED') {
            _cancelledBookings.add(parsedBooking);
          } else if (status == 'EXPIRED') {
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
      seatId: 0,
      seatCode: booking['seatCode'],
      zoneId: 0,
      zoneName: booking['zoneName'],
      areaId: 0,
      areaName: booking['areaName'],
      startTime: booking['startTime'],
      endTime: booking['endTime'],
      dayOfWeek: '',
      dayOfMonth: (booking['startTime'] as DateTime).day,
      timeRange: booking['time'],
    );

    BookingActionDialog.show(context, upcomingBooking, () {
      _loadBookings();
    });
  }

  Widget _buildErrorWidget() {
    if (_errorMessage == 'auth') {
      return ErrorDisplayWidget.auth(onRetry: _loadBookings);
    }
    return ErrorDisplayWidget(message: _errorMessage!, onRetry: _loadBookings);
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
            "Lịch sử đặt chỗ",
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
            tabs: [
              _buildTabLabel("Sắp tới", _activeBookings.length),
              _buildTabLabel("Hoàn thành", _completedBookings.length),
              _buildTabLabel("Đã huỷ", _cancelledBookings.length),
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
                    isActive: true,
                    summary: _buildUpcomingSummaryHeader(),
                  ),
                  _buildBookingList(
                    _completedBookings,
                    summary: _buildCompletedSummaryHeader(),
                  ),
                  _buildBookingList(
                    _cancelledBookings,
                    summary: _buildCancelledSummaryHeader(),
                  ),
                ],
              ),
      ),
    );
  }

  Widget _buildBookingList(
    List<Map<String, dynamic>> bookings, {
    bool isActive = false,
    required Widget summary,
  }) {
    return RefreshIndicator(
      color: AppColors.brandColor,
      onRefresh: _loadBookings,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.only(bottom: 24),
        children: [
          summary,
          if (bookings.isEmpty)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
              child: ErrorDisplayWidget.empty(message: 'Không có dữ liệu'),
            )
          else
            ...bookings.map(
              (booking) => Padding(
                padding: const EdgeInsets.fromLTRB(20, 0, 20, 15),
                child: _buildBookingCard(booking, isActive: isActive),
              ),
            ),
        ],
      ),
    );
  }

  Tab _buildTabLabel(String title, int count) {
    return Tab(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(title),
          const SizedBox(width: 6),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
            decoration: BoxDecoration(
              color: AppColors.brandColor.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Text(
              '$count',
              style: const TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w700,
                color: AppColors.brandColor,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildUpcomingSummaryHeader() {
    final now = DateTime.now();
    final ongoingCount = _activeBookings.where((booking) {
      final startTime = booking['startTime'] as DateTime;
      final endTime = booking['endTime'] as DateTime;
      return now.isAfter(startTime) && now.isBefore(endTime);
    }).length;
    final upcomingCount = _activeBookings.length - ongoingCount;

    return HistorySummaryCard(
      title: 'Tổng quan đặt chỗ sắp tới',
      subtitle: _activeBookings.isEmpty
          ? 'Bạn chưa có lịch đặt chỗ nào đang hoạt động.'
          : 'Các lịch đặt chỗ còn hiệu lực hoặc sắp diễn ra của bạn.',
      icon: Icons.event_available_rounded,
      gradientColors: const [Color(0xFFFF7A18), Color(0xFFFFA34D)],
      metrics: [
        HistorySummaryMetric(
          icon: Icons.list_alt_rounded,
          value: '${_activeBookings.length}',
          label: 'Tổng lịch sử',
        ),
        HistorySummaryMetric(
          icon: Icons.timelapse_rounded,
          value: '$ongoingCount',
          label: 'Đang sử dụng',
        ),
        HistorySummaryMetric(
          icon: Icons.schedule_rounded,
          value: '$upcomingCount',
          label: 'Sắp tới',
        ),
      ],
    );
  }

  Widget _buildCompletedSummaryHeader() {
    final totalHours = _completedBookings.fold<double>(0, (sum, booking) {
      final startTime = booking['startTime'] as DateTime;
      final endTime = booking['endTime'] as DateTime;
      return sum + endTime.difference(startTime).inMinutes / 60;
    });
    final thisMonthCount = _completedBookings.where((booking) {
      final startTime = booking['startTime'] as DateTime;
      final now = DateTime.now();
      return startTime.year == now.year && startTime.month == now.month;
    }).length;

    return HistorySummaryCard(
      title: 'Tổng quan lịch sử hoàn thành',
      subtitle: _completedBookings.isEmpty
          ? 'Bạn chưa có lượt đặt chỗ nào hoàn thành.'
          : 'Những lượt học tập đã kết thúc thành công của bạn.',
      icon: Icons.task_alt_rounded,
      gradientColors: const [Color(0xFF2E7D32), Color(0xFF66BB6A)],
      metrics: [
        HistorySummaryMetric(
          icon: Icons.list_alt_rounded,
          value: '${_completedBookings.length}',
          label: 'Tổng lịch sử',
        ),
        HistorySummaryMetric(
          icon: Icons.access_time_filled_rounded,
          value: totalHours.toStringAsFixed(1),
          label: 'Tổng giờ',
        ),
        HistorySummaryMetric(
          icon: Icons.calendar_month_rounded,
          value: '$thisMonthCount',
          label: 'Trong tháng này',
        ),
      ],
    );
  }

  Widget _buildCancelledSummaryHeader() {
    final cancelledCount = _cancelledBookings.where((booking) {
      final status = booking['status'] as String;
      return status == 'CANCEL' || status == 'CANCELLED';
    }).length;
    final expiredCount = _cancelledBookings.where((booking) {
      final status = booking['status'] as String;
      return status == 'EXPIRED';
    }).length;

    return HistorySummaryCard(
      title: 'Tổng quan lịch sử không thành công',
      subtitle: _cancelledBookings.isEmpty
          ? 'Bạn chưa có lịch đặt chỗ bị hủy hoặc hết hạn.'
          : 'Bao gồm các lượt đã hủy và các lượt không đến nhận chỗ.',
      icon: Icons.event_busy_rounded,
      gradientColors: const [Color(0xFFD84315), Color(0xFFFF8A65)],
      metrics: [
        HistorySummaryMetric(
          icon: Icons.list_alt_rounded,
          value: '${_cancelledBookings.length}',
          label: 'Tổng lịch sử',
        ),
        HistorySummaryMetric(
          icon: Icons.cancel_outlined,
          value: '$cancelledCount',
          label: 'Đã hủy',
        ),
        HistorySummaryMetric(
          icon: Icons.warning_amber_rounded,
          value: '$expiredCount',
          label: 'Không đến',
        ),
      ],
    );
  }

  Widget _buildBookingCard(
    Map<String, dynamic> booking, {
    bool isActive = false,
  }) {
    final status = booking['status'] as String;
    final startTime = booking['startTime'] as DateTime;
    final endTime = booking['endTime'] as DateTime;
    final now = DateTime.now();

    Color statusColor;
    String statusText;
    IconData statusIcon;
    bool isOngoing = now.isAfter(startTime) && now.isBefore(endTime);

    if (status == 'CANCEL' || status == 'CANCELLED') {
      statusColor = Colors.red;
      statusText = "Đã huỷ";
      statusIcon = Icons.cancel_outlined;
    } else if (status == 'EXPIRED') {
      statusColor = Colors.orange;
      statusText = "Không đến";
      statusIcon = Icons.warning_amber_rounded;
    } else if (status == 'COMPLETED' || endTime.isBefore(now)) {
      statusColor = Colors.green;
      statusText = "Hoàn thành";
      statusIcon = Icons.check_circle;
    } else if (isOngoing) {
      statusColor = Colors.teal;
      statusText = "Đang sử dụng";
      statusIcon = Icons.timelapse;
    } else {
      statusColor = AppColors.brandColor;
      statusText = status == 'PROCESSING' ? "Chờ xác nhận" : "Sắp tới";
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
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
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
                          "Ghế: ${booking['seatCode']}",
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
          ],
        ),
      ),
    );
  }
}
