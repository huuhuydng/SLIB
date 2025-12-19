class AuthResponse {
  final String accessToken;
  final String email;
  final String fullName;
  final String studentCode;

  AuthResponse({
    required this.accessToken,
    required this.email,
    required this.fullName,
    required this.studentCode,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    // 1. Lấy Token nằm ngay ngoài cùng
    final token = json['access_token'] ?? '';

    // 2. Lấy thông tin User nằm sâu bên trong
    final user = json['user'] ?? {};
    final meta = user['user_metadata'] ?? {};

    return AuthResponse(
      accessToken: token,
      email: user['email'] ?? '',
      fullName: meta['full_name'] ?? 'Sinh viên', 
      studentCode: meta['student_code'] ?? '',
    );
  }
}