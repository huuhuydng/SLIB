class UpcomingBooking {
  final String reservationId;
  final String status;
  
  // Seat info
  final int seatId;
  final String seatCode;
  
  // Zone info
  final int zoneId;
  final String zoneName;
  
  // Area info
  final int areaId;
  final String areaName;
  
  // Time info
  final DateTime startTime;
  final DateTime endTime;
  
  // Formatted for display
  final String dayOfWeek;
  final int dayOfMonth;
  final String timeRange;

  // Layout-change warning
  final bool layoutChanged;
  final String? layoutChangeTitle;
  final String? layoutChangeMessage;
  final DateTime? layoutChangedAt;
  final bool canCancel;
  final bool canChangeSeat;

  UpcomingBooking({
    required this.reservationId,
    required this.status,
    required this.seatId,
    required this.seatCode,
    required this.zoneId,
    required this.zoneName,
    required this.areaId,
    required this.areaName,
    required this.startTime,
    required this.endTime,
    required this.dayOfWeek,
    required this.dayOfMonth,
    required this.timeRange,
    this.layoutChanged = false,
    this.layoutChangeTitle,
    this.layoutChangeMessage,
    this.layoutChangedAt,
    this.canCancel = false,
    this.canChangeSeat = false,
  });

  factory UpcomingBooking.fromJson(Map<String, dynamic> json) {
    return UpcomingBooking(
      reservationId: json['reservationId'] ?? '',
      status: json['status'] ?? '',
      seatId: json['seatId'] ?? 0,
      seatCode: json['seatCode'] ?? '',
      zoneId: json['zoneId'] ?? 0,
      zoneName: json['zoneName'] ?? '',
      areaId: json['areaId'] ?? 0,
      areaName: json['areaName'] ?? '',
      startTime: DateTime.parse(json['startTime']),
      endTime: DateTime.parse(json['endTime']),
      dayOfWeek: json['dayOfWeek'] ?? '',
      dayOfMonth: json['dayOfMonth'] ?? 0,
      timeRange: json['timeRange'] ?? '',
      layoutChanged: json['layoutChanged'] == true,
      layoutChangeTitle: json['layoutChangeTitle']?.toString(),
      layoutChangeMessage: json['layoutChangeMessage']?.toString(),
      layoutChangedAt: json['layoutChangedAt'] != null
          ? DateTime.tryParse(json['layoutChangedAt'].toString())
          : null,
      canCancel: json['canCancel'] == true,
      canChangeSeat: json['canChangeSeat'] == true,
    );
  }

  UpcomingBooking copyWith({
    String? reservationId,
    String? status,
    int? seatId,
    String? seatCode,
    int? zoneId,
    String? zoneName,
    int? areaId,
    String? areaName,
    DateTime? startTime,
    DateTime? endTime,
    String? dayOfWeek,
    int? dayOfMonth,
    String? timeRange,
    bool? layoutChanged,
    String? layoutChangeTitle,
    String? layoutChangeMessage,
    DateTime? layoutChangedAt,
    bool? canCancel,
    bool? canChangeSeat,
  }) {
    return UpcomingBooking(
      reservationId: reservationId ?? this.reservationId,
      status: status ?? this.status,
      seatId: seatId ?? this.seatId,
      seatCode: seatCode ?? this.seatCode,
      zoneId: zoneId ?? this.zoneId,
      zoneName: zoneName ?? this.zoneName,
      areaId: areaId ?? this.areaId,
      areaName: areaName ?? this.areaName,
      startTime: startTime ?? this.startTime,
      endTime: endTime ?? this.endTime,
      dayOfWeek: dayOfWeek ?? this.dayOfWeek,
      dayOfMonth: dayOfMonth ?? this.dayOfMonth,
      timeRange: timeRange ?? this.timeRange,
      layoutChanged: layoutChanged ?? this.layoutChanged,
      layoutChangeTitle: layoutChangeTitle ?? this.layoutChangeTitle,
      layoutChangeMessage: layoutChangeMessage ?? this.layoutChangeMessage,
      layoutChangedAt: layoutChangedAt ?? this.layoutChangedAt,
      canCancel: canCancel ?? this.canCancel,
      canChangeSeat: canChangeSeat ?? this.canChangeSeat,
    );
  }

  /// Check if this booking is currently active (ongoing)
  bool get isActive {
    final now = DateTime.now();
    return now.isAfter(startTime) && now.isBefore(endTime);
  }

  /// Check if this booking is in the future
  bool get isFuture {
    return DateTime.now().isBefore(startTime);
  }
}
