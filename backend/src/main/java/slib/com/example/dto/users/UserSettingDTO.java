package slib.com.example.dto.users;

import lombok.Data;

@Data
public class UserSettingDTO {
    private Boolean isHceEnabled;
    private Boolean isAiRecommendEnabled;
    private Boolean isBookingRemindEnabled;
    private String themeMode;
    private String languageCode;
}