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
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.notification.PushNotificationService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.notification.NotificationController;

/**
 * Unit Tests for FE-55: Config Notification
 * Test Report: doc/Report/UnitTestReport/FE55_TestReport.md
 */
@WebMvcTest(value = NotificationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-55: Config Notification - Unit Tests")
class FE55_ConfigNotificationTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PushNotificationService pushNotificationService;

        @MockBean
        private UserRepository userRepository;

        @Autowired
        private ObjectMapper objectMapper;

        private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

        // =========================================
        // === UTCID01: Update settings - all fields ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Update notification settings with all fields returns 200 OK")
        void updateSettings_allFields_returns200OK() throws Exception {
                User mockUser = new User();
                mockUser.setId(TEST_USER_ID);
                mockUser.setNotifyBooking(true);
                mockUser.setNotifyReminder(true);
                mockUser.setNotifyNews(false);

                when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(mockUser));
                when(userRepository.save(any(User.class))).thenReturn(mockUser);

                String requestBody = """
                        {"notifyBooking": true, "notifyReminder": true, "notifyNews": false}
                        """;

                mockMvc.perform(put("/slib/notifications/settings/" + TEST_USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.notifyBooking").value(true))
                                .andExpect(jsonPath("$.notifyReminder").value(true))
                                .andExpect(jsonPath("$.notifyNews").value(false));

                verify(userRepository, times(1)).findById(TEST_USER_ID);
                verify(userRepository, times(1)).save(any(User.class));
        }

        // =========================================
        // === UTCID02: Get settings - existing user ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get notification settings for existing user returns 200 OK")
        void getSettings_existingUser_returns200OK() throws Exception {
                User mockUser = new User();
                mockUser.setId(TEST_USER_ID);
                mockUser.setNotifyBooking(true);
                mockUser.setNotifyReminder(false);
                mockUser.setNotifyNews(true);

                when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(mockUser));

                mockMvc.perform(get("/slib/notifications/settings/" + TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.notifyBooking").value(true))
                                .andExpect(jsonPath("$.notifyReminder").value(false))
                                .andExpect(jsonPath("$.notifyNews").value(true));

                verify(userRepository, times(1)).findById(TEST_USER_ID);
        }

        // =========================================
        // === UTCID03: Get settings - nullable fields default to true ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get settings with null fields defaults to true")
        void getSettings_nullFields_defaultsToTrue() throws Exception {
                User mockUser = new User();
                mockUser.setId(TEST_USER_ID);
                mockUser.setNotifyBooking(null);
                mockUser.setNotifyReminder(null);
                mockUser.setNotifyNews(null);

                when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(mockUser));

                mockMvc.perform(get("/slib/notifications/settings/" + TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.notifyBooking").value(true))
                                .andExpect(jsonPath("$.notifyReminder").value(true))
                                .andExpect(jsonPath("$.notifyNews").value(true));

                verify(userRepository, times(1)).findById(TEST_USER_ID);
        }

        // =========================================
        // === UTCID04: Partial update - only some fields ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Partial update notification settings returns 200 OK")
        void updateSettings_partialFields_returns200OK() throws Exception {
                User mockUser = new User();
                mockUser.setId(TEST_USER_ID);
                mockUser.setNotifyBooking(true);
                mockUser.setNotifyReminder(true);
                mockUser.setNotifyNews(true);

                when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(mockUser));
                when(userRepository.save(any(User.class))).thenReturn(mockUser);

                String requestBody = """
                        {"notifyBooking": false}
                        """;

                mockMvc.perform(put("/slib/notifications/settings/" + TEST_USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.notifyBooking").value(false));

                verify(userRepository, times(1)).findById(TEST_USER_ID);
                verify(userRepository, times(1)).save(any(User.class));
        }

        // =========================================
        // === UTCID05: User not found ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Update settings for non-existent user returns 500")
        void updateSettings_userNotFound_returns500() throws Exception {
                UUID nonExistentId = UUID.fromString("99999999-9999-9999-9999-999999999999");

                when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

                String requestBody = """
                        {"notifyBooking": true}
                        """;

                mockMvc.perform(put("/slib/notifications/settings/" + nonExistentId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isInternalServerError());

                verify(userRepository, times(1)).findById(nonExistentId);
                verify(userRepository, never()).save(any(User.class));
        }
}
