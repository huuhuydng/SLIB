/// Model Area (Khu vực) - tương ứng với Area đã config từ Admin
class Area {
  final int areaId;
  final String areaName;
  final String? description;
  final int positionX;
  final int positionY;
  final int width;
  final int height;
  final bool isActive;
  final bool isLocked;

  Area({
    required this.areaId,
    required this.areaName,
    this.description,
    this.positionX = 0,
    this.positionY = 0,
    this.width = 500,
    this.height = 400,
    this.isActive = true,
    this.isLocked = false,
  });

  factory Area.fromJson(Map<String, dynamic> json) {
    return Area(
      areaId: json['areaId'] ?? json['area_id'] ?? 0,
      areaName: json['areaName'] ?? json['area_name'] ?? '',
      description: json['description'],
      positionX: json['positionX'] ?? json['position_x'] ?? 0,
      positionY: json['positionY'] ?? json['position_y'] ?? 0,
      width: json['width'] ?? 500,
      height: json['height'] ?? 400,
      isActive: json['isActive'] ?? json['is_active'] ?? true,
      isLocked: json['isLocked'] ?? json['is_locked'] ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'areaId': areaId,
      'areaName': areaName,
      'description': description,
      'positionX': positionX,
      'positionY': positionY,
      'width': width,
      'height': height,
      'isActive': isActive,
      'isLocked': isLocked,
    };
  }
}
