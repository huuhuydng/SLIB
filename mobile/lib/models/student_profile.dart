class BookingRestrictionStatus {
  final bool allowedNow;
  final String? restrictionReason;
  final String? policyHint;
  final DateTime? blockedUntil;
  final int? remainingDays;
  final int? remainingHours;

  BookingRestrictionStatus({
    required this.allowedNow,
    this.restrictionReason,
    this.policyHint,
    this.blockedUntil,
    this.remainingDays,
    this.remainingHours,
  });

  factory BookingRestrictionStatus.fromJson(Map<String, dynamic> json) {
    return BookingRestrictionStatus(
      allowedNow: json['allowedNow'] ?? true,
      restrictionReason: json['restrictionReason'],
      policyHint: json['policyHint'],
      blockedUntil: json['blockedUntil'] != null
          ? DateTime.tryParse(json['blockedUntil'])
          : null,
      remainingDays: json['remainingDays'],
      remainingHours: json['remainingHours'],
    );
  }

  bool get hasNotice =>
      (restrictionReason?.isNotEmpty ?? false) ||
      (policyHint?.isNotEmpty ?? false);

  bool get isTemporarilyBlocked => !allowedNow && blockedUntil != null;

  String? get summaryMessage {
    if (restrictionReason?.isNotEmpty ?? false) {
      return restrictionReason;
    }
    if (policyHint?.isNotEmpty ?? false) {
      return policyHint;
    }
    return null;
  }

  String? get remainingText {
    final days = remainingDays ?? 0;
    final hours = remainingHours ?? 0;
    if (days <= 0 && hours <= 0) return null;
    if (days > 0 && hours > 0) return '$days ngày $hours giờ';
    if (days > 0) return '$days ngày';
    return '$hours giờ';
  }
}

class StudentProfile {
  final String userId;
  final int reputationScore;
  final double totalStudyHours;
  final int violationCount;
  final int totalBookings;
  final BookingRestrictionStatus? bookingRestriction;

  StudentProfile({
    required this.userId,
    required this.reputationScore,
    required this.totalStudyHours,
    required this.violationCount,
    required this.totalBookings,
    this.bookingRestriction,
  });

  factory StudentProfile.fromJson(Map<String, dynamic> json) {
    return StudentProfile(
      userId: json['userId'] ?? '',
      reputationScore: json['reputationScore'] ?? 100,
      totalStudyHours: (json['totalStudyHours'] ?? 0).toDouble(),
      violationCount: json['violationCount'] ?? 0,
      totalBookings: json['totalBookings'] ?? 0,
      bookingRestriction: json['bookingRestriction'] is Map<String, dynamic>
          ? BookingRestrictionStatus.fromJson(json['bookingRestriction'])
          : null,
    );
  }

  /// Format study hours as "XX.Xh"
  String get formattedStudyHours {
    return "${totalStudyHours.toStringAsFixed(1)}h";
  }
}
