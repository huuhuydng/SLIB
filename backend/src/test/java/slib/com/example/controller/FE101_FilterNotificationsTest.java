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
import slib.com.example.entity.notification.NotificationEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.notification.PushNotificationService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.notification.NotificationController;

/**
 * Unit Tests for FE-101: Filter Notifications
 * Test Report: doc/Report/UnitTestReport/FE101_TestReport.md
 */
@WebMvcTest(value = NotificationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-101: Filter Notifications - Unit Tests")
class FE101_FilterNotificationsTest {

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
        // === UTCID01: Normal - filter with default limit ===
        // =========================================
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID01: Filter notifications with default limit returns 200 OK")
        void filterNotifications_defaultLimit_returns200() throws Exception {
                NotificationEntity notification = new NotificationEntity();
                notification.setTitle("Thong bao mac dinh");
                slib.com.example.dto.notification.NotificationDTO dto = slib.com.example.dto.notification.NotificationDTO.builder()
                                .title("Thong bao mac dinh")
                                .build();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                when(pushNotificationService.getUserNotifications(eq(TEST_USER_ID), eq(50)))
                                .thenReturn(List.of(notification));
                when(pushNotificationService.toDTO(notification)).thenReturn(dto);

                mockMvc.perform(get("/slib/notifications/user/{userId}", TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Thong bao mac dinh"));

                verify(pushNotificationService, times(1)).getUserNotifications(eq(TEST_USER_ID), eq(50));
        }

        // =========================================
        // === UTCID02: Filter with explicit positive limit ===
        // =========================================
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID02: Filter notifications with positive limit returns 200 OK")
        void filterNotifications_positiveLimit_returns200() throws Exception {
                NotificationEntity n1 = new NotificationEntity();
                n1.setTitle("Thong bao 1");
                NotificationEntity n2 = new NotificationEntity();
                n2.setTitle("Thong bao 2");
                slib.com.example.dto.notification.NotificationDTO dto1 = slib.com.example.dto.notification.NotificationDTO.builder()
                                .title("Thong bao 1")
                                .build();
                slib.com.example.dto.notification.NotificationDTO dto2 = slib.com.example.dto.notification.NotificationDTO.builder()
                                .title("Thong bao 2")
                                .build();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                when(pushNotificationService.getUserNotifications(eq(TEST_USER_ID), eq(5)))
                                .thenReturn(List.of(n1, n2));
                when(pushNotificationService.toDTO(n1)).thenReturn(dto1);
                when(pushNotificationService.toDTO(n2)).thenReturn(dto2);

                mockMvc.perform(get("/slib/notifications/user/{userId}", TEST_USER_ID)
                                .param("limit", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2));

                verify(pushNotificationService, times(1)).getUserNotifications(eq(TEST_USER_ID), eq(5));
        }

        // =========================================
        // === UTCID03: Filter with large limit ===
        // =========================================
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID03: Filter notifications with large limit returns 200 OK")
        void filterNotifications_largeLimit_returns200() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                when(pushNotificationService.getUserNotifications(eq(TEST_USER_ID), eq(1000)))
                                .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/notifications/user/{userId}", TEST_USER_ID)
                                .param("limit", "1000"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(pushNotificationService, times(1)).getUserNotifications(eq(TEST_USER_ID), eq(1000));
        }

        // =========================================
        // === UTCID04: Filter returns empty list ===
        // =========================================
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID04: Filter notifications for user with no notifications returns empty list")
        void filterNotifications_noResults_returns200() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                when(pushNotificationService.getUserNotifications(eq(TEST_USER_ID), anyInt()))
                                .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/notifications/user/{userId}", TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(pushNotificationService, times(1)).getUserNotifications(eq(TEST_USER_ID), anyInt());
        }

        // =========================================
        // === UTCID05: Service throws exception ===
        // =========================================
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID05: Filter notifications when service fails returns error")
        void filterNotifications_serviceFails_returnsError() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                when(pushNotificationService.getUserNotifications(eq(TEST_USER_ID), anyInt()))
                                .thenThrow(new RuntimeException("Loi truy van co so du lieu"));

                mockMvc.perform(get("/slib/notifications/user/{userId}", TEST_USER_ID))
                                .andExpect(status().isInternalServerError());

                verify(pushNotificationService, times(1)).getUserNotifications(eq(TEST_USER_ID), anyInt());
        }
}
