/// Model TimeSlot (Khung giờ đặt chỗ)
class TimeSlot {
  final String startTime;  // "07:00"
  final String endTime;    // "08:00"
  final String label;      // "07:00 - 08:00"

  TimeSlot({
    required this.startTime,
    required this.endTime,
    required this.label,
  });

  factory TimeSlot.fromJson(Map<String, dynamic> json) {
    return TimeSlot(
      startTime: json['startTime'] ?? json['start_time'] ?? '',
      endTime: json['endTime'] ?? json['end_time'] ?? '',
      label: json['label'] ?? '${json['startTime']} - ${json['endTime']}',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'startTime': startTime,
      'endTime': endTime,
      'label': label,
    };
  }
}

/// Model LibrarySetting (Cấu hình thư viện)
class LibrarySetting {
  final String openTime;
  final String closeTime;
  final int slotDuration;
  final int maxBookingDays;
  final String workingDays; // "2,3,4,5,6" (1=CN, 2=T2, ..., 7=T7)
  final int maxBookingsPerDay; // Số lần đặt tối đa mỗi ngày
  final int maxHoursPerDay; // Số giờ tối đa được đặt mỗi ngày
  final bool libraryClosed; // true = thư viện đang tạm đóng
  final String? closedReason; // Lý do đóng thư viện

  LibrarySetting({
    required this.openTime,
    required this.closeTime,
    required this.slotDuration,
    required this.maxBookingDays,
    required this.workingDays,
    this.maxBookingsPerDay = 3,
    this.maxHoursPerDay = 4,
    this.libraryClosed = false,
    this.closedReason,
  });

  factory LibrarySetting.fromJson(Map<String, dynamic> json) {
    return LibrarySetting(
      openTime: json['openTime'] ?? json['open_time'] ?? '07:00',
      closeTime: json['closeTime'] ?? json['close_time'] ?? '21:00',
      slotDuration: json['slotDuration'] ?? json['slot_duration'] ?? 60,
      maxBookingDays: json['maxBookingDays'] ?? json['max_booking_days'] ?? 14,
      workingDays: json['workingDays'] ?? json['working_days'] ?? '2,3,4,5,6',
      maxBookingsPerDay: json['maxBookingsPerDay'] ?? json['max_bookings_per_day'] ?? 3,
      maxHoursPerDay: json['maxHoursPerDay'] ?? json['max_hours_per_day'] ?? 4,
      libraryClosed: json['libraryClosed'] ?? json['library_closed'] ?? false,
      closedReason: json['closedReason'] ?? json['closed_reason'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'openTime': openTime,
      'closeTime': closeTime,
      'slotDuration': slotDuration,
      'maxBookingDays': maxBookingDays,
      'workingDays': workingDays,
      'maxBookingsPerDay': maxBookingsPerDay,
      'maxHoursPerDay': maxHoursPerDay,
      'libraryClosed': libraryClosed,
      'closedReason': closedReason,
    };
  }

  /// Parse workingDays string thành list int
  /// "2,3,4,5,6" -> [2, 3, 4, 5, 6]
  List<int> get workingDaysList {
    return workingDays.split(',').map((s) => int.tryParse(s.trim()) ?? 0).where((d) => d > 0).toList();
  }

  /// Check xem ngày có phải ngày làm việc hay không
  /// DateTime.weekday: 1=T2, 2=T3, ..., 7=CN
  /// Config: 1=CN, 2=T2, ..., 7=T7
  bool isWorkingDay(DateTime date) {
    // Convert DateTime.weekday (1=Mon, 7=Sun) to config format (1=Sun, 2=Mon, ...)
    int configDay = date.weekday == 7 ? 1 : date.weekday + 1;
    return workingDaysList.contains(configDay);
  }
}

