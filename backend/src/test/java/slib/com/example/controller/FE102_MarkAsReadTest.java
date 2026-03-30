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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.notification.PushNotificationService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.notification.NotificationController;

/**
 * Unit Tests for FE-102: Mark as Read
 * Test Report: doc/Report/UnitTestReport/FE102_TestReport.md
 */
@WebMvcTest(value = NotificationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-102: Mark as Read - Unit Tests")
class FE102_MarkAsReadTest {

        private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
        private static final String TEST_EMAIL = "student@fpt.edu.vn";

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PushNotificationService pushNotificationService;

        @MockBean
        private UserRepository userRepository;

        private User buildCurrentUser(UUID userId, String email) {
                User user = new User();
                user.setId(userId);
                user.setEmail(email);
                user.setRole(Role.STUDENT);
                return user;
        }

        // =========================================
        // === UTCID01: Normal - mark single notification as read ===
        // =========================================
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID01: Mark single notification as read returns 200 OK")
        void markAsRead_singleNotification_returns200() throws Exception {
                UUID notificationId = UUID.randomUUID();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                doNothing().when(pushNotificationService).markAsRead(eq(notificationId), eq(TEST_USER_ID));

                mockMvc.perform(put("/slib/notifications/mark-read/{notificationId}", notificationId)
                                .param("userId", TEST_USER_ID.toString()))
                                .andExpect(status().isOk());

                verify(pushNotificationService, times(1)).markAsRead(eq(notificationId), eq(TEST_USER_ID));
        }

        // =========================================
        // === UTCID02: Mark all notifications as read ===
        // =========================================
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID02: Mark all notifications as read for user returns 200 OK")
        void markAllAsRead_forUser_returns200() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                doNothing().when(pushNotificationService).markAllAsRead(eq(TEST_USER_ID));

                mockMvc.perform(put("/slib/notifications/mark-all-read/{userId}", TEST_USER_ID))
                                .andExpect(status().isOk());

                verify(pushNotificationService, times(1)).markAllAsRead(eq(TEST_USER_ID));
        }

        // =========================================
        // === UTCID03: Mark as read for already read notification ===
        // =========================================
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID03: Mark already-read notification returns 200 OK (idempotent)")
        void markAsRead_alreadyRead_returns200() throws Exception {
                UUID notificationId = UUID.randomUUID();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                doNothing().when(pushNotificationService).markAsRead(eq(notificationId), eq(TEST_USER_ID));

                mockMvc.perform(put("/slib/notifications/mark-read/{notificationId}", notificationId)
                                .param("userId", TEST_USER_ID.toString()))
                                .andExpect(status().isOk());

                verify(pushNotificationService, times(1)).markAsRead(eq(notificationId), eq(TEST_USER_ID));
        }

        // =========================================
        // === UTCID04: Mark as read for non-existent notification ===
        // =========================================
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID04: Mark non-existent notification as read returns error")
        void markAsRead_notFound_returnsError() throws Exception {
                UUID notificationId = UUID.randomUUID();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                doThrow(new RuntimeException("Thong bao khong ton tai"))
                                .when(pushNotificationService).markAsRead(eq(notificationId), eq(TEST_USER_ID));

                mockMvc.perform(put("/slib/notifications/mark-read/{notificationId}", notificationId)
                                .param("userId", TEST_USER_ID.toString()))
                                .andExpect(status().isInternalServerError());

                verify(pushNotificationService, times(1)).markAsRead(eq(notificationId), eq(TEST_USER_ID));
        }

        // =========================================
        // === UTCID05: Mark all as read - service failure ===
        // =========================================
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID05: Mark all as read when service fails returns error")
        void markAllAsRead_serviceFails_returnsError() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                doThrow(new RuntimeException("Loi cap nhat co so du lieu"))
                                .when(pushNotificationService).markAllAsRead(eq(TEST_USER_ID));

                mockMvc.perform(put("/slib/notifications/mark-all-read/{userId}", TEST_USER_ID))
                                .andExpect(status().isInternalServerError());

                verify(pushNotificationService, times(1)).markAllAsRead(eq(TEST_USER_ID));
        }
}
