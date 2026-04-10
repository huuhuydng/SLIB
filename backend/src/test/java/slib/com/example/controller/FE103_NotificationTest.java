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

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.notification.NotificationController;

/**
 * Unit Tests for FE-103: View and delete list of notifications
 * Test Report: doc/Report/FE103_TestReport.md
 */
@WebMvcTest(value = NotificationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-103: View and delete list of notifications - Unit Tests")
class FE103_NotificationTest {

        private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
        private static final String TEST_EMAIL = "student@fpt.edu.vn";

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PushNotificationService notificationService;

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
        @DisplayName("UTCD01: View notifications returns 200 OK")
        void viewNotifications_validToken_returns200OK() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL))
                        .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                when(notificationService.getUserNotifications(eq(TEST_USER_ID), anyInt()))
                        .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/notifications/user/{userId}", TEST_USER_ID))
                        .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCD02: Get unread count returns 200 OK with count")
        void getUnreadCount_validUser_returns200OK() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL))
                        .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                when(notificationService.getUnreadCount(eq(TEST_USER_ID))).thenReturn(5L);

                mockMvc.perform(get("/slib/notifications/unread-count/{userId}", TEST_USER_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.count").value(5));
        }
}
