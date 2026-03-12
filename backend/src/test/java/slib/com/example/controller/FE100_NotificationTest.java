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

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-100: View/Delete Notifications
 * Test Report: doc/Report/FE100_TestReport.md
 */
@WebMvcTest(value = NotificationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-100: View/Delete Notifications - Unit Tests")
class FE100_NotificationTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PushNotificationService notificationService;

        @MockBean
        private UserRepository userRepository;

        @Test
        @DisplayName("UTCD01: View notifications returns 200 OK")
        void viewNotifications_validToken_returns200OK() throws Exception {
                UUID userId = UUID.randomUUID();
                when(notificationService.getUserNotifications(eq(userId), anyInt()))
                        .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/notifications/user/{userId}", userId))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("UTCD02: Get unread count returns 200 OK with count")
        void getUnreadCount_validUser_returns200OK() throws Exception {
                UUID userId = UUID.randomUUID();
                when(notificationService.getUnreadCount(eq(userId))).thenReturn(5L);

                mockMvc.perform(get("/slib/notifications/unread-count/{userId}", userId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.count").value(5));
        }
}
