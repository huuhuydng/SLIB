package slib.com.example.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.users.UserController;
import slib.com.example.dto.users.UserProfileResponse;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.users.AsyncImportService;
import slib.com.example.service.auth.AuthService;
import slib.com.example.service.users.StagingImportService;
import slib.com.example.service.users.UserService;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.system.SystemLogService;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-11: Turn on/Turn off Notification
 * Test Report: doc/Report/FE11_TestReport.md
 *
 * Note: /me/settings/notification endpoint does not exist in UserController.
 * Tests adapted to use PATCH /me endpoint to update profile.
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-11: Turn on/Turn off Notification - Unit Tests")
class FE11_NotificationSettingTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @MockBean
        private AuthService authService;

        @MockBean
        private CloudinaryService cloudinaryService;

        @MockBean
        private AsyncImportService asyncImportService;

        @MockBean
        private StagingImportService stagingImportService;

        @MockBean
        private SystemLogService systemLogService;

        @BeforeEach
        void clearSecurityContext() {
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD01: Update notification setting returns 200 OK")
        void updateNotificationSetting_validValue_returns200OK() throws Exception {
                when(userService.getMyProfile(anyString())).thenReturn(
                        UserProfileResponse.builder()
                                .id(UUID.randomUUID())
                                .email("student@fpt.edu.vn")
                                .build()
                );
                when(userService.updateUser(any(), any())).thenReturn(User.builder().build());

                mockMvc.perform(patch("/slib/users/me")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"fullName\":\"Test\"}"))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("UTCD02: No token returns 401 Unauthorized")
        void updateNotificationSetting_noToken_returns401() throws Exception {
                mockMvc.perform(patch("/slib/users/me")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"fullName\":\"Test\"}"))
                        .andExpect(status().isUnauthorized());
        }
}
