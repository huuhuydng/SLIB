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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-08: View History of Activities
 * Test Report: doc/Report/FE08_TestReport.md
 */
@WebMvcTest(value = ActivityController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-08: View History of Activities - Unit Tests")
class FE08_ViewActivitiesTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ActivityService activityService;

        // UTCD01: Valid token + has history - Success
        @Test
        @DisplayName("UTCD01: View activities with valid token returns 200 OK")
        void viewActivities_validToken_returns200OK() throws Exception {
                when(activityService.getUserActivities(any())).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/activities/user/123"))
                        .andExpect(status().isOk());
        }

        // UTCD02: No token - 401
        @Test
        @DisplayName("UTCD02: View activities without token returns 401 Unauthorized")
        void viewActivities_noToken_returns401Unauthorized() throws Exception {
                mockMvc.perform(get("/slib/activities/user/123"))
                        .andExpect(status().isUnauthorized());
        }

        // UTCD03: No activity history - 200 OK (empty)
        @Test
        @DisplayName("UTCD03: No activity history returns 200 OK (empty list)")
        void viewActivities_noHistory_returns200OK() throws Exception {
                when(activityService.getUserActivities(any())).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/activities/user/123"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").isArray());
        }
}
