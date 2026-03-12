class SeatStatusReport {
  final String id;
  final String? reporterName;
  final String? reporterCode;
  final int seatId;
  final String seatCode;
  final String? zoneName;
  final String? areaName;
  final String issueType;
  final String issueTypeLabel;
  final String? description;
  final String? imageUrl;
  final String status;
  final String? verifiedByName;
  final DateTime createdAt;
  final DateTime? verifiedAt;
  final DateTime? resolvedAt;

  SeatStatusReport({
    required this.id,
    this.reporterName,
    this.reporterCode,
    required this.seatId,
    required this.seatCode,
    this.zoneName,
    this.areaName,
    required this.issueType,
    required this.issueTypeLabel,
    this.description,
    this.imageUrl,
    required this.status,
    this.verifiedByName,
    required this.createdAt,
    this.verifiedAt,
    this.resolvedAt,
  });

  factory SeatStatusReport.fromJson(Map<String, dynamic> json) {
    return SeatStatusReport(
      id: json['id'] ?? '',
      reporterName: json['reporterName'],
      reporterCode: json['reporterCode'],
      seatId: json['seatId'] ?? 0,
      seatCode: json['seatCode'] ?? '',
      zoneName: json['zoneName'],
      areaName: json['areaName'],
      issueType: json['issueType'] ?? '',
      issueTypeLabel: json['issueTypeLabel'] ?? '',
      description: json['description'],
      imageUrl: json['imageUrl'],
      status: json['status'] ?? 'PENDING',
      verifiedByName: json['verifiedByName'],
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'])
          : DateTime.now(),
      verifiedAt: json['verifiedAt'] != null
          ? DateTime.parse(json['verifiedAt'])
          : null,
      resolvedAt: json['resolvedAt'] != null
          ? DateTime.parse(json['resolvedAt'])
          : null,
    );
  }

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
}
