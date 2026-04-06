class UserProfile {
  final String id;
  final String userCode; // Renamed from studentCode
  final String? username;
  final String fullName;
  final String? email;
  final String role;
  final int reputationScore;
  final bool isActive;
  final DateTime? createdAt;
  final DateTime? updatedAt;
  final String? notiDevice;
  final String? dob; // Date of birth as string (yyyy-MM-dd)
  final String? phone;
  final String? avtUrl;
  final bool passwordChanged;

  UserProfile({
    required this.id,
    required this.userCode,
    this.username,
    required this.fullName,
    this.email,
    required this.role,
    this.reputationScore = 100,
    this.isActive = true,
    this.createdAt,
    this.updatedAt,
    this.notiDevice,
    this.dob,
    this.phone,
    this.avtUrl,
    this.passwordChanged = true, // Default true for existing users
  });

  // Backward compatibility getter
  String get studentCode => userCode;

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id'] ?? '',
      userCode:
          json['userCode'] ??
          json['user_code'] ??
          json['studentCode'] ??
          json['student_code'] ??
          '',
      username: json['username'],
      fullName: json['fullName'] ?? json['full_name'] ?? '',
      email: json['email'],
      role: json['role'] ?? 'STUDENT',
      reputationScore: json['reputationScore'] is int
          ? json['reputationScore']
          : (json['reputation_score'] is int
                ? json['reputation_score']
                : int.tryParse(json['reputation_score']?.toString() ?? '100') ??
                      100),
      isActive: json['isActive'] ?? json['is_active'] ?? true,
      notiDevice: json['notiDevice'] ?? json['noti_device'],
      dob: json['dob'],
      phone: json['phone'],
      avtUrl: json['avtUrl'] ?? json['avt_url'],
      passwordChanged:
          json['passwordChanged'] ?? json['password_changed'] ?? true,
    );
  }

  String get normalizedRole => role.toUpperCase();
  bool get isStudent => normalizedRole == 'STUDENT';
  bool get isTeacher => normalizedRole == 'TEACHER';
  bool get isAdmin => normalizedRole == 'ADMIN';
  bool get isLibrarian => normalizedRole == 'LIBRARIAN';
  bool get isPatron => isStudent || isTeacher;
  bool get needsPasswordChange => !passwordChanged;

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'user_code': userCode,
      'username': username,
      'full_name': fullName,
      'email': email,
      'role': role,
      'reputation_score': reputationScore,
      'is_active': isActive,
      'noti_device': notiDevice,
      'dob': dob,
      'phone': phone,
      'avt_url': avtUrl,
      'password_changed': passwordChanged,
    };
  }

  // Copy with method for updates
  UserProfile copyWith({
    String? id,
    String? userCode,
    String? username,
    String? fullName,
    String? email,
    String? role,
    int? reputationScore,
    bool? isActive,
    DateTime? createdAt,
    DateTime? updatedAt,
    String? notiDevice,
    String? dob,
    String? phone,
    String? avtUrl,
    bool? passwordChanged,
  }) {
    return UserProfile(
      id: id ?? this.id,
      userCode: userCode ?? this.userCode,
      username: username ?? this.username,
      fullName: fullName ?? this.fullName,
      email: email ?? this.email,
      role: role ?? this.role,
      reputationScore: reputationScore ?? this.reputationScore,
      isActive: isActive ?? this.isActive,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      notiDevice: notiDevice ?? this.notiDevice,
      dob: dob ?? this.dob,
      phone: phone ?? this.phone,
      avtUrl: avtUrl ?? this.avtUrl,
      passwordChanged: passwordChanged ?? this.passwordChanged,
    );
  }
}
