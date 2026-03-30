class UserSetting {
  final String userId;
  final bool isHceEnabled;
  final bool isAiRecommendEnabled;
  final bool isBookingRemindEnabled;
  final String themeMode;
  final String languageCode;

  UserSetting({
    required this.userId,
    required this.isHceEnabled,
    required this.isAiRecommendEnabled,
    required this.isBookingRemindEnabled,
    required this.themeMode,
    required this.languageCode,
  });

  factory UserSetting.fromJson(Map<String, dynamic> json) {
    return UserSetting(
      userId: json['userId']?.toString() ?? '',
      isHceEnabled: json['isHceEnabled'] ?? false,
      isAiRecommendEnabled: json['isAiRecommendEnabled'] ?? true,
      isBookingRemindEnabled: json['isBookingRemindEnabled'] ?? true,
      themeMode: json['themeMode'] ?? 'light',
      languageCode: json['languageCode'] ?? 'vi',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'userId': userId,
      'isHceEnabled': isHceEnabled,
      'isAiRecommendEnabled': isAiRecommendEnabled,
      'isBookingRemindEnabled': isBookingRemindEnabled,
      'themeMode': themeMode,
      'languageCode': languageCode,
    };
  }

  UserSetting copyWith({
    bool? isHceEnabled,
    bool? isAiRecommendEnabled,
    bool? isBookingRemindEnabled,
    String? themeMode,
    String? languageCode,
  }) {
    return UserSetting(
      userId: userId,
      isHceEnabled: isHceEnabled ?? this.isHceEnabled,
      isAiRecommendEnabled: isAiRecommendEnabled ?? this.isAiRecommendEnabled,
      isBookingRemindEnabled:
          isBookingRemindEnabled ?? this.isBookingRemindEnabled,
      themeMode: themeMode ?? this.themeMode,
      languageCode: languageCode ?? this.languageCode,
    );
  }
}
