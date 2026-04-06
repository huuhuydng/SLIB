class Complaint {
  final String id;
  final String? studentId;
  final String? studentName;
  final String? studentCode;
  final String? studentAvatar;
  final String? pointTransactionId;
  final String? violationReportId;
  final String subject;
  final String content;
  final String? evidenceUrl;
  final String status;
  final String? resolutionNote;
  final String? resolvedByName;
  final DateTime createdAt;
  final DateTime? resolvedAt;

  Complaint({
    required this.id,
    this.studentId,
    this.studentName,
    this.studentCode,
    this.studentAvatar,
    this.pointTransactionId,
    this.violationReportId,
    required this.subject,
    required this.content,
    this.evidenceUrl,
    required this.status,
    this.resolutionNote,
    this.resolvedByName,
    required this.createdAt,
    this.resolvedAt,
  });

  factory Complaint.fromJson(Map<String, dynamic> json) {
    return Complaint(
      id: json['id']?.toString() ?? '',
      studentId: json['studentId']?.toString(),
      studentName: json['studentName'],
      studentCode: json['studentCode'],
      studentAvatar: json['studentAvatar'],
      pointTransactionId: json['pointTransactionId']?.toString(),
      violationReportId: json['violationReportId']?.toString(),
      subject: json['subject'] ?? '',
      content: json['content'] ?? '',
      evidenceUrl: json['evidenceUrl'],
      status: json['status'] ?? 'PENDING',
      resolutionNote: json['resolutionNote'],
      resolvedByName: json['resolvedByName'],
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'])
          : DateTime.now(),
      resolvedAt: json['resolvedAt'] != null
          ? DateTime.parse(json['resolvedAt'])
          : null,
    );
  }

  String get statusLabel {
    switch (status) {
      case 'PENDING':
        return 'Chờ xử lý';
      case 'ACCEPTED':
        return 'Được chấp nhận';
      case 'DENIED':
        return 'Bị từ chối';
      default:
        return status;
    }
  }

  String get targetLabel {
    if (violationReportId != null && violationReportId!.isNotEmpty) {
      return 'Khiếu nại báo cáo vi phạm';
    }
    if (pointTransactionId != null && pointTransactionId!.isNotEmpty) {
      return 'Khiếu nại giao dịch điểm';
    }
    return 'Khiếu nại';
  }
}
