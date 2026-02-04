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
