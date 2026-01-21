/// Model Amenity (Tiện ích của Zone)
class Amenity {
  final int amenityId;
  final int zoneId;
  final String amenityName;

  Amenity({
    required this.amenityId,
    required this.zoneId,
    required this.amenityName,
  });

  factory Amenity.fromJson(Map<String, dynamic> json) {
    return Amenity(
      amenityId: json['amenityId'] ?? json['amenity_id'] ?? 0,
      zoneId: json['zoneId'] ?? json['zone_id'] ?? 0,
      amenityName: json['amenityName'] ?? json['amenity_name'] ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'amenityId': amenityId,
      'zoneId': zoneId,
      'amenityName': amenityName,
    };
  }

  /// Get icon based on amenity name
  String get iconName {
    final lowerName = amenityName.toLowerCase();
    if (lowerName.contains('wifi')) return 'wifi';
    if (lowerName.contains('điện') || lowerName.contains('ổ cắm') || lowerName.contains('power')) return 'power';
    if (lowerName.contains('điều hòa') || lowerName.contains('máy lạnh') || lowerName.contains('ac')) return 'ac_unit';
    if (lowerName.contains('đèn') || lowerName.contains('ánh sáng') || lowerName.contains('light')) return 'light';
    if (lowerName.contains('quạt') || lowerName.contains('fan')) return 'air';
    if (lowerName.contains('nước') || lowerName.contains('water')) return 'water_drop';
    if (lowerName.contains('yên tĩnh') || lowerName.contains('quiet')) return 'volume_off';
    return 'check_circle';
  }
}
