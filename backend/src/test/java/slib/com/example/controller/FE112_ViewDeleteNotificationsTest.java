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
import slib.com.example.controller.notification.NotificationController;
import slib.com.example.dto.notification.NotificationDTO;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.notification.PushNotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = NotificationController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-112: View and delete list of notifications - Unit Tests")
class FE112_ViewDeleteNotificationsTest {

    private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID NOTIFICATION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
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

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
    @DisplayName("UTCID01: View notifications with valid userId and limit")
    void viewNotifications_withValidUserIdAndLimit() throws Exception {
        NotificationDTO notification = NotificationDTO.builder()
                .id(NOTIFICATION_ID)
                .userId(TEST_USER_ID)
                .title("Seat booking reminder")
                .content("Your reservation starts in 15 minutes")
                .notificationType("SYSTEM")
                .category("BOOKING")
                .isRead(false)
                .createdAt(LocalDateTime.of(2026, 4, 9, 8, 0))
                .build();

        when(userRepository.findByEmail(TEST_EMAIL))
                .thenReturn(Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
        when(pushNotificationService.getUserNotificationDTOs(TEST_USER_ID, 10))
                .thenReturn(List.of(notification));

        mockMvc.perform(get("/slib/notifications/user/{userId}", TEST_USER_ID)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(NOTIFICATION_ID.toString()))
                .andExpect(jsonPath("$[0].title").value("Seat booking reminder"))
                .andExpect(jsonPath("$[0].isRead").value(false));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
    @DisplayName("UTCID02: Get unread count with valid userId")
    void getUnreadCount_withValidUserId() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL))
                .thenReturn(Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
        when(pushNotificationService.getUnreadCount(TEST_USER_ID)).thenReturn(5L);

        mockMvc.perform(get("/slib/notifications/unread-count/{userId}", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
    @DisplayName("UTCID03: Delete notification with valid notificationId and owner userId")
    void deleteNotification_withValidNotificationIdAndOwnerUserId() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL))
                .thenReturn(Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));

        mockMvc.perform(delete("/slib/notifications/{notificationId}", NOTIFICATION_ID)
                        .param("userId", TEST_USER_ID.toString()))
                .andExpect(status().isOk());

        verify(pushNotificationService).deleteNotification(NOTIFICATION_ID, TEST_USER_ID);
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
    @DisplayName("UTCID04: Get unread count with invalid userId format")
    void getUnreadCount_withInvalidUserIdFormat() throws Exception {
        mockMvc.perform(get("/slib/notifications/unread-count/{userId}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
    @DisplayName("UTCID05: Delete notification for another user's userId")
    void deleteNotification_forAnotherUsersUserId() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL))
                .thenReturn(Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));

        mockMvc.perform(delete("/slib/notifications/{notificationId}", NOTIFICATION_ID)
                        .param("userId", OTHER_USER_ID.toString()))
                .andExpect(status().isForbidden());

        verify(pushNotificationService, never()).deleteNotification(eq(NOTIFICATION_ID), eq(OTHER_USER_ID));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
    @DisplayName("UTCID06: Get unread count when authenticated user does not exist")
    void getUnreadCount_whenAuthenticatedUserDoesNotExist() throws Exception {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        mockMvc.perform(get("/slib/notifications/unread-count/{userId}", TEST_USER_ID))
                .andExpect(status().isInternalServerError());
    }
}
