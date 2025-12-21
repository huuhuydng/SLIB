class AuthResponse {
  final String accessToken;
  final String email;
  final String fullName;
  final String studentCode;
  final String role; 

  AuthResponse({
    required this.accessToken,
    required this.email,
    required this.fullName,
    required this.studentCode,
    required this.role,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    final token = json['access_token'] as String? ?? '';
    final user = json['user'] as Map<String, dynamic>? ?? {};
    final meta = user['user_metadata'] as Map<String, dynamic>? ?? {};

    return AuthResponse(
      accessToken: token,
      email: user['email'] as String? ?? '', 
      fullName: meta['full_name'] as String? ?? 'Chưa cập nhật',
      studentCode: meta['student_code'] as String? ?? 'Chưa có MSSV',
      role: meta['role'] as String,
    );
  }
}