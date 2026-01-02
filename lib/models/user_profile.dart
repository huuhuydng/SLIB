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
  
  // 👉 1. THÊM FIELD NÀY
  final String? notiDevice;       // FCM Token

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
    
    // 👉 2. THÊM VÀO CONSTRUCTOR
    this.notiDevice,
  });

  // Factory parse JSON (Khớp với cột SQL)
  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id'] ?? '',
      
      // Hỗ trợ cả 2 kiểu key (camelCase từ Java Spring, snake_case từ SQL raw)
      supabaseUid: json['supabaseUid'] ?? json['supabase_uid'],
      studentCode: json['studentCode'] ?? json['student_code'] ?? '',
      fullName: json['fullName'] ?? json['full_name'] ?? '',
      email: json['email'],
      role: json['role'] ?? 'student',
      
      // Parse số an toàn (phòng trường hợp json trả về chuỗi)
      reputationScore: json['reputationScore'] is int 
          ? json['reputationScore'] 
          : (json['reputation_score'] is int 
              ? json['reputation_score'] 
              : int.tryParse(json['reputation_score'].toString()) ?? 100),
              
      isActive: json['isActive'] ?? json['is_active'] ?? true,

      // 👉 3. MAP JSON (Ưu tiên snake_case vì Backend trả về entity DB)
      notiDevice: json['notiDevice'] ?? json['noti_device'],
    );
  }

  // Helper check quyền tiện lợi
  bool get isStudent => role == 'student';
  bool get isAdmin => role == 'admin';
  bool get isLibrarian => role == 'librarian';
  
  // Thêm toJson nếu cần debug hoặc gửi ngược lên server
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'supabase_uid': supabaseUid,
      'student_code': studentCode,
      'full_name': fullName,
      'email': email,
      'role': role,
      'reputation_score': reputationScore,
      'is_active': isActive,
      'noti_device': notiDevice,
    };
  }
}