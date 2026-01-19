/// Model Factory (Vật cản) - tương ứng với AreaFactory từ Admin config
class AreaFactory {
  final int factoryId;
  final int areaId;
  final String factoryName;
  final int positionX;
  final int positionY;
  final int width;
  final int height;
  final bool isLocked;

  AreaFactory({
    required this.factoryId,
    required this.areaId,
    required this.factoryName,
    this.positionX = 0,
    this.positionY = 0,
    this.width = 100,
    this.height = 100,
    this.isLocked = false,
  });

  factory AreaFactory.fromJson(Map<String, dynamic> json) {
    return AreaFactory(
      factoryId: json['factoryId'] ?? json['factory_id'] ?? 0,
      areaId: json['areaId'] ?? json['area_id'] ?? 0,
      factoryName: json['factoryName'] ?? json['factory_name'] ?? 'Vật cản',
      positionX: json['positionX'] ?? json['position_x'] ?? 0,
      positionY: json['positionY'] ?? json['position_y'] ?? 0,
      width: json['width'] ?? 100,
      height: json['height'] ?? 100,
      isLocked: json['isLocked'] ?? json['is_locked'] ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'factoryId': factoryId,
      'areaId': areaId,
      'factoryName': factoryName,
      'positionX': positionX,
      'positionY': positionY,
      'width': width,
      'height': height,
      'isLocked': isLocked,
    };
  }
}
