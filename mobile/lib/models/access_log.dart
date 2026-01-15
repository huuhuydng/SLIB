class AccessLog {
  final String id;          // UUID
  final String userId;
  final String reservationId;
  final DateTime? checkInTime;
  final DateTime? checkOutTime;
  final String? deviceId;

  AccessLog({
    required this.id,
    required this.userId,
    required this.reservationId,
    this.checkInTime,
    this.checkOutTime,
    this.deviceId,
  });

  factory AccessLog.fromJson(Map<String, dynamic> json) {
    return AccessLog(
      id: json['log_id'],
      userId: json['user_id'],
      reservationId: json['reservation_id'],
      checkInTime: json['check_in_time'] != null ? DateTime.parse(json['check_in_time']) : null,
      checkOutTime: json['check_out_time'] != null ? DateTime.parse(json['check_out_time']) : null,
      deviceId: json['device_id'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'log_id': id,
      'user_id': userId,
      'reservation_id': reservationId,
      'check_in_time': checkInTime?.toIso8601String(),
      'check_out_time': checkOutTime?.toIso8601String(),
      'device_id': deviceId,
    };
  }
}
