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
