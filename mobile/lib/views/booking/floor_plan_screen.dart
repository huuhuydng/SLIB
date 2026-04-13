import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:slib/assets/colors.dart';
import 'package:slib/models/amenity.dart';
import 'package:slib/models/area.dart';
import 'package:slib/models/area_factory.dart';
import 'package:slib/models/library_setting.dart';
import 'package:slib/models/seat.dart';
import 'package:slib/models/upcoming_booking.dart';
import 'package:slib/models/zones.dart';
import 'package:slib/services/auth/auth_service.dart';
import 'package:slib/services/booking/booking_service.dart';
import 'package:slib/services/seat/seat_websocket_service.dart';
import 'package:slib/views/home/widgets/booking_confirm_screen.dart';
import 'package:slib/views/widgets/error_display_widget.dart';
import 'package:intl/intl.dart';

/// Màn hình sơ đồ mặt bằng thư viện
class FloorPlanScreen extends StatefulWidget {
  final int? initialZoneId;
  final int? initialSeatId;
  final DateTime? initialDate;
  final String? initialTimeSlot;
  final String? replacementReservationId;

  const FloorPlanScreen({
    super.key,
    this.initialZoneId,
    this.initialSeatId,
    this.initialDate,
    this.initialTimeSlot,
    this.replacementReservationId,
  });

  @override
  State<FloorPlanScreen> createState() => _FloorPlanScreenState();
}

class _FloorPlanScreenState extends State<FloorPlanScreen> {
  final BookingService _bookingService = BookingService();
  bool get _replacementMode => widget.replacementReservationId != null;

  List<Area> _areas = [];
  List<Zone> _zones = [];
  List<AreaFactory> _factories = [];
  Map<int, double> _zoneOccupancy = {};
  Map<int, List<Seat>> _zoneSeats = {}; // Ghế theo zone

  Area? _selectedArea;
  bool _isLoading = true;
  String? _errorMessage;

  // Date and Time Slot Filter
  DateTime _selectedDate = DateTime.now();
  String? _selectedTimeSlot;
  List<TimeSlot> _timeSlots = [];
  LibrarySetting? _librarySettings;

  // Calculated dimensions
  double _contentWidth = 300;
  double _contentHeight = 400;

  // === PERFORMANCE OPTIMIZATION ===
  // Cache: key = "date|start-end" (e.g. "2026-01-20|14:00-15:00")
  final Map<String, Map<int, List<Seat>>> _seatsCache = {};
  // Debounce timer
  Timer? _debounceTimer;
  // Đang fetch seats?
  bool _isFetchingSeats = false;

  // Auto-select seat from AI recommendation
  bool _didAutoSelectSeat = false;

  // Non-working day check
  bool _isNonWorkingDay = false;

  // Library locked check
  bool _isLibraryClosed = false;
  String? _closedReason;
  UpcomingBooking? _currentUserUpcomingBooking;

  @override
  void initState() {
    super.initState();
    if (widget.initialDate != null) {
      _selectedDate = widget.initialDate!;
    }
    if (widget.initialTimeSlot != null && widget.initialTimeSlot!.trim().isNotEmpty) {
      _selectedTimeSlot = widget.initialTimeSlot;
    }
    _loadCurrentUserUpcomingBooking();
    _loadData();
    _setupWebSocket();
  }

  bool _isSameCalendarDay(DateTime left, DateTime right) {
    return left.year == right.year &&
        left.month == right.month &&
        left.day == right.day;
  }

  bool _isCurrentUserBookedSeat(Seat seat) {
    final booking = _currentUserUpcomingBooking;
    if (booking == null || _selectedTimeSlot == null) {
      return false;
    }

    return booking.seatId == seat.seatId &&
        _isSameCalendarDay(booking.startTime, _selectedDate) &&
        booking.timeRange.trim() == _selectedTimeSlot!.trim();
  }

  Color _seatFillColor(Seat seat) {
    if (seat.isUnavailable || seat.seatStatus == 'MAINTENANCE') {
      return Colors.grey[400]!;
    }
    if (_isCurrentUserBookedSeat(seat)) {
      return AppColors.brandColor;
    }
    if (seat.seatStatus == 'AVAILABLE') {
      return Colors.green;
    }
    return Colors.red[400]!;
  }

  Color _seatBorderColor(Seat seat) {
    if (seat.isUnavailable || seat.seatStatus == 'MAINTENANCE') {
      return Colors.grey[600]!;
    }
    if (_isCurrentUserBookedSeat(seat)) {
      return const Color(0xFFE85A00);
    }
    if (seat.seatStatus == 'AVAILABLE') {
      return Colors.green[700]!;
    }
    return Colors.red[700]!;
  }

  String _seatTooltipLabel(Seat seat) {
    if (seat.isUnavailable || seat.seatStatus == 'MAINTENANCE') {
      return '${seat.seatCode} - Ghế đang bị hạn chế';
    }
    if (_isCurrentUserBookedSeat(seat)) {
      return '${seat.seatCode} - Ghế của bạn';
    }
    if (seat.seatStatus == 'AVAILABLE') {
      return '${seat.seatCode} - Trống';
    }
    return '${seat.seatCode} - Đã được người khác đặt';
  }

  String _seatBlockedMessage(Seat seat) {
    if (seat.isUnavailable || seat.seatStatus == 'MAINTENANCE') {
      return 'Ghế ${seat.seatCode} hiện đang bị hạn chế sử dụng';
    }
    if (_isCurrentUserBookedSeat(seat)) {
      return 'Ghế ${seat.seatCode} là ghế anh đã đặt trong khung giờ này';
    }
    if (seat.seatStatus == 'HOLDING') {
      return 'Ghế ${seat.seatCode} đang được người khác chọn';
    }
    if (seat.seatStatus == 'CONFIRMED') {
      return 'Ghế ${seat.seatCode} đang có người sử dụng';
    }
    return 'Ghế ${seat.seatCode} đã được người khác đặt';
  }

  Future<void> _loadCurrentUserUpcomingBooking() async {
    final authService = Provider.of<AuthService>(context, listen: false);
    final userId = authService.currentUser?.id;
    if (userId == null) {
      if (mounted) {
        setState(() => _currentUserUpcomingBooking = null);
      }
      return;
    }

    try {
      final data = await _bookingService.getUpcomingBooking(userId);
      if (!mounted) return;

      UpcomingBooking? booking;
      if (data is Map<String, dynamic>) {
        booking = UpcomingBooking.fromJson(data);
      }

      setState(() => _currentUserUpcomingBooking = booking);
    } catch (e) {
      debugPrint('Không thể tải upcoming booking hiện tại: $e');
    }
  }

  /// Setup WebSocket for real-time seat updates
  void _setupWebSocket() {
    seatWebSocketService.addListener(_onSeatUpdate);
    seatWebSocketService.connect();
  }

  /// Handle real-time seat updates from WebSocket
  void _onSeatUpdate(Map<String, dynamic> data) {
    final seatId = data['seatId'] as int?;
    final zoneId = data['zoneId'] as int?;
    final status = data['status'] as String?;

    if (seatId == null || zoneId == null || status == null) return;

    // Optimistic update: cập nhật local state NGAY LẬP TỨC (0ms delay)
    setState(() {
      for (final entry in _zoneSeats.entries) {
        final seats = entry.value;
        final idx = seats.indexWhere((s) => s.seatId == seatId);
        if (idx != -1) {
          final old = seats[idx];
          seats[idx] = Seat(
            seatId: old.seatId,
            zoneId: old.zoneId,
            seatCode: old.seatCode,
            seatStatus: status == 'AVAILABLE'
                ? 'AVAILABLE'
                : status == 'HOLDING'
                ? 'HOLDING'
                : status == 'CONFIRMED'
                ? 'CONFIRMED'
                : 'BOOKED',
            rowNumber: old.rowNumber,
            columnNumber: old.columnNumber,
            isActive: old.isActive,
            reservationEndTime: data['endTime'] as String?,
          );
          break;
        }
      }
    });

    // Fetch API background để đồng bộ data chính xác + schedule timer mới
    _clearSeatsCache();
    _forceRefreshSeats().then((_) => _scheduleExpirationRefresh());
  }

  String _getVietnameseDayName(DateTime date) {
    const dayNames = [
      'Chủ Nhật',
      'Thứ Hai',
      'Thứ Ba',
      'Thứ Tư',
      'Thứ Năm',
      'Thứ Sáu',
      'Thứ Bảy',
    ];
    return dayNames[date.weekday % 7];
  }

  /// Tính ngày cuối cùng có thể đặt - chỉ tính ngày làm việc
  /// VD: maxBookingDays = 3, hôm nay thứ 7 -> chỉ đếm T2, T3, T4 = 3 ngày làm việc
  DateTime _getLastBookableDate(int maxWorkingDays) {
    if (_librarySettings == null) {
      return DateTime.now().add(Duration(days: maxWorkingDays));
    }

    DateTime currentDate = DateTime.now();
    int workingDaysCount = 0;

    // Đếm đủ số ngày làm việc
    while (workingDaysCount < maxWorkingDays) {
      currentDate = currentDate.add(const Duration(days: 1));
      if (_librarySettings!.isWorkingDay(currentDate)) {
        workingDaysCount++;
      }
    }

    return currentDate;
  }

  Future<void> _loadData() async {
    try {
      setState(() {
        _isLoading = true;
        _errorMessage = null;
      });

      // Load areas, settings, and time slots in parallel
      final results = await Future.wait([
        _bookingService.getAllAreas(),
        _bookingService.getLibrarySettings(),
        _bookingService.getTimeSlots(),
      ]);

      final areas = results[0] as List<Area>;
      final settings = results[1] as LibrarySetting;
      final allTimeSlots = results[2] as List<TimeSlot>;

      // Check if selected date is a working day
      final isWorkingDay = settings.isWorkingDay(_selectedDate);

      // Filter time slots: nếu là hôm nay, chỉ hiển thị slot từ giờ hiện tại trở đi
      final filteredSlots = isWorkingDay
          ? _filterTimeSlotsForDate(_selectedDate, allTimeSlots)
          : <TimeSlot>[];

      // Giữ nguyên slot đang chọn nếu còn valid, nếu không thì chọn slot mặc định
      String? newTimeSlot = _selectedTimeSlot;
      if (newTimeSlot != null) {
        // Kiểm tra slot cũ còn trong danh sách valid không
        final isStillValid = filteredSlots.any((s) => s.label == newTimeSlot);
        if (!isStillValid) {
          // Slot cũ không còn valid (đã qua), chọn slot mới
          newTimeSlot = _findCurrentSlot(filteredSlots);
        }
      } else {
        // Chưa có slot nào, chọn slot mặc định
        newTimeSlot = _findCurrentSlot(filteredSlots);
      }

      final activeAreas = areas.where((a) => a.isActive).toList();

      // Nếu có initialZoneId, tìm area chứa zone đó
      Area? targetArea;
      if (widget.initialZoneId != null && activeAreas.isNotEmpty) {
        targetArea = await _findAreaForZone(widget.initialZoneId!, activeAreas);
      }

      setState(() {
        _areas = activeAreas;
        _librarySettings = settings;
        _timeSlots = allTimeSlots; // Giữ tất cả slots gốc
        _selectedTimeSlot = newTimeSlot;
        _isLoading = false;
        _isNonWorkingDay = !isWorkingDay;
        _isLibraryClosed = settings.libraryClosed;
        _closedReason = settings.closedReason;

        // Kiểm tra ngày làm việc trước
        if (!isWorkingDay) {
          _errorMessage = null; // Sẽ hiển thị màn hình thư viện đóng cửa riêng
        }
        // Nếu không có slot valid cho hôm nay (hết giờ làm việc), chỉ thông báo nhưng vẫn cho chọn ngày khác
        else if (filteredSlots.isEmpty && allTimeSlots.isNotEmpty) {
          _errorMessage =
              'Thư viện đã đóng cửa hôm nay. Vui lòng chọn ngày khác phía trên.';
        }

        // Vẫn load areas để user có thể chọn ngày khác
        if (_areas.isNotEmpty) {
          _selectedArea = targetArea ?? _areas.first;
          // Chỉ load zones/seats nếu có time slot hợp lệ
          if (newTimeSlot != null) {
            _loadZonesAndFactories();
          }
        }
      });

      // Prefetch seats cho slot hiện tại và slot tiếp theo
      _prefetchSeats(filteredSlots);

      // Start real-time refresh timer (every 30 seconds)
      _startAutoRefresh();
    } catch (e) {
      setState(() {
        _isLoading = false;
        _errorMessage = ErrorDisplayWidget.toVietnamese(e);
      });
    }
  }

  /// Filter time slots - bỏ các slot đã qua nếu là ngày hôm nay
  List<TimeSlot> _filterTimeSlotsForDate(
    DateTime date,
    List<TimeSlot> allSlots,
  ) {
    final now = DateTime.now();
    final isToday =
        date.year == now.year && date.month == now.month && date.day == now.day;

    if (!isToday) return allSlots;

    final currentHour = now.hour;
    final currentMinute = now.minute;

    return allSlots.where((slot) {
      // Parse end time "08:00" -> 8
      final endParts = slot.endTime.split(':');
      final endHour = int.tryParse(endParts[0]) ?? 0;
      final endMinute =
          int.tryParse(endParts.length > 1 ? endParts[1] : '0') ?? 0;

      // Slot còn valid nếu endTime > now (slot vẫn đang diễn ra hoặc chưa bắt đầu)
      if (endHour > currentHour) return true;
      if (endHour == currentHour && endMinute > currentMinute) return true;
      return false;
    }).toList();
  }

  /// Tìm slot phù hợp với thời gian - nếu là hôm nay dùng giờ hiện tại, ngày khác dùng slot đầu tiên
  String? _findCurrentSlot(List<TimeSlot> slots, {DateTime? forDate}) {
    if (slots.isEmpty) return null;

    final now = DateTime.now();
    final date = forDate ?? _selectedDate;
    final isToday =
        date.year == now.year && date.month == now.month && date.day == now.day;

    // Nếu không phải hôm nay, trả về slot đầu tiên (giờ mở cửa)
    if (!isToday) {
      return slots.first.label;
    }

    // Nếu là hôm nay, tìm slot chứa hoặc gần giờ hiện tại
    final currentHour = now.hour;

    for (final slot in slots) {
      final startParts = slot.startTime.split(':');
      final startHour = int.tryParse(startParts[0]) ?? 0;
      if (startHour >= currentHour) {
        return slot.label;
      }
    }

    return slots.first.label;
  }

  /// Timer for smart seat expiration
  Timer? _refreshTimer;
  Timer? _expirationTimer;

  void _startAutoRefresh() {
    _refreshTimer?.cancel();
    // Smart timer cho expiration chính xác
    _scheduleExpirationRefresh();
    // Fallback polling 30s — chỉ dùng khi WebSocket mất kết nối
    _refreshTimer = Timer.periodic(const Duration(seconds: 30), (_) {
      if (!mounted || seatWebSocketService.isConnected) {
        return;
      }

      _forceRefreshSeats().then((_) => _scheduleExpirationRefresh());
    });
  }

  /// Smart timer: tìm endTime gần nhất trong _zoneSeats → set Timer re-fetch đúng lúc
  void _scheduleExpirationRefresh() {
    _expirationTimer?.cancel();

    DateTime? nearestEnd;
    final now = DateTime.now();

    for (final seats in _zoneSeats.values) {
      for (final seat in seats) {
        if (seat.reservationEndTime != null &&
            (seat.seatStatus == 'BOOKED' ||
                seat.seatStatus == 'CONFIRMED' ||
                seat.seatStatus == 'HOLDING')) {
          try {
            final endTime = DateTime.parse(seat.reservationEndTime!);
            if (endTime.isAfter(now)) {
              if (nearestEnd == null || endTime.isBefore(nearestEnd)) {
                nearestEnd = endTime;
              }
            }
          } catch (_) {}
        }
      }
    }

    if (nearestEnd != null) {
      final delay =
          nearestEnd.difference(now) + const Duration(milliseconds: 100);
      debugPrint(
        'Smart timer: re-fetch trong ${delay.inSeconds}s lúc $nearestEnd',
      );
      _expirationTimer = Timer(delay, () {
        if (mounted) {
          _forceRefreshSeats().then((_) {
            // Sau khi refresh, schedule timer tiếp cho endTime kế tiếp
            _scheduleExpirationRefresh();
          });
        }
      });
    }
  }

  /// Force refresh seats từ API, bypass cache/debounce hoàn toàn
  Future<void> _forceRefreshSeats() async {
    if (_selectedArea == null || _selectedTimeSlot == null) return;

    try {
      final parts = _selectedTimeSlot!.split(' - ');
      final zoneSeats = await _bookingService.getAllSeatsByArea(
        _selectedArea!.areaId,
        _selectedDate,
        parts[0],
        parts[1],
      );

      if (mounted) {
        // Cập nhật cache và state
        final dateStr = DateFormat('yyyy-MM-dd').format(_selectedDate);
        final cacheKey = '$dateStr|${parts[0]}-${parts[1]}';
        _seatsCache[cacheKey] = zoneSeats;

        setState(() {
          _zoneSeats = zoneSeats;
        });
      }
    } catch (e) {
      debugPrint('Auto-refresh error: $e');
    }
  }

  /// Tìm area chứa zone theo zoneId
  Future<Area?> _findAreaForZone(int zoneId, List<Area> areas) async {
    for (final area in areas) {
      try {
        final zones = await _bookingService.getZonesByArea(area.areaId);
        if (zones.any((z) => z.zoneId == zoneId)) {
          return area;
        }
      } catch (_) {}
    }
    return null;
  }

  @override
  void dispose() {
    _refreshTimer?.cancel();
    _expirationTimer?.cancel();
    _debounceTimer?.cancel();
    seatWebSocketService.removeListener(_onSeatUpdate);
    super.dispose();
  }

  Future<void> _loadZonesAndFactories() async {
    if (_selectedArea == null) return;

    try {
      // Load zones and factories first (critical)
      final results = await Future.wait([
        _bookingService.getFactoriesByArea(_selectedArea!.areaId),
        _bookingService.getZonesByArea(_selectedArea!.areaId),
      ]);

      final factories = results[0] as List<AreaFactory>;
      final zones = results[1] as List<Zone>;

      // Load occupancy separately (non-critical, don't block seats)
      Map<int, double> occupancy = {};
      try {
        final zoneOccupancies = await _bookingService.getZoneOccupancy(
          _selectedArea!.areaId,
        );
        for (final zo in zoneOccupancies) {
          occupancy[zo.zoneId] = zo.occupancyRate * 100;
        }
      } catch (e) {
        debugPrint('Warning: Could not load zone occupancy: $e');
        // Continue without occupancy data
      }

      // Load seats for each zone - use getSeatsByTime if date+slot selected
      Map<int, List<Seat>> zoneSeats = {};
      final seatFutures = zones.map((zone) async {
        try {
          List<Seat> seats;
          if (_selectedTimeSlot != null) {
            // Parse time slot: "07:00 - 08:00" -> start="07:00", end="08:00"
            final parts = _selectedTimeSlot!.split(' - ');
            seats = await _bookingService.getSeatsByTime(
              zone.zoneId,
              _selectedDate,
              parts[0],
              parts[1],
            );
          } else {
            seats = await _bookingService.getSeats(zone.zoneId);
          }
          return MapEntry(zone.zoneId, seats);
        } catch (e) {
          debugPrint('Zone ${zone.zoneId}: Error loading seats: $e');
          return MapEntry(zone.zoneId, <Seat>[]);
        }
      });
      final seatResults = await Future.wait(seatFutures);
      for (final entry in seatResults) {
        zoneSeats[entry.key] = entry.value;
      }

      // Calculate content dimensions based on zones and factories
      _calculateContentSize(zones, factories);

      setState(() {
        _zones = zones;
        _factories = factories;
        _zoneOccupancy = occupancy;
        _zoneSeats = zoneSeats;
      });

      // Auto-select seat từ AI recommendation
      _tryAutoSelectSeat(zones, zoneSeats);
    } catch (e) {
      debugPrint('Error loading zones/factories: $e');
    }
  }

  /// Tự động chọn ghế được AI gợi ý
  void _tryAutoSelectSeat(List<Zone> zones, Map<int, List<Seat>> zoneSeats) {
    if (_didAutoSelectSeat || widget.initialSeatId == null) return;

    // Kiểm tra cấu hình AI recommendation
    final authService = Provider.of<AuthService>(context, listen: false);
    final aiEnabled = authService.currentSetting?.isAiRecommendEnabled ?? true;
    if (!aiEnabled) return;

    _didAutoSelectSeat = true;

    for (final zone in zones) {
      final seats = zoneSeats[zone.zoneId] ?? [];
      final seat = seats.cast<Seat?>().firstWhere(
        (s) => s!.seatId == widget.initialSeatId,
        orElse: () => null,
      );
      if (seat != null) {
        // Delay 1 frame để UI render xong rồi mới show popup
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) _onSeatTap(seat, zone);
        });
        return;
      }
    }
  }

  /// Chỉ reload seats - tối ưu với cache, debounce, và 1 API call
  void _loadSeatsOnly() {
    // Cancel previous debounce timer
    _debounceTimer?.cancel();

    // Debounce 300ms - tránh spam API khi click nhanh
    _debounceTimer = Timer(const Duration(milliseconds: 300), () {
      _fetchSeatsWithCache();
    });
  }

  /// Fetch seats với cache
  Future<void> _fetchSeatsWithCache() async {
    if (_selectedArea == null ||
        _selectedTimeSlot == null ||
        _isFetchingSeats) {
      return;
    }

    final parts = _selectedTimeSlot!.split(' - ');
    final dateStr = DateFormat('yyyy-MM-dd').format(_selectedDate);
    final cacheKey = '$dateStr|${parts[0]}-${parts[1]}';

    // Check cache trước
    if (_seatsCache.containsKey(cacheKey)) {
      debugPrint('🚀 Using cached seats for $cacheKey');
      setState(() {
        _zoneSeats = _seatsCache[cacheKey]!;
      });
      return;
    }

    // Không có cache, gọi API
    setState(() => _isFetchingSeats = true);

    try {
      debugPrint('📡 Fetching seats for $cacheKey');
      // Gọi 1 API thay vì N API
      final zoneSeats = await _bookingService.getAllSeatsByArea(
        _selectedArea!.areaId,
        _selectedDate,
        parts[0],
        parts[1],
      );

      // Lưu vào cache
      _seatsCache[cacheKey] = zoneSeats;

      if (mounted) {
        setState(() {
          _zoneSeats = zoneSeats;
          _isFetchingSeats = false;
        });
      }
    } catch (e) {
      debugPrint('Error loading seats: $e');
      if (mounted) setState(() => _isFetchingSeats = false);
    }
  }

  /// Clear cache khi booking thay đổi
  void _clearSeatsCache() {
    _seatsCache.clear();
    debugPrint('🗑️ Seats cache cleared');
  }

  /// Prefetch seats cho slot hiện tại và slot tiếp theo để có data ngay khi chọn
  void _prefetchSeats(List<TimeSlot> filteredSlots) async {
    if (_selectedArea == null || filteredSlots.isEmpty) return;

    // Lấy tối đa 2 slot đầu tiên để prefetch
    final slotsToFetch = filteredSlots.take(2).toList();

    for (final slot in slotsToFetch) {
      final parts = slot.label.split(' - ');
      final dateStr = DateFormat('yyyy-MM-dd').format(_selectedDate);
      final cacheKey = '$dateStr|${parts[0]}-${parts[1]}';

      // Skip nếu đã có trong cache
      if (_seatsCache.containsKey(cacheKey)) continue;

      try {
        debugPrint('🔮 Prefetching seats for $cacheKey');
        final zoneSeats = await _bookingService.getAllSeatsByArea(
          _selectedArea!.areaId,
          _selectedDate,
          parts[0],
          parts[1],
        );
        _seatsCache[cacheKey] = zoneSeats;

        // Nếu đây là slot đang selected, cập nhật UI
        if (_selectedTimeSlot == slot.label && mounted) {
          setState(() {
            _zoneSeats = zoneSeats;
          });
        }
      } catch (e) {
        debugPrint('Error prefetching seats for $cacheKey: $e');
      }
    }
  }

  void _calculateContentSize(List<Zone> zones, List<AreaFactory> factories) {
    double maxX = 0;
    double maxY = 0;

    for (final zone in zones) {
      final right = zone.positionX + zone.width;
      final bottom = zone.positionY + zone.height;
      if (right > maxX) maxX = right.toDouble();
      if (bottom > maxY) maxY = bottom.toDouble();
    }

    for (final factory in factories) {
      final right = factory.positionX + factory.width;
      final bottom = factory.positionY + factory.height;
      if (right > maxX) maxX = right.toDouble();
      if (bottom > maxY) maxY = bottom.toDouble();
    }

    // Add padding
    _contentWidth = maxX + 40;
    _contentHeight = maxY + 40;
  }

  Color _getZoneColor(double occupancy) {
    if (occupancy >= 90) {
      return const Color(0xFFE74C3C); // Red
    } else if (occupancy >= 50) {
      return const Color(0xFFF39C12); // Orange
    } else {
      return const Color(0xFF27AE60); // Green
    }
  }

  String _getStatusText(double occupancy) {
    if (occupancy >= 90) {
      return 'Hết chỗ';
    } else if (occupancy >= 50) {
      return 'Khá đông';
    } else {
      return 'Trống';
    }
  }

  DateTime? _buildSlotDateTime(DateTime date, String timeValue) {
    final parts = timeValue.split(':');
    if (parts.length < 2) return null;

    final hour = int.tryParse(parts[0]);
    final minute = int.tryParse(parts[1]);
    if (hour == null || minute == null) return null;

    return DateTime(date.year, date.month, date.day, hour, minute);
  }

  int? _getRemainingMinutesForOngoingSlot(DateTime date, String? timeSlot) {
    if (timeSlot == null) return null;

    final now = DateTime.now();
    final isToday =
        date.year == now.year && date.month == now.month && date.day == now.day;
    if (!isToday) return null;

    final parts = timeSlot.split(' - ');
    if (parts.length != 2) return null;

    final slotStart = _buildSlotDateTime(date, parts[0]);
    final slotEnd = _buildSlotDateTime(date, parts[1]);
    if (slotStart == null || slotEnd == null) return null;

    if (now.isBefore(slotStart) || !now.isBefore(slotEnd)) return null;

    final remaining = slotEnd.difference(now);
    if (remaining.inSeconds <= 0) return null;

    return (remaining.inSeconds / 60).ceil();
  }

  Future<bool> _confirmOngoingTimeSlotIfNeeded() async {
    final remainingMinutes = _getRemainingMinutesForOngoingSlot(
      _selectedDate,
      _selectedTimeSlot,
    );

    if (remainingMinutes == null) return true;
    if (!mounted) return false;

    final remainingLabel = remainingMinutes <= 1
        ? 'chưa đến 1 phút'
        : '$remainingMinutes phút';

    final accepted = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
        title: const Text(
          'Khung giờ này đã bắt đầu',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        content: Text(
          'Suất ngồi bạn chọn đang diễn ra. Nếu tiếp tục đặt chỗ, bạn chỉ còn khoảng $remainingLabel sử dụng trong khung giờ ${_selectedTimeSlot!}. Bạn vẫn muốn tiếp tục chứ?',
          style: const TextStyle(height: 1.45),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Chọn khung giờ khác'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.brandColor,
              foregroundColor: Colors.white,
            ),
            child: const Text('Tiếp tục đặt'),
          ),
        ],
      ),
    );

    return accepted == true;
  }

  void _showLegendDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: const Color(0xFFFFF5EE),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text(
          'Chú thích bản đồ',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Khu vực:',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
            ),
            const SizedBox(height: 8),
            _buildLegendRow(const Color(0xFF27AE60), 'Vắng', 'Mật độ < 50%'),
            const SizedBox(height: 8),
            _buildLegendRow(
              const Color(0xFFF39C12),
              'Khá đông',
              'Mật độ 50-90%',
            ),
            const SizedBox(height: 8),
            _buildLegendRow(const Color(0xFFE74C3C), 'Đông', 'Mật độ > 90%'),
            const SizedBox(height: 8),
            _buildLegendRow(
              const Color(0xFFBDC3C7),
              'Vật cản',
              'Không đặt chỗ được',
            ),
            const Divider(height: 24),
            const Text(
              'Ghế:',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
            ),
            const SizedBox(height: 8),
            _buildLegendRow(Colors.green, 'Ghế trống', 'Có thể đặt'),
            const SizedBox(height: 8),
            _buildLegendRow(
              AppColors.brandColor,
              'Ghế của bạn',
              'Suất anh đã đặt trong khung giờ này',
            ),
            const SizedBox(height: 8),
            _buildLegendRow(Colors.red, 'Ghế đã đặt', 'Không thể đặt'),
            const SizedBox(height: 8),
            _buildLegendRow(Colors.grey, 'Ghế bị hạn chế', 'Tạm ngưng sử dụng'),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text('Đóng', style: TextStyle(color: AppColors.brandColor)),
          ),
        ],
      ),
    );
  }

  Widget _buildLegendRow(Color color, String label, String sublabel) {
    return Row(
      children: [
        Container(
          width: 24,
          height: 24,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(6),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: const TextStyle(fontWeight: FontWeight.w500)),
              if (sublabel.isNotEmpty)
                Text(
                  sublabel,
                  style: TextStyle(fontSize: 12, color: Colors.grey[600]),
                ),
            ],
          ),
        ),
      ],
    );
  }

  void _showZoneBottomSheet(Zone zone) async {
    final occupancy = _zoneOccupancy[zone.zoneId] ?? 0;
    final statusText = _getStatusText(occupancy);
    final statusColor = _getZoneColor(occupancy);
    final seats = _zoneSeats[zone.zoneId] ?? [];
    final availableSeats = seats
        .where((s) => s.seatStatus == 'AVAILABLE')
        .length;

    // Fetch amenities from API
    List<Amenity> amenities = [];
    try {
      amenities = await _bookingService.getAmenities(zone.zoneId);
    } catch (e) {
      debugPrint('Error loading amenities: $e');
    }

    if (!mounted) return;

    showModalBottomSheet(
      context: context,
      backgroundColor: const Color(0xFFFFF5EE),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header: tên zone + trạng thái
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    zone.zoneName,
                    style: const TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 6,
                  ),
                  decoration: BoxDecoration(
                    color: statusColor.withAlpha(30),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: statusColor),
                  ),
                  child: Text(
                    statusText,
                    style: TextStyle(
                      color: statusColor,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Mô tả
            if (zone.description != null && zone.description!.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(bottom: 16),
                child: Text(
                  zone.description!,
                  style: TextStyle(color: Colors.grey[700]),
                ),
              ),

            // Tiện ích
            if (amenities.isNotEmpty) ...[
              const Text(
                'Tiện ích:',
                style: TextStyle(fontSize: 14, color: Colors.grey),
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: amenities
                    .map((amenity) => _buildAmenityChipFromModel(amenity))
                    .toList(),
              ),
              const SizedBox(height: 16),
            ],

            // Thống kê ghế
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.grey[200]!),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  _buildStatItem(
                    'Tổng ghế',
                    seats.length.toString(),
                    Icons.event_seat,
                  ),
                  Container(width: 1, height: 40, color: Colors.grey[200]),
                  _buildStatItem(
                    'Còn trống',
                    availableSeats.toString(),
                    Icons.check_circle,
                    color: Colors.green,
                  ),
                  Container(width: 1, height: 40, color: Colors.grey[200]),
                  _buildStatItem(
                    'Đã đặt',
                    (seats.length - availableSeats).toString(),
                    Icons.block,
                    color: Colors.red,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // Hướng dẫn
            Center(
              child: Text(
                'Chạm vào ghế trên sơ đồ để đặt chỗ',
                style: TextStyle(color: Colors.grey[600], fontSize: 13),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAmenityChipFromModel(Amenity amenity) {
    // Map icon name to actual IconData
    IconData getIcon() {
      switch (amenity.iconName) {
        case 'wifi':
          return Icons.wifi;
        case 'power':
          return Icons.power;
        case 'ac_unit':
          return Icons.ac_unit;
        case 'light':
          return Icons.light;
        case 'air':
          return Icons.air;
        case 'water_drop':
          return Icons.water_drop;
        case 'volume_off':
          return Icons.volume_off;
        default:
          return Icons.check_circle;
      }
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.grey[300]!),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(getIcon(), size: 16, color: AppColors.brandColor),
          const SizedBox(width: 4),
          Text(amenity.amenityName, style: TextStyle(color: Colors.grey[700])),
        ],
      ),
    );
  }

  Widget _buildStatItem(
    String label,
    String value,
    IconData icon, {
    Color? color,
  }) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, color: color ?? Colors.grey[600], size: 20),
        const SizedBox(height: 4),
        Text(
          value,
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 16,
            color: color ?? Colors.black,
          ),
        ),
        Text(label, style: TextStyle(color: Colors.grey[600], fontSize: 11)),
      ],
    );
  }

  /// Xử lý khi tap vào ghế
  void _onSeatTap(Seat seat, Zone zone) async {
    final messenger = ScaffoldMessenger.of(context);
    final navigator = Navigator.of(context);
    final authService = Provider.of<AuthService>(context, listen: false);
    final bookingService = Provider.of<BookingService>(context, listen: false);
    bool loadingShown = false;

    // Kiểm tra ghế có available không
    if (seat.seatStatus != 'AVAILABLE') {
      final message = _seatBlockedMessage(seat);
      messenger.showSnackBar(
        SnackBar(content: Text(message), backgroundColor: Colors.red),
      );
      return;
    }

    // Kiểm tra đã chọn time slot chưa
    if (_selectedTimeSlot == null) {
      messenger.showSnackBar(
        const SnackBar(
          content: Text('Vui lòng chọn khung giờ trước'),
          backgroundColor: Colors.orange,
        ),
      );
      return;
    }

    // Hiển thị popup xác nhận thông tin ghế
    final confirmed = await _showSeatConfirmPopup(seat, zone);
    if (confirmed != true) return;
    if (!mounted) return;

    final acceptedOngoingSlot = await _confirmOngoingTimeSlotIfNeeded();
    if (!acceptedOngoingSlot || !mounted) return;

    // Kiểm tra user đã đăng nhập
    final user = authService.currentUser;

    if (user == null) {
      messenger.showSnackBar(
        const SnackBar(
          content: Text('Vui lòng đăng nhập để đặt chỗ'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    // Tạo booking với status PROCESSING (ghế chuyển sang HOLDING)
    try {
      // Show loading
      showDialog(
        context: navigator.context,
        barrierDismissible: false,
        builder: (_) => const Center(child: CircularProgressIndicator()),
      );
      loadingShown = true;

      final timeParts = _selectedTimeSlot!.split(' - ');

      if (_replacementMode) {
        await bookingService.changeSeatForAffectedReservation(
          reservationId: widget.replacementReservationId!,
          newSeatId: seat.seatId,
        );

        if (!mounted) return;
        if (loadingShown && navigator.canPop()) {
          navigator.pop();
        }
        navigator.pop(true);
        return;
      }

      final result = await bookingService.createBooking(
        seatId: seat.seatId,
        userId: user.id,
        date: _selectedDate,
        start: timeParts[0],
        end: timeParts[1],
      );

      if (!mounted) return;

      navigator.pop(); // Close loading

      final reservationId = result['reservationId']?.toString();
      if (reservationId == null) {
        throw Exception('Không nhận được mã đặt chỗ');
      }

      // Reload seats để hiển thị trạng thái HOLDING
      _loadZonesAndFactories();

      // Navigate tới màn hình xác nhận (user sẽ confirm hoặc cancel)
      navigator
          .push(
            MaterialPageRoute(
              builder: (_) => BookingConfirmScreen(
                seat: seat,
                date: _selectedDate,
                timeSlot: _selectedTimeSlot!,
                zoneName: zone.zoneName,
                userId: user.id,
                reservationId: reservationId,
              ),
            ),
          )
          .then((_) {
            // Clear cache và reload seats khi quay lại từ confirm screen
            _clearSeatsCache();
            _loadSeatsOnly();
          });
    } catch (e) {
      if (!mounted) return;
      if (loadingShown && navigator.canPop()) {
        navigator.pop();
      }
      // Parse lỗi để hiển thị message thân thiện
      final errorMsg = e.toString().replaceFirst('Exception: ', '');
      messenger.showSnackBar(
        SnackBar(content: Text(errorMsg), backgroundColor: Colors.red),
      );
    }
  }

  /// Popup xác nhận thông tin ghế
  Future<bool?> _showSeatConfirmPopup(Seat seat, Zone zone) {
    return showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (context) {
        final mediaQuery = MediaQuery.of(context);

        return SafeArea(
          top: false,
          child: Padding(
            padding: EdgeInsets.only(bottom: mediaQuery.viewInsets.bottom),
            child: Container(
              constraints: BoxConstraints(
                maxHeight: mediaQuery.size.height * 0.85,
              ),
              padding: EdgeInsets.fromLTRB(
                24,
                24,
                24,
                24 + mediaQuery.padding.bottom,
              ),
              decoration: const BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
              ),
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // Handle bar
                    Container(
                      width: 40,
                      height: 4,
                      margin: const EdgeInsets.only(bottom: 20),
                      decoration: BoxDecoration(
                        color: Colors.grey[300],
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),

                    // Seat icon
                    Container(
                      width: 80,
                      height: 80,
                      decoration: BoxDecoration(
                        color: AppColors.brandColor.withAlpha(30),
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: Icon(
                        Icons.event_seat,
                        size: 48,
                        color: AppColors.brandColor,
                      ),
                    ),
                    const SizedBox(height: 20),

                    // Seat code
                    Text(
                      'Ghế ${seat.seatCode}',
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 8),

                    // Zone name
                    Text(
                      'Khu vực: ${zone.zoneName}',
                      style: TextStyle(fontSize: 16, color: Colors.grey[600]),
                    ),
                    const SizedBox(height: 16),

                    // Date and time info
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Colors.grey[100],
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: [
                          Column(
                            children: [
                              Icon(
                                Icons.calendar_today,
                                color: AppColors.brandColor,
                              ),
                              const SizedBox(height: 4),
                              Text(
                                DateFormat('dd/MM').format(_selectedDate),
                                style: const TextStyle(
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              Text(
                                DateFormat('EEE', 'vi').format(_selectedDate),
                                style: TextStyle(
                                  fontSize: 12,
                                  color: Colors.grey[600],
                                ),
                              ),
                            ],
                          ),
                          Container(
                            width: 1,
                            height: 40,
                            color: Colors.grey[300],
                          ),
                          Column(
                            children: [
                              Icon(
                                Icons.access_time,
                                color: AppColors.brandColor,
                              ),
                              const SizedBox(height: 4),
                              Text(
                                _selectedTimeSlot ?? '',
                                style: const TextStyle(
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              Text(
                                'Khung giờ',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: Colors.grey[600],
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 24),

                    // Buttons
                    Row(
                      children: [
                        Expanded(
                          child: OutlinedButton(
                            onPressed: () => Navigator.pop(context, false),
                            style: OutlinedButton.styleFrom(
                              padding: const EdgeInsets.symmetric(vertical: 14),
                              side: BorderSide(color: Colors.grey[400]!),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                            ),
                            child: const Text(
                              'Hủy',
                              style: TextStyle(color: Colors.grey),
                            ),
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          flex: 2,
                          child: ElevatedButton(
                            onPressed: () => Navigator.pop(context, true),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: AppColors.brandColor,
                              padding: const EdgeInsets.symmetric(vertical: 14),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                            ),
                            child: const Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Text(
                                  'Tiếp theo',
                                  style: TextStyle(
                                    color: Colors.white,
                                    fontSize: 16,
                                  ),
                                ),
                                SizedBox(width: 8),
                                Icon(
                                  Icons.arrow_forward,
                                  color: Colors.white,
                                  size: 18,
                                ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      appBar: AppBar(
        title: const Text(
          'Sơ đồ thư viện',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
        actions: [
          IconButton(
            icon: const Icon(Icons.info_outline),
            onPressed: _showLegendDialog,
          ),
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                // Area tabs
                if (_areas.length > 1) _buildAreaTabs(),
                // Date and Time Slot Filter - LUÔN HIỂN THỊ để cho phép chọn ngày khác
                _buildDateTimeFilter(),
                // Non-working day message OR Error message OR Floor plan
                Expanded(
                  child: _isLibraryClosed
                      ? _buildLibraryLockedMessage()
                      : _isNonWorkingDay
                      ? _buildClosedDayMessage()
                      : _errorMessage != null
                      ? _buildClosedTodayMessage()
                      : _buildFloorPlan(),
                ),
                // Tip (only show when working day and no error)
                if (!_isLibraryClosed &&
                    !_isNonWorkingDay &&
                    _errorMessage == null)
                  Container(
                    padding: const EdgeInsets.all(12),
                    color: const Color(0xFFFFF5EE),
                    child: Text(
                      _replacementMode
                          ? 'Chạm vào ghế màu xanh để đổi sang ghế mới cho lịch hiện tại'
                          : 'Chạm vào ghế màu xanh để đặt chỗ',
                      textAlign: TextAlign.center,
                      style: const TextStyle(color: Colors.grey, fontSize: 12),
                    ),
                  ),
              ],
            ),
    );
  }

  /// Widget hiển thị khi thư viện bị khoá bởi admin
  Widget _buildLibraryLockedMessage() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.red[50],
                shape: BoxShape.circle,
              ),
              child: Icon(Icons.lock, size: 48, color: Colors.red[400]),
            ),
            const SizedBox(height: 24),
            const Text(
              'Thư viện đang tạm đóng',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: Colors.red,
              ),
            ),
            const SizedBox(height: 12),
            Text(
              _closedReason != null && _closedReason!.isNotEmpty
                  ? 'Lý do: $_closedReason'
                  : 'Thư viện tạm thời không nhận đặt chỗ',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 14, color: Colors.grey[700]),
            ),
            const SizedBox(height: 8),
            Text(
              'Vui lòng quay lại sau',
              style: TextStyle(fontSize: 13, color: Colors.grey[500]),
            ),
          ],
        ),
      ),
    );
  }

  /// Widget hiển thị khi chọn ngày thư viện không hoạt động
  Widget _buildClosedDayMessage() {
    final dayName = _getVietnameseDayName(_selectedDate);

    // Find next working day
    DateTime nextWorkingDay = _selectedDate.add(const Duration(days: 1));
    while (_librarySettings != null &&
        !_librarySettings!.isWorkingDay(nextWorkingDay)) {
      nextWorkingDay = nextWorkingDay.add(const Duration(days: 1));
      if (nextWorkingDay.difference(_selectedDate).inDays > 7) break;
    }
    final nextDayName = _getVietnameseDayName(nextWorkingDay);
    final nextDateStr = DateFormat('dd/MM').format(nextWorkingDay);

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.orange[50],
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.event_busy,
                size: 64,
                color: Colors.orange[400],
              ),
            ),
            const SizedBox(height: 24),
            Text(
              'Thư viện không hoạt động',
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: Colors.grey[800],
              ),
            ),
            const SizedBox(height: 12),
            Text(
              'Thư viện không mở cửa vào $dayName.\nVui lòng chọn ngày khác để đặt chỗ.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 16,
                color: Colors.grey[600],
                height: 1.5,
              ),
            ),
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              decoration: BoxDecoration(
                color: Colors.blue[50],
                borderRadius: BorderRadius.circular(20),
              ),
              child: Text(
                'Thư viện mở cửa: Thứ Hai - Thứ Sáu',
                style: TextStyle(
                  fontSize: 14,
                  color: Colors.blue[700],
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            const SizedBox(height: 32),
            ElevatedButton.icon(
              onPressed: () {
                setState(() {
                  _selectedDate = nextWorkingDay;
                  _isNonWorkingDay = false;
                });
                _clearSeatsCache();
                _loadData();
              },
              icon: const Icon(Icons.calendar_today),
              label: Text('Chọn $nextDayName ($nextDateStr)'),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.brandColor,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(
                  horizontal: 24,
                  vertical: 14,
                ),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Widget hiển thị khi thư viện đã đóng cửa hôm nay (quá giờ)
  Widget _buildClosedTodayMessage() {
    // Find next working day
    DateTime nextWorkingDay = DateTime.now().add(const Duration(days: 1));
    while (_librarySettings != null &&
        !_librarySettings!.isWorkingDay(nextWorkingDay)) {
      nextWorkingDay = nextWorkingDay.add(const Duration(days: 1));
      if (nextWorkingDay.difference(DateTime.now()).inDays > 7) break;
    }
    final nextDayName = _getVietnameseDayName(nextWorkingDay);
    final nextDateStr = DateFormat('dd/MM').format(nextWorkingDay);

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.orange[50],
                shape: BoxShape.circle,
              ),
              child: Icon(Icons.schedule, size: 64, color: Colors.orange[400]),
            ),
            const SizedBox(height: 24),
            Text(
              'Đã hết giờ đặt chỗ hôm nay',
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: Colors.grey[800],
              ),
            ),
            const SizedBox(height: 12),
            Text(
              'Thư viện đã đóng cửa hôm nay.\nHãy chọn ngày khác để đặt chỗ.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 16,
                color: Colors.grey[600],
                height: 1.5,
              ),
            ),
            const SizedBox(height: 32),
            ElevatedButton.icon(
              onPressed: () {
                // Filter slots cho ngày mới
                final validSlots = _filterTimeSlotsForDate(
                  nextWorkingDay,
                  _timeSlots,
                );
                final newTimeSlot = _findCurrentSlot(
                  validSlots,
                  forDate: nextWorkingDay,
                );

                setState(() {
                  _selectedDate = nextWorkingDay;
                  _selectedTimeSlot = newTimeSlot;
                  _errorMessage = null;
                });
                _clearSeatsCache();
                _loadZonesAndFactories();
              },
              icon: const Icon(Icons.calendar_today),
              label: Text('Đặt chỗ $nextDayName ($nextDateStr)'),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.brandColor,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(
                  horizontal: 24,
                  vertical: 14,
                ),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Widget chọn ngày và khung giờ
  Widget _buildDateTimeFilter() {
    final maxDays = _librarySettings?.maxBookingDays ?? 14;
    final lastBookableDate = _getLastBookableDate(maxDays);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      color: Colors.white,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (_replacementMode) ...[
            Container(
              width: double.infinity,
              margin: const EdgeInsets.only(bottom: 12),
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFFFF7ED),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: const Color(0xFFFDBA74)),
              ),
              child: const Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(
                    Icons.warning_amber_rounded,
                    color: Color(0xFFEA580C),
                    size: 18,
                  ),
                  SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Bạn đang đổi ghế cho lịch đặt bị ảnh hưởng. Ngày và khung giờ hiện tại được giữ nguyên.',
                      style: TextStyle(
                        color: Color(0xFF9A3412),
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        height: 1.4,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
          // Row 1: Date selector
          Row(
            children: [
              Expanded(
                child: InkWell(
                  onTap: _replacementMode
                      ? null
                      : () async {
                    final date = await showDatePicker(
                      context: context,
                      initialDate: _selectedDate,
                      firstDate: DateTime.now(),
                      lastDate: lastBookableDate,
                      selectableDayPredicate: (day) {
                        return _librarySettings?.isWorkingDay(day) ?? true;
                      },
                    );
                    if (date != null) {
                      // Filter slots cho ngày mới
                      final validSlots = _filterTimeSlotsForDate(
                        date,
                        _timeSlots,
                      );
                      // Reset time slot về slot đầu tiên của ngày mới (giờ mở cửa nếu là ngày mai)
                      final newTimeSlot = _findCurrentSlot(
                        validSlots,
                        forDate: date,
                      );

                      setState(() {
                        _selectedDate = date;
                        _selectedTimeSlot = newTimeSlot;
                        _errorMessage = validSlots.isEmpty
                            ? 'Không còn khung giờ trống cho ngày này'
                            : null;
                      });

                      // Clear cache và reload seats for new date
                      _clearSeatsCache();
                      _loadZonesAndFactories();
                    }
                  },
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 8,
                    ),
                    decoration: BoxDecoration(
                      color: AppColors.brandColor.withAlpha(20),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(
                        color: AppColors.brandColor.withAlpha(50),
                      ),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          DateFormat('EEE, dd/MM', 'vi').format(_selectedDate),
                          style: TextStyle(
                            color: AppColors.brandColor,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(width: 4),
                        Icon(
                          Icons.arrow_drop_down,
                          color: AppColors.brandColor,
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          // Row 2: Time slot chips - filtered by selected date
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Builder(
              builder: (context) {
                // Chỉ hiển thị slots hợp lệ cho ngày đã chọn
                final validSlots = _filterTimeSlotsForDate(
                  _selectedDate,
                  _timeSlots,
                );
                if (validSlots.isEmpty) {
                  return Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    child: Text(
                      'Không còn khung giờ trống cho ngày này',
                      style: TextStyle(
                        color: Colors.grey[600],
                        fontStyle: FontStyle.italic,
                      ),
                    ),
                  );
                }
                return Row(
                  children: validSlots.map((slot) {
                    final isSelected = _selectedTimeSlot == slot.label;
                    return Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: ChoiceChip(
                        label: Text(
                          slot.label,
                          style: TextStyle(
                            fontSize: 12,
                            color: isSelected ? Colors.white : Colors.grey[700],
                          ),
                        ),
                        selected: isSelected,
                        selectedColor: AppColors.brandColor,
                        backgroundColor: Colors.grey[100],
                        onSelected: _replacementMode
                            ? null
                            : (selected) {
                                if (selected) {
                                  setState(() => _selectedTimeSlot = slot.label);
                                  _loadSeatsOnly(); // Chỉ reload seats - nhanh hơn
                                }
                              },
                      ),
                    );
                  }).toList(),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAreaTabs() {
    return Container(
      height: 50,
      color: Colors.white,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 12),
        itemCount: _areas.length,
        itemBuilder: (context, index) {
          final area = _areas[index];
          final isSelected = _selectedArea?.areaId == area.areaId;
          return GestureDetector(
            onTap: () {
              setState(() => _selectedArea = area);
              _loadZonesAndFactories();
            },
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
              margin: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
              decoration: BoxDecoration(
                color: isSelected ? AppColors.brandColor : Colors.grey[200],
                borderRadius: BorderRadius.circular(20),
              ),
              child: Text(
                area.areaName,
                style: TextStyle(
                  color: isSelected ? Colors.white : Colors.black87,
                  fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildFloorPlan() {
    final screenWidth = MediaQuery.of(context).size.width;
    final screenHeight = MediaQuery.of(context).size.height;

    return InteractiveViewer(
      constrained: false,
      boundaryMargin: EdgeInsets.all(max(screenWidth, screenHeight) * 2),
      minScale: 0.02, // 2% - rất nhỏ để xem toàn bộ dù thư viện lớn bao nhiêu
      maxScale: 5.0,
      panEnabled: true,
      scaleEnabled: true,
      child: Container(
        width: _contentWidth + 40,
        height: _contentHeight + 40,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(color: Colors.grey[100]),
        child: Stack(
          children: [
            // Grid background
            CustomPaint(
              size: Size(_contentWidth, _contentHeight),
              painter: _GridPainter(),
            ),
            // Factories (obstacles) - gray
            ..._factories.map((f) => _buildFactoryWidget(f, 1.0)),
            // Zones - colored by occupancy with seat info
            ..._zones.map((z) => _buildZoneWidget(z, 1.0)),
          ],
        ),
      ),
    );
  }

  Widget _buildFactoryWidget(AreaFactory factory, double scale) {
    final x = factory.positionX * scale;
    final y = factory.positionY * scale;
    final w = factory.width * scale;
    final h = factory.height * scale;

    return Positioned(
      left: x,
      top: y,
      child: Container(
        width: w,
        height: h,
        decoration: BoxDecoration(
          color: const Color(0xFFBDC3C7),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.grey[600]!, width: 1),
        ),
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(4),
            child: Text(
              factory.factoryName,
              style: TextStyle(
                color: Colors.grey[700],
                fontSize: 11,
                fontWeight: FontWeight.w500,
              ),
              textAlign: TextAlign.center,
              overflow: TextOverflow.ellipsis,
              maxLines: 2,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildZoneWidget(Zone zone, double scale) {
    final occupancy = _zoneOccupancy[zone.zoneId] ?? 0;
    final color = _getZoneColor(occupancy);
    final seats = _zoneSeats[zone.zoneId] ?? [];

    // Sắp xếp ghế theo row rồi column
    final sortedSeats = List<Seat>.from(seats)
      ..sort((a, b) {
        if (a.rowNumber != b.rowNumber) {
          return a.rowNumber.compareTo(b.rowNumber);
        }
        return a.columnNumber.compareTo(b.columnNumber);
      });

    final availableSeats = sortedSeats
        .where((s) => s.seatStatus == 'AVAILABLE')
        .length;
    final totalSeats = sortedSeats.length;

    final x = zone.positionX * scale;
    final y = zone.positionY * scale;
    final w = zone.width * scale;
    final h = zone.height * scale;

    // Tính kích thước ghế dựa trên zone size
    final seatSize = totalSeats > 0
        ? ((w - 16) / (totalSeats > 6 ? 6 : totalSeats)).clamp(20.0, 40.0)
        : 30.0;

    return Positioned(
      left: x,
      top: y,
      child: GestureDetector(
        onTap: () => _showZoneBottomSheet(zone),
        child: Container(
          width: w,
          height: h,
          decoration: BoxDecoration(
            color: color.withAlpha(50), // Background màu theo mật độ
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: color, width: 2),
            boxShadow: [
              BoxShadow(
                color: color.withAlpha(40),
                blurRadius: 4,
                offset: const Offset(0, 2),
              ),
            ],
          ),
          child: Column(
            children: [
              // Header: tên zone + trạng thái ghế
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 3),
                decoration: BoxDecoration(
                  color: color,
                  borderRadius: const BorderRadius.only(
                    topLeft: Radius.circular(6),
                    topRight: Radius.circular(6),
                  ),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    // Tên zone
                    Expanded(
                      child: Text(
                        zone.zoneName,
                        style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                          fontSize: 10,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    // Trạng thái ghế
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 4,
                        vertical: 1,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.white.withAlpha(50),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(
                            Icons.event_seat,
                            color: Colors.white,
                            size: 10,
                          ),
                          const SizedBox(width: 2),
                          Text(
                            '${totalSeats - availableSeats}/$totalSeats',
                            style: const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                              fontSize: 9,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              // Seat grid - grouped by row
              Expanded(
                child: totalSeats == 0
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(
                              Icons.event_seat_outlined,
                              color: color.withAlpha(150),
                              size: 24,
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'Chưa có ghế',
                              style: TextStyle(
                                color: color.withAlpha(180),
                                fontSize: 10,
                              ),
                            ),
                          ],
                        ),
                      )
                    : Padding(
                        padding: const EdgeInsets.all(4),
                        child: SingleChildScrollView(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: _buildSeatRows(
                              sortedSeats,
                              seatSize,
                              color,
                              w,
                              h,
                              zone,
                            ),
                          ),
                        ),
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// Build seat rows grouped by row number - mã ghế đầy đủ (A1, A2...)
  List<Widget> _buildSeatRows(
    List<Seat> seats,
    double seatSize,
    Color color,
    double zoneWidth,
    double zoneHeight,
    Zone zone,
  ) {
    // Group seats by row
    Map<int, List<Seat>> seatsByRow = {};
    for (final seat in seats) {
      seatsByRow.putIfAbsent(seat.rowNumber, () => []).add(seat);
    }

    // Convert row number to letter (1=A, 2=B, etc.)
    String getRowLabel(int rowNumber) {
      return String.fromCharCode('A'.codeUnitAt(0) + rowNumber - 1);
    }

    // Calculate dynamic spacing based on zone size
    final numRows = seatsByRow.length;
    final maxSeatsPerRow = seatsByRow.values
        .map((r) => r.length)
        .reduce((a, b) => a > b ? a : b);

    // Kích thước ghế cố định
    const fixedSeatSize = 35.0;

    // Available space (trừ padding)
    final availableWidth = zoneWidth - 16; // padding 8 mỗi bên
    final availableHeight = zoneHeight - 50; // header ~30 + padding top/bottom

    // Tính giãn cách đều để ghế phân bố toàn bộ zone
    // Giãn cách ngang = (không gian trống) / (số khe giữa ghế + 2 khe 2 bên)
    final totalSeatWidthInRow = fixedSeatSize * maxSeatsPerRow;
    final hSpacing =
        (availableWidth - totalSeatWidthInRow) / (maxSeatsPerRow + 1);

    // Giãn cách dọc = (không gian trống) / (số khe giữa hàng + 2 khe trên dưới)
    final totalSeatHeightInCol = fixedSeatSize * numRows;
    final vSpacing = (availableHeight - totalSeatHeightInCol) / (numRows + 1);

    List<Widget> rows = [];
    final sortedRowNumbers = seatsByRow.keys.toList()..sort();

    for (int i = 0; i < sortedRowNumbers.length; i++) {
      final rowNum = sortedRowNumbers[i];
      final rowSeats = seatsByRow[rowNum]!;
      // Sort seats in row by column
      rowSeats.sort((a, b) => a.columnNumber.compareTo(b.columnNumber));
      final rowLabel = getRowLabel(rowNum);

      rows.add(
        Padding(
          // Giãn cách dọc giữa các hàng
          padding: EdgeInsets.only(
            top: i == 0 ? max(vSpacing, 4.0) : 0,
            bottom: max(vSpacing, 4.0),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: rowSeats.asMap().entries.map((entry) {
              final j = entry.key;
              final seat = entry.value;
                      final seatLabel = '$rowLabel${seat.columnNumber}';
              return Padding(
                // Giãn cách ngang giữa các ghế
                padding: EdgeInsets.only(
                  left: j == 0 ? max(hSpacing, 4.0) : max(hSpacing / 2, 2.0),
                  right: j == rowSeats.length - 1
                      ? max(hSpacing, 4.0)
                      : max(hSpacing / 2, 2.0),
                ),
                child: GestureDetector(
                  onTap: () => _onSeatTap(seat, zone),
                  child: Tooltip(
                    message: _seatTooltipLabel(seat),
                    child: Container(
                      width: fixedSeatSize,
                      height: fixedSeatSize,
                      decoration: BoxDecoration(
                        color: _seatFillColor(seat),
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(
                          color: _seatBorderColor(seat),
                          width: 1.5,
                        ),
                      ),
                      child: Center(
                        child: Text(
                          seatLabel,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 10,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
        ),
      );
    }

    return rows;
  }
}

class _GridPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.grey[200]!
      ..strokeWidth = 0.5;

    const gridSize = 20.0;

    for (double x = 0; x <= size.width; x += gridSize) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), paint);
    }
    for (double y = 0; y <= size.height; y += gridSize) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), paint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

/// Màn hình chọn ghế theo grid row/column
class SeatGridScreen extends StatefulWidget {
  final Zone zone;
  final double occupancy;

  const SeatGridScreen({
    super.key,
    required this.zone,
    required this.occupancy,
  });

  @override
  State<SeatGridScreen> createState() => _SeatGridScreenState();
}

class _SeatGridScreenState extends State<SeatGridScreen> {
  final BookingService _bookingService = BookingService();

  List<Seat> _seats = [];
  int? _selectedIndex;
  DateTime? _selectedDate;
  String? _selectedTime;
  bool _isLoading = true;
  LibrarySetting? _settings;
  UpcomingBooking? _currentUserUpcomingBooking;

  final List<String> _timeSlots = [
    "07:00 - 09:00",
    "09:00 - 11:00",
    "13:00 - 15:00",
    "15:00 - 17:00",
  ];

  @override
  void initState() {
    super.initState();
    _loadCurrentUserUpcomingBooking();
    _loadData();
  }

  bool _isSameCalendarDay(DateTime left, DateTime right) {
    return left.year == right.year &&
        left.month == right.month &&
        left.day == right.day;
  }

  bool _isCurrentUserBookedSeat(Seat seat) {
    final booking = _currentUserUpcomingBooking;
    if (booking == null || _selectedDate == null || _selectedTime == null) {
      return false;
    }

    return booking.seatId == seat.seatId &&
        _isSameCalendarDay(booking.startTime, _selectedDate!) &&
        booking.timeRange.trim() == _selectedTime!.trim();
  }

  Future<void> _loadCurrentUserUpcomingBooking() async {
    final authService = Provider.of<AuthService>(context, listen: false);
    final userId = authService.currentUser?.id;
    if (userId == null) {
      if (mounted) {
        setState(() => _currentUserUpcomingBooking = null);
      }
      return;
    }

    try {
      final data = await _bookingService.getUpcomingBooking(userId);
      if (!mounted) return;

      UpcomingBooking? booking;
      if (data is Map<String, dynamic>) {
        booking = UpcomingBooking.fromJson(data);
      }

      setState(() => _currentUserUpcomingBooking = booking);
    } catch (e) {
      debugPrint('Không thể tải upcoming booking hiện tại: $e');
    }
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      // Load settings first
      final settings = await _bookingService.getLibrarySettings();

      // Find first working day from today
      DateTime firstWorkingDay = DateTime.now();
      for (int i = 0; i < 14; i++) {
        if (settings.isWorkingDay(firstWorkingDay)) break;
        firstWorkingDay = firstWorkingDay.add(const Duration(days: 1));
      }

      setState(() {
        _settings = settings;
        _selectedDate = firstWorkingDay;
      });

      await _loadSeats();
    } catch (e) {
      debugPrint('Error loading settings: $e');
      await _loadSeats();
    }
  }

  Future<void> _loadSeats() async {
    setState(() => _isLoading = true);
    try {
      final seats = await _bookingService.getSeats(widget.zone.zoneId);
      seats.sort((a, b) {
        if (a.rowNumber != b.rowNumber) {
          return a.rowNumber.compareTo(b.rowNumber);
        }
        return a.columnNumber.compareTo(b.columnNumber);
      });
      setState(() {
        _seats = seats;
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _loadSeatsByTime() async {
    if (_selectedDate == null || _selectedTime == null) return;

    setState(() => _isLoading = true);
    try {
      final parts = _selectedTime!.split(' - ');
      final seats = await _bookingService.getSeatsByTime(
        widget.zone.zoneId,
        _selectedDate!,
        parts[0],
        parts[1],
      );
      seats.sort((a, b) {
        if (a.rowNumber != b.rowNumber) {
          return a.rowNumber.compareTo(b.rowNumber);
        }
        return a.columnNumber.compareTo(b.columnNumber);
      });
      setState(() {
        _seats = seats;
        _isLoading = false;
        _selectedIndex = null;
      });
    } catch (e) {
      setState(() => _isLoading = false);
    }
  }

  DateTime? _buildSlotDateTime(DateTime date, String timeValue) {
    final parts = timeValue.split(':');
    if (parts.length < 2) return null;

    final hour = int.tryParse(parts[0]);
    final minute = int.tryParse(parts[1]);
    if (hour == null || minute == null) return null;

    return DateTime(date.year, date.month, date.day, hour, minute);
  }

  int? _getRemainingMinutesForOngoingSlot(DateTime date, String? timeSlot) {
    if (timeSlot == null) return null;

    final now = DateTime.now();
    final isToday =
        date.year == now.year && date.month == now.month && date.day == now.day;
    if (!isToday) return null;

    final parts = timeSlot.split(' - ');
    if (parts.length != 2) return null;

    final slotStart = _buildSlotDateTime(date, parts[0]);
    final slotEnd = _buildSlotDateTime(date, parts[1]);
    if (slotStart == null || slotEnd == null) return null;

    if (now.isBefore(slotStart) || !now.isBefore(slotEnd)) return null;

    final remaining = slotEnd.difference(now);
    if (remaining.inSeconds <= 0) return null;

    return (remaining.inSeconds / 60).ceil();
  }

  Future<void> _confirmBooking() async {
    if (_selectedIndex == null ||
        _selectedTime == null ||
        _selectedDate == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng chọn ngày, giờ và ghế')),
      );
      return;
    }

    final authService = Provider.of<AuthService>(context, listen: false);
    final userId = authService.currentUser?.id;
    if (userId == null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Vui lòng đăng nhập')));
      return;
    }

    final parts = _selectedTime!.split(' - ');

    final remainingMinutes = _getRemainingMinutesForOngoingSlot(
      _selectedDate!,
      _selectedTime,
    );
    if (remainingMinutes != null) {
      final accepted = await showDialog<bool>(
        context: context,
        builder: (dialogContext) => AlertDialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
          ),
          title: const Text(
            'Khung giờ này đã bắt đầu',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          content: Text(
            'Suất ngồi bạn chọn đang diễn ra. Nếu tiếp tục đặt chỗ, bạn chỉ còn khoảng ${remainingMinutes <= 1 ? 'chưa đến 1 phút' : '$remainingMinutes phút'} sử dụng trong khung giờ $_selectedTime. Bạn vẫn muốn tiếp tục chứ?',
            style: const TextStyle(height: 1.45),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('Chọn khung giờ khác'),
            ),
            ElevatedButton(
              onPressed: () => Navigator.pop(dialogContext, true),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.brandColor,
                foregroundColor: Colors.white,
              ),
              child: const Text('Tiếp tục đặt'),
            ),
          ],
        ),
      );

      if (accepted != true) {
        return;
      }
    }

    try {
      await _bookingService.createBooking(
        userId: userId,
        seatId: _seats[_selectedIndex!].seatId,
        date: _selectedDate!,
        start: parts[0],
        end: parts[1],
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Đặt ghế ${_seats[_selectedIndex!].seatCode} thành công!',
            ),
            backgroundColor: AppColors.success,
          ),
        );
        Navigator.pop(context);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Lỗi: $e'), backgroundColor: AppColors.error),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: Text(widget.zone.zoneName),
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: ElevatedButton.icon(
              icon: const Icon(Icons.date_range),
              label: Text(
                _selectedDate != null
                    ? DateFormat('dd/MM/yyyy').format(_selectedDate!)
                    : 'Chọn ngày đặt',
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.amber[800],
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(
                  horizontal: 20,
                  vertical: 12,
                ),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              onPressed: () async {
                final now = DateTime.now();
                // Calculate last bookable date based on working days
                DateTime lastBookableDate = now;
                int workingDaysCount = 0;
                final maxDays = _settings?.maxBookingDays ?? 14;
                while (workingDaysCount < maxDays) {
                  lastBookableDate = lastBookableDate.add(
                    const Duration(days: 1),
                  );
                  if (_settings?.isWorkingDay(lastBookableDate) ?? true) {
                    workingDaysCount++;
                  }
                }

                // Find first working day for initial date
                DateTime initialDate = _selectedDate ?? now;
                if (_settings != null &&
                    !_settings!.isWorkingDay(initialDate)) {
                  for (int i = 0; i < 14; i++) {
                    initialDate = initialDate.add(const Duration(days: 1));
                    if (_settings!.isWorkingDay(initialDate)) break;
                  }
                }

                final picked = await showDatePicker(
                  context: context,
                  initialDate: initialDate,
                  firstDate: now,
                  lastDate: lastBookableDate,
                  selectableDayPredicate: (DateTime day) {
                    // Only allow working days
                    return _settings?.isWorkingDay(day) ?? true;
                  },
                );
                if (picked != null) {
                  setState(() {
                    _selectedDate = picked;
                    _selectedTime = null;
                    _selectedIndex = null;
                  });
                }
              },
            ),
          ),

          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: _timeSlots.map((slot) {
                  final isSelected = _selectedTime == slot;
                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                      label: Text(slot),
                      selected: isSelected,
                      selectedColor: Colors.orange.withAlpha(80),
                      backgroundColor: Colors.orange.shade50,
                      labelStyle: TextStyle(
                        color: isSelected ? Colors.orange : Colors.black87,
                        fontWeight: FontWeight.bold,
                      ),
                      onSelected: (_) {
                        setState(() => _selectedTime = slot);
                        _loadSeatsByTime();
                      },
                    ),
                  );
                }).toList(),
              ),
            ),
          ),

          const Divider(height: 32),

          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                _legend(AppColors.seatAvailable, 'Trống'),
                const SizedBox(width: 16),
                _legend(AppColors.brandColor, 'Ghế của tôi'),
                const SizedBox(width: 16),
                _legend(AppColors.seatOccupied, 'Đã đặt'),
                const SizedBox(width: 16),
                _legend(Colors.grey[400]!, 'Bảo trì'),
                const SizedBox(width: 16),
                _legend(AppColors.brandColor, 'Đang chọn'),
              ],
            ),
          ),

          const SizedBox(height: 16),

          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _seats.isEmpty
                ? ErrorDisplayWidget.empty(message: 'Không có ghế')
                : _buildSeatGrid(),
          ),

          _buildFooter(),
        ],
      ),
    );
  }

  Widget _buildSeatGrid() {
    final Map<int, List<Seat>> seatsByRow = {};
    for (final seat in _seats) {
      seatsByRow.putIfAbsent(seat.rowNumber, () => []).add(seat);
    }
    final sortedRows = seatsByRow.keys.toList()..sort();

    // Find max seats per row for display
    int maxSeatsPerRow = 0;
    for (final row in seatsByRow.values) {
      if (row.length > maxSeatsPerRow) maxSeatsPerRow = row.length;
    }

    return ListView.builder(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      itemCount: sortedRows.length,
      itemBuilder: (context, index) {
        final rowNum = sortedRows[index];
        final rowSeats = seatsByRow[rowNum]!;
        final rowLetter = String.fromCharCode(64 + rowNum);

        return Padding(
          padding: const EdgeInsets.only(bottom: 16),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              // Row label with seat count
              SizedBox(
                width: 40,
                child: Column(
                  children: [
                    Text(
                      rowLetter,
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 18,
                        color: Colors.black87,
                      ),
                    ),
                    Text(
                      '${rowSeats.length}',
                      style: TextStyle(fontSize: 10, color: Colors.grey[500]),
                    ),
                  ],
                ),
              ),
              // Horizontal scrollable seats
              Expanded(
                child: SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: rowSeats.map((seat) {
                      final globalIndex = _seats.indexOf(seat);
                      final isSelected = _selectedIndex == globalIndex;
                      final isUnavailable = seat.isUnavailable;
                      final isCurrentUserSeat = _isCurrentUserBookedSeat(seat);
                      final isAvailable =
                          seat.seatStatus == 'AVAILABLE' && !isUnavailable;

                      Color color;
                      if (isSelected) {
                        color = AppColors.brandColor;
                      } else if (isUnavailable) {
                        color = Colors.grey[400]!;
                      } else if (isCurrentUserSeat) {
                        color = AppColors.brandColor;
                      } else if (isAvailable) {
                        color = AppColors.seatAvailable;
                      } else {
                        color = AppColors.seatOccupied;
                      }

                      return Padding(
                        padding: const EdgeInsets.only(right: 8),
                        child: GestureDetector(
                          onTap: (isAvailable && _selectedTime != null)
                              ? () =>
                                    setState(() => _selectedIndex = globalIndex)
                              : null,
                          child: AnimatedContainer(
                            duration: const Duration(milliseconds: 200),
                            width: 48,
                            height: 48,
                            decoration: BoxDecoration(
                              color: color,
                              borderRadius: BorderRadius.circular(10),
                              border: isSelected
                                  ? Border.all(color: Colors.white, width: 2)
                                  : null,
                              boxShadow: isSelected
                                  ? [
                                      BoxShadow(
                                        color: AppColors.brandColor.withAlpha(
                                          100,
                                        ),
                                        blurRadius: 8,
                                        spreadRadius: 2,
                                      ),
                                    ]
                                  : null,
                            ),
                            child: Center(
                              child: Text(
                                '${seat.columnNumber}',
                                style: TextStyle(
                                  color: (isSelected || !isAvailable)
                                      ? Colors.white
                                      : Colors.black54,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 14,
                                ),
                              ),
                            ),
                          ),
                        ),
                      );
                    }).toList(),
                  ),
                ),
              ),
              // Scroll indicator if many seats
              if (rowSeats.length > 6)
                Icon(Icons.chevron_right, color: Colors.grey[400], size: 20),
            ],
          ),
        );
      },
    );
  }

  Widget _legend(Color c, String l) => Row(
    children: [
      Container(
        width: 14,
        height: 14,
        decoration: BoxDecoration(
          color: c,
          borderRadius: BorderRadius.circular(4),
        ),
      ),
      const SizedBox(width: 4),
      Text(l, style: const TextStyle(fontSize: 12)),
    ],
  );

  Widget _buildFooter() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: const BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black12,
            blurRadius: 10,
            offset: Offset(0, -5),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text(
                  'Ghế đã chọn:',
                  style: TextStyle(color: Colors.grey, fontSize: 12),
                ),
                Text(
                  _selectedIndex != null
                      ? '${_seats[_selectedIndex!].seatCode} • ${_selectedTime ?? "Chọn giờ"}'
                      : 'Chưa chọn',
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 16,
                  ),
                ),
              ],
            ),
          ),
          ElevatedButton(
            onPressed:
                (_selectedIndex != null &&
                    _selectedTime != null &&
                    _selectedDate != null)
                ? _confirmBooking
                : null,
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.brandColor,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(horizontal: 30, vertical: 12),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
            ),
            child: const Text('XÁC NHẬN'),
          ),
        ],
      ),
    );
  }
}
