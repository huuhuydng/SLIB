/// Model cho zone với thông tin mật độ sử dụng
class ZoneOccupancy {
  final int zoneId;
  final String zoneName;
  final int positionX;
  final int positionY;
  final int width;
  final int height;
  final int totalSeats;
  final int occupiedSeats;
  final double occupancyRate;

  ZoneOccupancy({
    required this.zoneId,
    required this.zoneName,
    this.positionX = 0,
    this.positionY = 0,
    this.width = 200,
    this.height = 150,
    this.totalSeats = 0,
    this.occupiedSeats = 0,
    this.occupancyRate = 0.0,
  });

  factory ZoneOccupancy.fromJson(Map<String, dynamic> json) {
    return ZoneOccupancy(
      zoneId: json['zoneId'] ?? 0,
      zoneName: json['zoneName'] ?? '',
      positionX: json['positionX'] ?? 0,
      positionY: json['positionY'] ?? 0,
      width: json['width'] ?? 200,
      height: json['height'] ?? 150,
      totalSeats: json['totalSeats'] ?? 0,
      occupiedSeats: json['occupiedSeats'] ?? 0,
      occupancyRate: (json['occupancyRate'] ?? 0.0).toDouble(),
    );
  }

  /// Lấy mức độ đông: 0 = vắng, 1 = vừa, 2 = đông
  int get densityLevel {
    if (occupancyRate >= 0.8) return 2;  // Đông (>80%)
    if (occupancyRate >= 0.5) return 1;  // Vừa (50-80%)
    return 0;  // Vắng (<50%)
  }

  /// Lấy text mô tả mật độ
  String get densityText {
    switch (densityLevel) {
      case 2: return 'Đông';
      case 1: return 'Vừa';
      default: return 'Vắng';
    }
  }

  /// Số ghế còn trống
  int get availableSeats => totalSeats - occupiedSeats;
}
