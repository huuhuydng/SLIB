class ViolationReport {
  final String id;
  final String? reporterName;
  final String? reporterCode;
  final int seatId;
  final String seatCode;
  final String? zoneName;
  final String? areaName;
  final String violationType;
  final String violationTypeLabel;
  final String? description;
  final String? evidenceUrl;
  final String status;
  final String? verifiedByName;
  final int? pointDeducted;
  final DateTime createdAt;
  final DateTime? verifiedAt;

  ViolationReport({
    required this.id,
    this.reporterName,
    this.reporterCode,
    required this.seatId,
    required this.seatCode,
    this.zoneName,
    this.areaName,
    required this.violationType,
    required this.violationTypeLabel,
    this.description,
    this.evidenceUrl,
    required this.status,
    this.verifiedByName,
    this.pointDeducted,
    required this.createdAt,
    this.verifiedAt,
  });

  factory ViolationReport.fromJson(Map<String, dynamic> json) {
    return ViolationReport(
      id: json['id'] ?? '',
      reporterName: json['reporterName'],
      reporterCode: json['reporterCode'],
      seatId: json['seatId'] ?? 0,
      seatCode: json['seatCode'] ?? '',
      zoneName: json['zoneName'],
      areaName: json['areaName'],
      violationType: json['violationType'] ?? '',
      violationTypeLabel: json['violationTypeLabel'] ?? '',
      description: json['description'],
      evidenceUrl: json['evidenceUrl'],
      status: json['status'] ?? 'PENDING',
      verifiedByName: json['verifiedByName'],
      pointDeducted: json['pointDeducted'],
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'])
          : DateTime.now(),
      verifiedAt: json['verifiedAt'] != null
          ? DateTime.parse(json['verifiedAt'])
          : null,
    );
  }

  /// Lấy label trạng thái tiếng Việt
  String get statusLabel {
    switch (status) {
      case 'PENDING':
        return 'Chờ xử lý';
      case 'VERIFIED':
        return 'Đã xác minh';
      case 'RESOLVED':
        return 'Đã xử lý';
      case 'REJECTED':
        return 'Bị từ chối';
      default:
        return status;
    }
  }

  /// Lấy loại vi phạm tiếng Việt
  static String getViolationLabel(String type) {
    switch (type) {
      case 'UNAUTHORIZED_USE':
        return 'Sử dụng ghế không đúng';
      case 'LEFT_BELONGINGS':
        return 'Để đồ giữ chỗ';
      case 'NOISE':
        return 'Gây ồn ào';
      case 'FEET_ON_SEAT':
        return 'Gác chân lên ghế/bàn';
      case 'FOOD_DRINK':
        return 'Ăn uống trong thư viện';
      case 'SLEEPING':
        return 'Ngủ tại chỗ ngồi';
      case 'OTHER':
        return 'Khác';
      default:
        return type;
    }
  }
}
