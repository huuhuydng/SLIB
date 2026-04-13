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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.notification.NotificationController;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.notification.PushNotificationService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit Tests for FE-11: Turn on/Turn off Notification
 */
@WebMvcTest(value = NotificationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-11: Turn on/Turn off Notification - Unit Tests")
class FE11_NotificationSettingTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PushNotificationService pushNotificationService;

        @MockBean
        private UserRepository userRepository;

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCID01: Update notification settings with all flags specified")
        void updateNotificationSettings_withAllFlagsSpecified() throws Exception {
                UUID userId = UUID.randomUUID();
                User currentUser = mock(User.class);

                when(currentUser.getId()).thenReturn(userId);
                when(currentUser.getRole()).thenReturn(null);
                when(currentUser.getNotifyBooking()).thenReturn(true);
                when(currentUser.getNotifyReminder()).thenReturn(false);
                when(currentUser.getNotifyNews()).thenReturn(true);
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(currentUser));
                when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

                mockMvc.perform(put("/slib/notifications/settings/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "notifyBooking": true,
                                                  "notifyReminder": false,
                                                  "notifyNews": true
                                                }
                                                """))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.notifyBooking").value(true))
                        .andExpect(jsonPath("$.notifyReminder").value(false))
                        .andExpect(jsonPath("$.notifyNews").value(true));

                verify(userRepository).save(currentUser);
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCID02: Update notification settings with partial payload")
        void updateNotificationSettings_withPartialPayload() throws Exception {
                UUID userId = UUID.randomUUID();
                User currentUser = mock(User.class);

                when(currentUser.getId()).thenReturn(userId);
                when(currentUser.getRole()).thenReturn(null);
                when(currentUser.getNotifyBooking()).thenReturn(true);
                when(currentUser.getNotifyReminder()).thenReturn(true);
                when(currentUser.getNotifyNews()).thenReturn(false);
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(currentUser));
                when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

                mockMvc.perform(put("/slib/notifications/settings/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "notifyNews": false
                                                }
                                                """))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.notifyBooking").value(true))
                        .andExpect(jsonPath("$.notifyReminder").value(true))
                        .andExpect(jsonPath("$.notifyNews").value(false));
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCID03: Update notification settings when authenticated user does not exist")
        void updateNotificationSettings_whenAuthenticatedUserDoesNotExist() throws Exception {
                UUID userId = UUID.randomUUID();

                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.empty());

                mockMvc.perform(put("/slib/notifications/settings/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "notifyBooking": true
                                                }
                                                """))
                        .andExpect(status().isInternalServerError());
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCID04: Get notification settings with stored values")
        void getNotificationSettings_withStoredValues() throws Exception {
                UUID userId = UUID.randomUUID();
                User currentUser = mock(User.class);

                when(currentUser.getId()).thenReturn(userId);
                when(currentUser.getRole()).thenReturn(null);
                when(currentUser.getNotifyBooking()).thenReturn(false);
                when(currentUser.getNotifyReminder()).thenReturn(true);
                when(currentUser.getNotifyNews()).thenReturn(false);
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(currentUser));
                when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

                mockMvc.perform(get("/slib/notifications/settings/{userId}", userId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.notifyBooking").value(false))
                        .andExpect(jsonPath("$.notifyReminder").value(true))
                        .andExpect(jsonPath("$.notifyNews").value(false));
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCID05: Get notification settings when stored values are null")
        void getNotificationSettings_whenStoredValuesAreNull() throws Exception {
                UUID userId = UUID.randomUUID();
                User currentUser = mock(User.class);

                when(currentUser.getId()).thenReturn(userId);
                when(currentUser.getRole()).thenReturn(null);
                when(currentUser.getNotifyBooking()).thenReturn(null);
                when(currentUser.getNotifyReminder()).thenReturn(null);
                when(currentUser.getNotifyNews()).thenReturn(null);
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(currentUser));
                when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

                mockMvc.perform(get("/slib/notifications/settings/{userId}", userId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.notifyBooking").value(true))
                        .andExpect(jsonPath("$.notifyReminder").value(true))
                        .andExpect(jsonPath("$.notifyNews").value(true));
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCID06: Update notification settings for another user's userId")
        void updateNotificationSettings_forAnotherUsersUserId() throws Exception {
                UUID currentUserId = UUID.randomUUID();
                UUID requestedUserId = UUID.randomUUID();
                User currentUser = mock(User.class);

                when(currentUser.getId()).thenReturn(currentUserId);
                when(currentUser.getRole()).thenReturn(null);
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(currentUser));

                mockMvc.perform(put("/slib/notifications/settings/{userId}", requestedUserId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "notifyBooking": false
                                                }
                                                """))
                        .andExpect(status().isForbidden());

                verify(userRepository, never()).findById(requestedUserId);
                verify(userRepository, never()).save(currentUser);
        }
}
