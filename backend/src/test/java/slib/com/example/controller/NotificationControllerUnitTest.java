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
import slib.com.example.entity.notification.NotificationEntity;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.PushNotificationService;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for NotificationController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 */
@WebMvcTest(value = NotificationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("NotificationController Unit Tests")
class NotificationControllerUnitTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PushNotificationService pushNotificationService;

        @MockBean
        private UserRepository userRepository;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === GET USER NOTIFICATIONS ===
        // =========================================

        @Test
        @DisplayName("getUserNotifications_validUserId_returns200WithList")
        void getUserNotifications_validUserId_returns200WithList() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                List<NotificationEntity> notifications = Arrays.asList(
                                createNotification(UUID.randomUUID(), "Title 1", "Body 1"),
                                createNotification(UUID.randomUUID(), "Title 2", "Body 2"));

                when(pushNotificationService.getUserNotifications(eq(userId), anyInt()))
                                .thenReturn(notifications);

                // Act & Assert
                mockMvc.perform(get("/slib/notifications/user/{userId}", userId)
                                .param("limit", "50")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2));

                verify(pushNotificationService, times(1)).getUserNotifications(userId, 50);
        }

        @Test
        @DisplayName("getUserNotifications_noNotifications_returns200WithEmptyList")
        void getUserNotifications_noNotifications_returns200WithEmptyList() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                when(pushNotificationService.getUserNotifications(eq(userId), anyInt()))
                                .thenReturn(List.of());

                // Act & Assert
                mockMvc.perform(get("/slib/notifications/user/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(0));
        }

        // =========================================
        // === GET UNREAD COUNT ===
        // =========================================

        @Test
        @DisplayName("getUnreadCount_validUserId_returns200WithCount")
        void getUnreadCount_validUserId_returns200WithCount() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                when(pushNotificationService.getUnreadCount(userId)).thenReturn(5L);

                // Act & Assert
                mockMvc.perform(get("/slib/notifications/unread-count/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.count").value(5));

                verify(pushNotificationService, times(1)).getUnreadCount(userId);
        }

        // =========================================
        // === MARK AS READ ===
        // =========================================

        @Test
        @DisplayName("markAsRead_validNotificationId_returns200")
        void markAsRead_validNotificationId_returns200() throws Exception {
                // Arrange
                UUID notificationId = UUID.randomUUID();
                doNothing().when(pushNotificationService).markAsRead(notificationId);

                // Act & Assert
                mockMvc.perform(put("/slib/notifications/mark-read/{notificationId}", notificationId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                verify(pushNotificationService, times(1)).markAsRead(notificationId);
        }

        // =========================================
        // === MARK ALL AS READ ===
        // =========================================

        @Test
        @DisplayName("markAllAsRead_validUserId_returns200")
        void markAllAsRead_validUserId_returns200() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                doNothing().when(pushNotificationService).markAllAsRead(userId);

                // Act & Assert
                mockMvc.perform(put("/slib/notifications/mark-all-read/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                verify(pushNotificationService, times(1)).markAllAsRead(userId);
        }

        // =========================================
        // === UPDATE SETTINGS ===
        // =========================================

        @Test
        @DisplayName("updateSettings_validRequest_returns200WithUpdatedSettings")
        void updateSettings_validRequest_returns200WithUpdatedSettings() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                User user = new User();
                user.setId(userId);
                user.setNotifyBooking(true);
                user.setNotifyReminder(false);
                user.setNotifyNews(true);

                when(userRepository.findById(userId)).thenReturn(Optional.of(user));
                when(userRepository.save(any(User.class))).thenReturn(user);

                Map<String, Boolean> request = new HashMap<>();
                request.put("notifyBooking", true);
                request.put("notifyReminder", false);
                request.put("notifyNews", true);

                // Act & Assert
                mockMvc.perform(put("/slib/notifications/settings/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.notifyBooking").value(true))
                                .andExpect(jsonPath("$.notifyReminder").value(false))
                                .andExpect(jsonPath("$.notifyNews").value(true));

                verify(userRepository, times(1)).findById(userId);
                verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("updateSettings_userNotFound_throwsException")
        void updateSettings_userNotFound_throwsException() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                when(userRepository.findById(userId)).thenReturn(Optional.empty());

                Map<String, Boolean> request = new HashMap<>();
                request.put("notifyBooking", true);

                // Act & Assert
                mockMvc.perform(put("/slib/notifications/settings/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(userRepository, times(1)).findById(userId);
                verify(userRepository, never()).save(any());
        }

        // =========================================
        // === GET SETTINGS ===
        // =========================================

        @Test
        @DisplayName("getSettings_validUserId_returns200WithSettings")
        void getSettings_validUserId_returns200WithSettings() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                User user = new User();
                user.setId(userId);
                user.setNotifyBooking(true);
                user.setNotifyReminder(true);
                user.setNotifyNews(false);

                when(userRepository.findById(userId)).thenReturn(Optional.of(user));

                // Act & Assert
                mockMvc.perform(get("/slib/notifications/settings/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.notifyBooking").value(true))
                                .andExpect(jsonPath("$.notifyReminder").value(true))
                                .andExpect(jsonPath("$.notifyNews").value(false));

                verify(userRepository, times(1)).findById(userId);
        }

        // =========================================
        // === HELPER METHODS ===
        // =========================================

        private NotificationEntity createNotification(UUID id, String title, String content) {
                NotificationEntity notification = new NotificationEntity();
                notification.setId(id);
                notification.setTitle(title);
                notification.setContent(content);
                notification.setIsRead(false);
                notification.setCreatedAt(LocalDateTime.now());
                return notification;
        }
}
