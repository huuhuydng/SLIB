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
import slib.com.example.service.ActivityService;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

        private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

        // UTCD01: Valid token + has history - Success
        @Test
        @DisplayName("UTCD01: View activities with valid token returns 200 OK")
        void viewActivities_validToken_returns200OK() throws Exception {
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
        @DisplayName("UTCD03: No activity history returns 200 OK with empty data")
        void viewActivities_noHistory_returns200OK() throws Exception {
                when(activityService.getActivitiesByUser(any())).thenReturn(Collections.emptyList());
                when(activityService.getTotalStudyHours(any())).thenReturn(0.0);
                when(activityService.getTotalVisits(any())).thenReturn(0L);

                mockMvc.perform(get("/slib/activities/user/" + TEST_USER_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.activities").isArray());
        }
}
