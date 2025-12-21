class UserProfile {
  final String id;
  final String fullName;
  final String email;
  final String studentCode;
  final String role;
  final int reputationScore; // 👈 Quan trọng nhất

  UserProfile({
    required this.id,
    required this.fullName,
    required this.email,
    required this.studentCode,
    required this.role,
    required this.reputationScore,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id'] ?? '',
      fullName: json['fullName'],
      email: json['email'],
      studentCode: json['studentCode'],
      role: json['role'],
      reputationScore: json['reputationScore'], 
    );
  }
}