class UserProfile {
  final String id;                
  final String studentCode;       
  final String fullName;         
  final String? email;
  final String role;          
  final int reputationScore;      
  final bool isActive;          
  final DateTime? createdAt;
  final DateTime? updatedAt;
  

  final String? notiDevice;      

  UserProfile({
    required this.id,
    required this.studentCode,
    required this.fullName,
    this.email,
    required this.role,
    this.reputationScore = 100, 
    this.isActive = true,      
    this.createdAt,
    this.updatedAt,
    
    this.notiDevice,
  });


  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id'] ?? '',
      studentCode: json['studentCode'] ?? json['student_code'] ?? '',
      fullName: json['fullName'] ?? json['full_name'] ?? '',
      email: json['email'],
      role: json['role'] ?? 'student',

      reputationScore: json['reputationScore'] is int 
          ? json['reputationScore'] 
          : (json['reputation_score'] is int 
              ? json['reputation_score'] 
              : int.tryParse(json['reputation_score'].toString()) ?? 100),
              
      isActive: json['isActive'] ?? json['is_active'] ?? true,

      notiDevice: json['notiDevice'] ?? json['noti_device'],
    );
  }

  bool get isStudent => role == 'student';
  bool get isAdmin => role == 'admin';
  bool get isLibrarian => role == 'librarian';
  
  Map<String, dynamic> toJson() {
    return {
      'id': id,
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