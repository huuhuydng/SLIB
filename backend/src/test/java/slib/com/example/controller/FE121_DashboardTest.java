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
import slib.com.example.service.DashboardService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-121: View Analytics Dashboard
 * Test Report: doc/Report/FE121_TestReport.md
 */
@WebMvcTest(value = DashboardController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-121: View Analytics Dashboard - Unit Tests")
class FE121_DashboardTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private DashboardService dashboardService;

        // UTCD01: Valid token - Success
        @Test
        @DisplayName("UTCD01: View dashboard with valid token returns 200 OK")
        void viewDashboard_validToken_returns200OK() throws Exception {
                mockMvc.perform(get("/slib/dashboard"))
                        .andExpect(status().isOk());
        }

        // UTCD02: No token - 401
        @Test
        @DisplayName("UTCD02: View dashboard without token returns 401 Unauthorized")
        void viewDashboard_noToken_returns401() throws Exception {
                mockMvc.perform(get("/slib/dashboard"))
                        .andExpect(status().isUnauthorized());
        }

        // UTCD03: Not librarian/admin - 403
        @Test
        @DisplayName("UTCD03: View dashboard without permission returns 403 Forbidden")
        void viewDashboard_noPermission_returns403() throws Exception {
                mockMvc.perform(get("/slib/dashboard"))
                        .andExpect(status().isForbidden());
        }
}
