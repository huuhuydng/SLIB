class AuthResponse {
  final String id; 
  final String accessToken;
  final String email;
  final String fullName;
  final String studentCode;
  final String role;

  AuthResponse({
    required this.id, 
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
      id: user['id'] as String? ?? '', 
      accessToken: token,
      email: user['email'],
      fullName: meta['full_name'] as String,
      studentCode: meta['student_code'] as String,
      role: meta['role'] as String,
    );
  }
}