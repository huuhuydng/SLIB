class StudentProfile {
  final String userId;
  final int reputationScore;
  final double totalStudyHours;
  final int violationCount;

  StudentProfile({
    required this.userId,
    required this.reputationScore,
    required this.totalStudyHours,
    required this.violationCount,
  });

  factory StudentProfile.fromJson(Map<String, dynamic> json) {
    return StudentProfile(
      userId: json['userId'] ?? '',
      reputationScore: json['reputationScore'] ?? 100,
      totalStudyHours: (json['totalStudyHours'] ?? 0).toDouble(),
      violationCount: json['violationCount'] ?? 0,
    );
  }

  /// Format study hours as "XX.Xh"
  String get formattedStudyHours {
    return "${totalStudyHours.toStringAsFixed(1)}h";
  }
}
