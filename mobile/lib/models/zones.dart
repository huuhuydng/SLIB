/// Model Zone (Khu vực trong Area) - tương ứng với Zone đã config từ Admin
class Zone {
  final int zoneId;
  final int areaId;
  final String zoneName;
  final String? description;
  final int positionX;
  final int positionY;
  final int width;
  final int height;
  final String? color;
  final bool hasPowerOutlet;

  Zone({
    required this.zoneId,
    required this.areaId,
    required this.zoneName,
    this.description,
    this.positionX = 0,
    this.positionY = 0,
    this.width = 200,
    this.height = 150,
    this.color,
    this.hasPowerOutlet = false,
  });

  factory Zone.fromJson(Map<String, dynamic> json) {
    return Zone(
      zoneId: json['zoneId'] ?? json['zone_id'] ?? 0,
      areaId: json['areaId'] ?? json['area_id'] ?? 0,
      zoneName: json['zoneName'] ?? json['zone_name'] ?? '',
      description: json['zoneDes'] ?? json['description'],
      positionX: json['positionX'] ?? json['position_x'] ?? 0,
      positionY: json['positionY'] ?? json['position_y'] ?? 0,
      width: json['width'] ?? 200,
      height: json['height'] ?? 150,
      color: json['color'],
      hasPowerOutlet: json['hasPowerOutlet'] ?? json['has_power_outlet'] ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'zoneId': zoneId,
      'areaId': areaId,
      'zoneName': zoneName,
      'description': description,
      'positionX': positionX,
      'positionY': positionY,
      'width': width,
      'height': height,
      'color': color,
      'hasPowerOutlet': hasPowerOutlet,
    };
  }
}

/// Giữ lại class cũ cho backward compatibility
class Zones {
  final int id;
  final String name;
  final String description;
  final bool hasPowerOutlet;

  Zones({
    required this.id,
    required this.name,
    required this.description,
    required this.hasPowerOutlet,
  });

  factory Zones.fromJson(Map<String, dynamic> json) {
    return Zones(
      id: json['zoneId'] ?? 0,
      name: json['zoneName'] ?? '',
      description: json['zoneDes'] ?? '',
      hasPowerOutlet: json['hasPowerOutlet'] ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'zone_id': id,
      'zone_name': name,
      'zone_des': description,
      'has_power_outlet': hasPowerOutlet,
    };
  }
}
