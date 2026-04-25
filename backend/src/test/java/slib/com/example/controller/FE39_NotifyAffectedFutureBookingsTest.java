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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = NotificationController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-39: Notify affected future bookings after layout changes - Unit Tests")
class FE39_NotifyAffectedFutureBookingsTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "student@fpt.edu.vn";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PushNotificationService pushNotificationService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = EMAIL, roles = "STUDENT")
    @DisplayName("UTCID01: Student receives layout-change notification in notification list")
    void viewNotifications_layoutChanged_returns200() throws Exception {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setRole(Role.STUDENT);

        NotificationDTO dto = NotificationDTO.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .title("Lịch đặt chỗ của bạn vừa bị ảnh hưởng")
                .content("Thư viện đã cập nhật sơ đồ và ghế của bạn cần được xử lý lại.")
                .category("BOOKING")
                .notificationType("SYSTEM")
                .isRead(false)
                .createdAt(LocalDateTime.of(2026, 4, 20, 10, 0))
                .build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(pushNotificationService.getUserNotificationDTOs(eq(USER_ID), eq(10))).thenReturn(List.of(dto));

        mockMvc.perform(get("/slib/notifications/user/{userId}", USER_ID)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Lịch đặt chỗ của bạn vừa bị ảnh hưởng"))
                .andExpect(jsonPath("$[0].category").value("BOOKING"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "STUDENT")
    @DisplayName("UTCID02: Student gets empty notification list when no layout-change notification exists")
    void viewNotifications_noNotification_returns200WithEmptyList() throws Exception {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setRole(Role.STUDENT);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(pushNotificationService.getUserNotificationDTOs(eq(USER_ID), eq(10))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/slib/notifications/user/{userId}", USER_ID)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "STUDENT")
    @DisplayName("UTCID03: Get layout-change notification when current user does not exist returns 500 Internal Server Error")
    void viewNotifications_userNotFound_returns500() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        mockMvc.perform(get("/slib/notifications/user/{userId}", USER_ID)
                        .param("limit", "10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "STUDENT")
    @DisplayName("UTCID04: Student cannot view another user's layout-change notification list")
    void viewNotifications_otherUser_returns403() throws Exception {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setRole(Role.STUDENT);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/slib/notifications/user/{userId}",
                                UUID.fromString("22222222-2222-2222-2222-222222222222"))
                        .param("limit", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "STUDENT")
    @DisplayName("UTCID05: Get layout-change notification when service fails returns 500 Internal Server Error")
    void viewNotifications_serviceFails_returns500() throws Exception {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setRole(Role.STUDENT);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(pushNotificationService.getUserNotificationDTOs(eq(USER_ID), eq(10)))
                .thenThrow(new RuntimeException("Không thể tải danh sách thông báo"));

        mockMvc.perform(get("/slib/notifications/user/{userId}", USER_ID)
                        .param("limit", "10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}
