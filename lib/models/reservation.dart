class Reservation {
  final String id;              // UUID
  final String userId;          // Liên kết với UserProfile.id
  final int seatId;             // Liên kết với Seat.id
  final DateTime startTime;
  final DateTime endTime;
  final String status;          // pending, confirmed, cancelled
  final DateTime createdAt;

  Reservation({
    required this.id,
    required this.userId,
    required this.seatId,
    required this.startTime,
    required this.endTime,
    required this.status,
    required this.createdAt,
  });

  factory Reservation.fromJson(Map<String, dynamic> json) {
    return Reservation(
      id: json['reservation_id'] ?? json['id'],
      userId: json['user_id'],
      seatId: json['seat_id'],
      startTime: DateTime.parse(json['start_time']),
      endTime: DateTime.parse(json['end_time']),
      status: json['status'] ?? 'EXPIRED',
      createdAt: DateTime.parse(json['created_at']),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'reservation_id': id,
      'user_id': userId,
      'seat_id': seatId,
      'start_time': startTime.toIso8601String(),
      'end_time': endTime.toIso8601String(),
      'status': status,
      'created_at': createdAt.toIso8601String(),
    };
  }
}
