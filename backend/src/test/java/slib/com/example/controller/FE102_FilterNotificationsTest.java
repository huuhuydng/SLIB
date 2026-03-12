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
import slib.com.example.entity.notification.NotificationEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.PushNotificationService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-102: Filter Notifications
 * Test Report: doc/Report/UnitTestReport/FE102_TestReport.md
 */
@WebMvcTest(value = NotificationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-102: Filter Notifications - Unit Tests")
class FE102_FilterNotificationsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PushNotificationService pushNotificationService;

        @MockBean
        private UserRepository userRepository;

        // =========================================
        // === UTCID01: Normal - filter with default limit ===
        // =========================================
        @Test
        @DisplayName("UTCID01: Filter notifications with default limit returns 200 OK")
        void filterNotifications_defaultLimit_returns200() throws Exception {
                UUID userId = UUID.randomUUID();
                NotificationEntity notification = new NotificationEntity();
                notification.setTitle("Thong bao mac dinh");

                when(pushNotificationService.getUserNotifications(eq(userId), eq(50)))
                                .thenReturn(List.of(notification));

                mockMvc.perform(get("/slib/notifications/user/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Thong bao mac dinh"));

                verify(pushNotificationService, times(1)).getUserNotifications(eq(userId), eq(50));
        }

        // =========================================
        // === UTCID02: Filter with explicit positive limit ===
        // =========================================
        @Test
        @DisplayName("UTCID02: Filter notifications with positive limit returns 200 OK")
        void filterNotifications_positiveLimit_returns200() throws Exception {
                UUID userId = UUID.randomUUID();
                NotificationEntity n1 = new NotificationEntity();
                n1.setTitle("Thong bao 1");
                NotificationEntity n2 = new NotificationEntity();
                n2.setTitle("Thong bao 2");

                when(pushNotificationService.getUserNotifications(eq(userId), eq(5)))
                                .thenReturn(List.of(n1, n2));

                mockMvc.perform(get("/slib/notifications/user/{userId}", userId)
                                .param("limit", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2));

                verify(pushNotificationService, times(1)).getUserNotifications(eq(userId), eq(5));
        }

        // =========================================
        // === UTCID03: Filter with large limit ===
        // =========================================
        @Test
        @DisplayName("UTCID03: Filter notifications with large limit returns 200 OK")
        void filterNotifications_largeLimit_returns200() throws Exception {
                UUID userId = UUID.randomUUID();

                when(pushNotificationService.getUserNotifications(eq(userId), eq(1000)))
                                .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/notifications/user/{userId}", userId)
                                .param("limit", "1000"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(pushNotificationService, times(1)).getUserNotifications(eq(userId), eq(1000));
        }

        // =========================================
        // === UTCID04: Filter returns empty list ===
        // =========================================
        @Test
        @DisplayName("UTCID04: Filter notifications for user with no notifications returns empty list")
        void filterNotifications_noResults_returns200() throws Exception {
                UUID userId = UUID.randomUUID();

                when(pushNotificationService.getUserNotifications(eq(userId), anyInt()))
                                .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/notifications/user/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(pushNotificationService, times(1)).getUserNotifications(eq(userId), anyInt());
        }

        // =========================================
        // === UTCID05: Service throws exception ===
        // =========================================
        @Test
        @DisplayName("UTCID05: Filter notifications when service fails returns error")
        void filterNotifications_serviceFails_returnsError() throws Exception {
                UUID userId = UUID.randomUUID();

                when(pushNotificationService.getUserNotifications(eq(userId), anyInt()))
                                .thenThrow(new RuntimeException("Loi truy van co so du lieu"));

                mockMvc.perform(get("/slib/notifications/user/{userId}", userId))
                                .andExpect(status().isInternalServerError());

                verify(pushNotificationService, times(1)).getUserNotifications(eq(userId), anyInt());
        }
}
