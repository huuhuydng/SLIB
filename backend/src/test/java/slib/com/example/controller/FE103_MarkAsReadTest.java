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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.PushNotificationService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-103: Mark as Read
 * Test Report: doc/Report/UnitTestReport/FE103_TestReport.md
 */
@WebMvcTest(value = NotificationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-103: Mark as Read - Unit Tests")
class FE103_MarkAsReadTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PushNotificationService pushNotificationService;

        @MockBean
        private UserRepository userRepository;

        // =========================================
        // === UTCID01: Normal - mark single notification as read ===
        // =========================================
        @Test
        @DisplayName("UTCID01: Mark single notification as read returns 200 OK")
        void markAsRead_singleNotification_returns200() throws Exception {
                UUID notificationId = UUID.randomUUID();

                doNothing().when(pushNotificationService).markAsRead(eq(notificationId));

                mockMvc.perform(put("/slib/notifications/mark-read/{notificationId}", notificationId))
                                .andExpect(status().isOk());

                verify(pushNotificationService, times(1)).markAsRead(eq(notificationId));
        }

        // =========================================
        // === UTCID02: Mark all notifications as read ===
        // =========================================
        @Test
        @DisplayName("UTCID02: Mark all notifications as read for user returns 200 OK")
        void markAllAsRead_forUser_returns200() throws Exception {
                UUID userId = UUID.randomUUID();

                doNothing().when(pushNotificationService).markAllAsRead(eq(userId));

                mockMvc.perform(put("/slib/notifications/mark-all-read/{userId}", userId))
                                .andExpect(status().isOk());

                verify(pushNotificationService, times(1)).markAllAsRead(eq(userId));
        }

        // =========================================
        // === UTCID03: Mark as read for already read notification ===
        // =========================================
        @Test
        @DisplayName("UTCID03: Mark already-read notification returns 200 OK (idempotent)")
        void markAsRead_alreadyRead_returns200() throws Exception {
                UUID notificationId = UUID.randomUUID();

                doNothing().when(pushNotificationService).markAsRead(eq(notificationId));

                mockMvc.perform(put("/slib/notifications/mark-read/{notificationId}", notificationId))
                                .andExpect(status().isOk());

                verify(pushNotificationService, times(1)).markAsRead(eq(notificationId));
        }

        // =========================================
        // === UTCID04: Mark as read for non-existent notification ===
        // =========================================
        @Test
        @DisplayName("UTCID04: Mark non-existent notification as read returns error")
        void markAsRead_notFound_returnsError() throws Exception {
                UUID notificationId = UUID.randomUUID();

                doThrow(new RuntimeException("Thong bao khong ton tai"))
                                .when(pushNotificationService).markAsRead(eq(notificationId));

                mockMvc.perform(put("/slib/notifications/mark-read/{notificationId}", notificationId))
                                .andExpect(status().isInternalServerError());

                verify(pushNotificationService, times(1)).markAsRead(eq(notificationId));
        }

        // =========================================
        // === UTCID05: Mark all as read - service failure ===
        // =========================================
        @Test
        @DisplayName("UTCID05: Mark all as read when service fails returns error")
        void markAllAsRead_serviceFails_returnsError() throws Exception {
                UUID userId = UUID.randomUUID();

                doThrow(new RuntimeException("Loi cap nhat co so du lieu"))
                                .when(pushNotificationService).markAllAsRead(eq(userId));

                mockMvc.perform(put("/slib/notifications/mark-all-read/{userId}", userId))
                                .andExpect(status().isInternalServerError());

                verify(pushNotificationService, times(1)).markAllAsRead(eq(userId));
        }
}
