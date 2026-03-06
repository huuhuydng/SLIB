package slib.com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import slib.com.example.dto.users.UserSettingDTO;
import slib.com.example.entity.users.UserSetting;
import slib.com.example.repository.UserSettingRepository;

import java.util.UUID;

@Service
public class UserSettingService {

    @Autowired
    private UserSettingRepository userSettingRepository;

    public UserSetting getSettings(UUID userId) {
        return userSettingRepository.findById(userId)
                .orElseGet(() -> {
                    // Auto-create default settings if not found
                    UserSetting newSetting = UserSetting.builder()
                            .userId(userId)
                            .isHceEnabled(true)
                            .isAiRecommendEnabled(true)
                            .isBookingRemindEnabled(true)
                            .themeMode("light")
                            .languageCode("vi")
                            .build();
                    return userSettingRepository.save(newSetting);
                });
    }

    public UserSetting updateSettings(UUID userId, UserSettingDTO dto) {
        UserSetting setting = getSettings(userId);

        if (dto.getIsHceEnabled() != null)
            setting.setIsHceEnabled(dto.getIsHceEnabled());

        if (dto.getIsAiRecommendEnabled() != null)
            setting.setIsAiRecommendEnabled(dto.getIsAiRecommendEnabled());

        if (dto.getIsBookingRemindEnabled() != null)
            setting.setIsBookingRemindEnabled(dto.getIsBookingRemindEnabled());

        if (dto.getThemeMode() != null)
            setting.setThemeMode(dto.getThemeMode());

        if (dto.getLanguageCode() != null)
            setting.setLanguageCode(dto.getLanguageCode());

        return userSettingRepository.save(setting);
    }
}