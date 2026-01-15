package slib.com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.dto.UserSettingDTO;
import slib.com.example.entity.users.UserSetting;
import slib.com.example.service.UserSettingService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for UserSettingController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 * Test Type: @WebMvcTest (Unit Tests only - no full context)
 */
@WebMvcTest(value = UserSettingController.class, 
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, 
    classes = {slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserSettingController Unit Tests")
class UserSettingControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserSettingService userSettingService;

    @Autowired
    private ObjectMapper objectMapper;

    // =============================================
    // === GET USER SETTINGS ENDPOINT ===
    // =============================================

    @Test
    @DisplayName("getUserSettings_validUserId_returns200WithSettings")
    void getUserSettings_validUserId_returns200WithSettings() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserSetting settings = createUserSetting(userId, true, true, false, "DARK", "vi");

        when(userSettingService.getSettings(userId)).thenReturn(settings);

        // Act & Assert
        mockMvc.perform(get("/slib/settings/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.isHceEnabled").value(true))
                .andExpect(jsonPath("$.isAiRecommendEnabled").value(true))
                .andExpect(jsonPath("$.isBookingRemindEnabled").value(false))
                .andExpect(jsonPath("$.themeMode").value("DARK"))
                .andExpect(jsonPath("$.languageCode").value("vi"));

        verify(userSettingService, times(1)).getSettings(userId);
    }

    @Test
    @DisplayName("getUserSettings_userNotFound_throwsRuntimeException")
    void getUserSettings_userNotFound_throwsRuntimeException() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userSettingService.getSettings(userId))
                .thenThrow(new RuntimeException("User settings not found"));

        // Act & Assert
        mockMvc.perform(get("/slib/settings/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(userSettingService, times(1)).getSettings(userId);
    }

    @Test
    @DisplayName("getUserSettings_invalidUUID_returns400")
    void getUserSettings_invalidUUID_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/slib/settings/{userId}", "invalid-uuid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(userSettingService, never()).getSettings(any());
    }

    // =============================================
    // === UPDATE USER SETTINGS ENDPOINT ===
    // =============================================

    @Test
    @DisplayName("updateUserSettings_validData_returns200WithUpdatedSettings")
    void updateUserSettings_validData_returns200WithUpdatedSettings() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        
        UserSettingDTO settingDTO = new UserSettingDTO();
        settingDTO.setIsHceEnabled(false);
        settingDTO.setIsAiRecommendEnabled(true);
        settingDTO.setIsBookingRemindEnabled(true);
        settingDTO.setThemeMode("LIGHT");
        settingDTO.setLanguageCode("en");

        UserSetting updatedSettings = createUserSetting(userId, false, true, true, "LIGHT", "en");

        when(userSettingService.updateSettings(eq(userId), any(UserSettingDTO.class)))
                .thenReturn(updatedSettings);

        // Act & Assert
        mockMvc.perform(put("/slib/settings/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.isHceEnabled").value(false))
                .andExpect(jsonPath("$.isAiRecommendEnabled").value(true))
                .andExpect(jsonPath("$.isBookingRemindEnabled").value(true))
                .andExpect(jsonPath("$.themeMode").value("LIGHT"))
                .andExpect(jsonPath("$.languageCode").value("en"));

        verify(userSettingService, times(1)).updateSettings(eq(userId), any(UserSettingDTO.class));
    }

    @Test
    @DisplayName("updateUserSettings_partialUpdate_returns200")
    void updateUserSettings_partialUpdate_returns200() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        
        UserSettingDTO settingDTO = new UserSettingDTO();
        settingDTO.setThemeMode("AUTO");
        // Other fields are null (partial update)

        UserSetting updatedSettings = createUserSetting(userId, true, true, false, "AUTO", "vi");

        when(userSettingService.updateSettings(eq(userId), any(UserSettingDTO.class)))
                .thenReturn(updatedSettings);

        // Act & Assert
        mockMvc.perform(put("/slib/settings/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeMode").value("AUTO"));

        verify(userSettingService, times(1)).updateSettings(eq(userId), any(UserSettingDTO.class));
    }

    @Test
    @DisplayName("updateUserSettings_emptyRequestBody_returns400")
    void updateUserSettings_emptyRequestBody_returns400() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(put("/slib/settings/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        verify(userSettingService, never()).updateSettings(any(), any());
    }

    @Test
    @DisplayName("updateUserSettings_invalidJson_returns400")
    void updateUserSettings_invalidJson_returns400() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(put("/slib/settings/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verify(userSettingService, never()).updateSettings(any(), any());
    }

    @Test
    @DisplayName("updateUserSettings_invalidUUID_returns400")
    void updateUserSettings_invalidUUID_returns400() throws Exception {
        // Arrange
        UserSettingDTO settingDTO = new UserSettingDTO();
        settingDTO.setThemeMode("DARK");

        // Act & Assert
        mockMvc.perform(put("/slib/settings/{userId}", "invalid-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingDTO)))
                .andExpect(status().isBadRequest());

        verify(userSettingService, never()).updateSettings(any(), any());
    }

    @Test
    @DisplayName("updateUserSettings_userNotFound_throwsRuntimeException")
    void updateUserSettings_userNotFound_throwsRuntimeException() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        
        UserSettingDTO settingDTO = new UserSettingDTO();
        settingDTO.setThemeMode("DARK");

        when(userSettingService.updateSettings(eq(userId), any(UserSettingDTO.class)))
                .thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        mockMvc.perform(put("/slib/settings/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingDTO)))
                .andExpect(status().isInternalServerError());

        verify(userSettingService, times(1)).updateSettings(eq(userId), any(UserSettingDTO.class));
    }

    @Test
    @DisplayName("updateUserSettings_allFeaturesEnabled_returns200")
    void updateUserSettings_allFeaturesEnabled_returns200() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        
        UserSettingDTO settingDTO = new UserSettingDTO();
        settingDTO.setIsHceEnabled(true);
        settingDTO.setIsAiRecommendEnabled(true);
        settingDTO.setIsBookingRemindEnabled(true);
        settingDTO.setThemeMode("DARK");
        settingDTO.setLanguageCode("en");

        UserSetting updatedSettings = createUserSetting(userId, true, true, true, "DARK", "en");

        when(userSettingService.updateSettings(eq(userId), any(UserSettingDTO.class)))
                .thenReturn(updatedSettings);

        // Act & Assert
        mockMvc.perform(put("/slib/settings/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isHceEnabled").value(true))
                .andExpect(jsonPath("$.isAiRecommendEnabled").value(true))
                .andExpect(jsonPath("$.isBookingRemindEnabled").value(true));

        verify(userSettingService, times(1)).updateSettings(eq(userId), any(UserSettingDTO.class));
    }

    @Test
    @DisplayName("updateUserSettings_allFeaturesDisabled_returns200")
    void updateUserSettings_allFeaturesDisabled_returns200() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        
        UserSettingDTO settingDTO = new UserSettingDTO();
        settingDTO.setIsHceEnabled(false);
        settingDTO.setIsAiRecommendEnabled(false);
        settingDTO.setIsBookingRemindEnabled(false);
        settingDTO.setThemeMode("LIGHT");
        settingDTO.setLanguageCode("vi");

        UserSetting updatedSettings = createUserSetting(userId, false, false, false, "LIGHT", "vi");

        when(userSettingService.updateSettings(eq(userId), any(UserSettingDTO.class)))
                .thenReturn(updatedSettings);

        // Act & Assert
        mockMvc.perform(put("/slib/settings/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isHceEnabled").value(false))
                .andExpect(jsonPath("$.isAiRecommendEnabled").value(false))
                .andExpect(jsonPath("$.isBookingRemindEnabled").value(false));

        verify(userSettingService, times(1)).updateSettings(eq(userId), any(UserSettingDTO.class));
    }

    // ==========================================
    // === HELPER METHOD TO CREATE TEST DATA ===
    // ==========================================

    /**
     * Helper method to create UserSetting objects for testing
     */
    private UserSetting createUserSetting(UUID userId, Boolean isHceEnabled, Boolean isAiRecommendEnabled,
                                          Boolean isBookingRemindEnabled, String themeMode, String languageCode) {
        UserSetting setting = new UserSetting();
        setting.setUserId(userId);
        setting.setIsHceEnabled(isHceEnabled);
        setting.setIsAiRecommendEnabled(isAiRecommendEnabled);
        setting.setIsBookingRemindEnabled(isBookingRemindEnabled);
        setting.setThemeMode(themeMode);
        setting.setLanguageCode(languageCode);
        return setting;
    }
}
