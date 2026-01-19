/// Model ghế - đã simplified để match với backend mới
class Seat {
  final int seatId;
  final int zoneId;
  final String seatCode;
  final String seatStatus;
  final int rowNumber;
  final int columnNumber;

  Seat({
    required this.seatId,
    required this.zoneId,
    required this.seatCode,
    required this.seatStatus,
    required this.rowNumber,
    required this.columnNumber,
  });

  factory Seat.fromJson(Map<String, dynamic> json) {
    return Seat(
      seatId: json['seatId'] as int? ?? 0,
      zoneId: (json['zoneId'] as int?) ?? 0,
      seatCode: json['seatCode'] ?? '',
      seatStatus: json['seatStatus'] ?? 'UNAVAILABLE',
      rowNumber: json['rowNumber'] ?? 1,
      columnNumber: json['columnNumber'] ?? 1,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'seatId': seatId,
      'zoneId': zoneId,
      'seatCode': seatCode,
      'seatStatus': seatStatus,
      'rowNumber': rowNumber,
      'columnNumber': columnNumber,
    };
  }

  /// Kiểm tra ghế có available hay không
  bool get isAvailable => seatStatus == 'AVAILABLE';

  /// Kiểm tra ghế đã được đặt hay chưa
  bool get isBooked => seatStatus == 'BOOKED';

  /// Tính position X dựa trên columnNumber (mỗi ghế 44px + 4px margin)
  double get positionX => (columnNumber - 1) * 48.0;

  /// Tính position Y dựa trên rowNumber (header 40px + mỗi row 48px)
  double get positionY => 40.0 + (rowNumber - 1) * 48.0;

  /// Kích thước mặc định của ghế
  static const double seatWidth = 44.0;
  static const double seatHeight = 44.0;
}
