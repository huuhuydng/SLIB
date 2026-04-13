package slib.com.example.controller;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.users.UserSettingController;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.entity.users.UserSetting;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.users.UserSettingService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit Tests for FE-13: Turn on/Turn off HCE Feature
 */
@WebMvcTest(value = UserSettingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-13: Turn on/Turn off HCE Feature - Unit Tests")
class FE13_HCESettingTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserSettingService userSettingService;

        @MockBean
        private UserRepository userRepository;

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCID01: Update HCE setting with isHceEnabled false")
        void updateHceSetting_withIsHceEnabledFalse() throws Exception {
                UUID userId = UUID.randomUUID();
                User currentUser = mock(User.class);
                UserSetting updatedSetting = UserSetting.builder()
                                .userId(userId)
                                .isHceEnabled(false)
                                .isAiRecommendEnabled(true)
                                .isBookingRemindEnabled(true)
                                .themeMode("light")
                                .languageCode("vi")
                                .build();

                when(currentUser.getId()).thenReturn(userId);
                when(currentUser.getRole()).thenReturn(Role.STUDENT);
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(currentUser));
                when(userSettingService.updateSettings(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any()))
                                .thenReturn(updatedSetting);

                mockMvc.perform(put("/slib/settings/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "isHceEnabled": false
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isHceEnabled").value(false))
                                .andExpect(jsonPath("$.isAiRecommendEnabled").value(true))
                                .andExpect(jsonPath("$.isBookingRemindEnabled").value(true));
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCID02: Get HCE setting with existing user settings")
        void getHceSetting_withExistingUserSettings() throws Exception {
                UUID userId = UUID.randomUUID();
                User currentUser = mock(User.class);
                UserSetting currentSetting = UserSetting.builder()
                                .userId(userId)
                                .isHceEnabled(true)
                                .isAiRecommendEnabled(false)
                                .isBookingRemindEnabled(true)
                                .themeMode("light")
                                .languageCode("vi")
                                .build();

                when(currentUser.getId()).thenReturn(userId);
                when(currentUser.getRole()).thenReturn(Role.STUDENT);
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(currentUser));
                when(userSettingService.getSettings(userId)).thenReturn(currentSetting);

                mockMvc.perform(get("/slib/settings/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isHceEnabled").value(true))
                                .andExpect(jsonPath("$.isAiRecommendEnabled").value(false))
                                .andExpect(jsonPath("$.languageCode").value("vi"));
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCID03: Update HCE setting with malformed JSON body")
        void updateHceSetting_withMalformedJsonBody() throws Exception {
                UUID userId = UUID.randomUUID();
                User currentUser = mock(User.class);

                when(currentUser.getId()).thenReturn(userId);
                when(currentUser.getRole()).thenReturn(Role.STUDENT);
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(currentUser));

                mockMvc.perform(put("/slib/settings/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "isHceEnabled":
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCID04: Update HCE setting for another user's userId")
        void updateHceSetting_forAnotherUsersUserId() throws Exception {
                UUID currentUserId = UUID.randomUUID();
                UUID requestedUserId = UUID.randomUUID();
                User currentUser = mock(User.class);

                when(currentUser.getId()).thenReturn(currentUserId);
                when(currentUser.getRole()).thenReturn(Role.STUDENT);
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(currentUser));

                mockMvc.perform(put("/slib/settings/{userId}", requestedUserId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "isHceEnabled": true
                                                }
                                                """))
                                .andExpect(status().isForbidden());

                verify(userSettingService, never()).updateSettings(org.mockito.ArgumentMatchers.eq(requestedUserId), org.mockito.ArgumentMatchers.any());
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCID05: Get HCE setting when authenticated user does not exist")
        void getHceSetting_whenAuthenticatedUserDoesNotExist() throws Exception {
                UUID userId = UUID.randomUUID();

                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.empty());

                mockMvc.perform(get("/slib/settings/{userId}", userId))
                                .andExpect(status().isInternalServerError());
        }
}
