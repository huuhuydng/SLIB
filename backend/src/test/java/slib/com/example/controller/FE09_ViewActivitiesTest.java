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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.activity.ActivityService;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.activity.ActivityController;

/**
 * Unit Tests for FE-09: View History of Activities
 * Test Report: doc/Report/FE09_TestReport.md
 */
@WebMvcTest(value = ActivityController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-09: View History of Activities - Unit Tests")
class FE09_ViewActivitiesTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ActivityService activityService;

        @MockBean
        private UserRepository userRepository;

        private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        private static final String TEST_EMAIL = "student@fpt.edu.vn";

        private User buildCurrentUser(UUID userId) {
                User user = new User();
                user.setId(userId);
                user.setEmail(TEST_EMAIL);
                user.setRole(Role.STUDENT);
                return user;
        }

        // UTCD01: Valid token + has history - Success
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCD01: View activities with valid token returns 200 OK")
        void viewActivities_validToken_returns200OK() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(activityService.getActivitiesByUser(any())).thenReturn(Collections.emptyList());
                when(activityService.getTotalStudyHours(any())).thenReturn(0.0);
                when(activityService.getTotalVisits(any())).thenReturn(0L);

                mockMvc.perform(get("/slib/activities/user/" + TEST_USER_ID))
                        .andExpect(status().isOk());
        }

        // UTCD02: Invalid userId format - 400
        @Test
        @DisplayName("UTCD02: View activities with invalid userId returns 400 Bad Request")
        void viewActivities_invalidUserId_returns400BadRequest() throws Exception {
                mockMvc.perform(get("/slib/activities/user/invalid-uuid"))
                        .andExpect(status().isBadRequest());
        }

        // UTCD03: No activity history - 200 OK (empty)
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCD03: No activity history returns 200 OK with empty data")
        void viewActivities_noHistory_returns200OK() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(activityService.getActivitiesByUser(any())).thenReturn(Collections.emptyList());
                when(activityService.getTotalStudyHours(any())).thenReturn(0.0);
                when(activityService.getTotalVisits(any())).thenReturn(0L);

                mockMvc.perform(get("/slib/activities/user/" + TEST_USER_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.activities").isArray());
        }
}
