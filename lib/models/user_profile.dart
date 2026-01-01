class UserProfile {
  final String id;                // UUID
  final String? supabaseUid;      // Link với Auth
  final String studentCode;       // DE180295
  final String fullName;          // Tên hiển thị
  final String? email;
  final String role;              // 'student', 'librarian', 'admin'
  final int reputationScore;      // 100
  final bool isActive;            // true/false
  final DateTime? createdAt;
  final DateTime? updatedAt;

  UserProfile({
    required this.id,
    this.supabaseUid,
    required this.studentCode,
    required this.fullName,
    this.email,
    required this.role,
    this.reputationScore = 100, 
    this.isActive = true,      
    this.createdAt,
    this.updatedAt,
  });

  // Factory parse JSON (Khớp với cột SQL)
  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id'] ?? '',
      
      // 👉 SỬA Ở ĐÂY: Hỗ trợ cả 2 kiểu key (camelCase từ Java Spring, snake_case từ SQL raw nếu có)
      supabaseUid: json['supabaseUid'] ?? json['supabase_uid'],
      studentCode: json['studentCode'] ?? json['student_code'] ?? '',
      fullName: json['fullName'] ?? json['full_name'] ?? '',
      email: json['email'],
      role: json['role'] ?? 'student',
      
      // Parse số an toàn
      reputationScore: (json['reputationScore'] ?? json['reputation_score'] ?? 100) as int,
      isActive: json['isActive'] ?? json['is_active'] ?? true,
    );
  }

  // Helper check quyền tiện lợi
  bool get isStudent => role == 'student';
  bool get isAdmin => role == 'admin';
  bool get isLibrarian => role == 'librarian';
}