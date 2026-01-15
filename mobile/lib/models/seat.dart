class Seat {
  final int seatId;
  final int zoneId;
  final String seatCode;
  final String seatStatus;
  final int positionX;
  final int positionY;

  Seat({
    required this.seatId,
    required this.zoneId,
    required this.seatCode,
    required this.seatStatus,
    required this.positionX,
    required this.positionY,
  });

  factory Seat.fromJson(Map<String, dynamic> json) {
    return Seat(
      seatId: json['seatId'] as int? ?? 0,
      zoneId: (json['zoneId'] as int?) ?? 0,
      seatCode: json['seatCode'] ?? '',
      seatStatus: json['seatStatus'] ?? 'UNAVAILABLE',
      positionX: json['positionX'] ?? 0,
      positionY: json['positionY'] ?? 0,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'seat_id': seatId,
      'zone_id': zoneId,
      'seat_code': seatCode,
      'seat_status': seatStatus,
      'position_x': positionX,
      'position_y': positionY,
    };
  }
}
