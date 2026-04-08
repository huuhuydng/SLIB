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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.dto.dashboard.DashboardStatsDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.dashboard.DashboardService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.dashboard.DashboardController;

/**
 * Unit Tests for FE-121: View general analytics dashboard
 * Test Report: doc/Report/UnitTestReport/FE121_TestReport.md
 */
@WebMvcTest(value = DashboardController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-121: View general analytics dashboard - Unit Tests")
class FE121_DashboardTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private DashboardService dashboardService;

        @MockBean
        private SimpMessagingTemplate messagingTemplate;

        // UTCD01: Valid request - Success
        @Test
        @DisplayName("UTCD01: View dashboard stats returns 200 OK")
        void viewDashboard_validToken_returns200OK() throws Exception {
                when(dashboardService.getDashboardStats()).thenReturn(new DashboardStatsDTO());

                mockMvc.perform(get("/slib/dashboard/stats"))
                        .andExpect(status().isOk());
        }

        // UTCD02: Library status endpoint returns 200
        @Test
        @DisplayName("UTCD02: View library status returns 200 OK")
        void viewLibraryStatus_returns200OK() throws Exception {
                when(dashboardService.getLibraryStatus()).thenReturn(java.util.Map.of(
                        "status", "OPEN",
                        "occupancy", 50));

                mockMvc.perform(get("/slib/dashboard/library-status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("OPEN"));
        }

        // UTCD03: Chart stats endpoint returns 200
        @Test
        @DisplayName("UTCD03: View chart stats returns 200 OK")
        void viewChartStats_returns200OK() throws Exception {
                when(dashboardService.getChartStats("week")).thenReturn(java.util.Collections.emptyList());

                mockMvc.perform(get("/slib/dashboard/chart-stats").param("range", "week"))
                        .andExpect(status().isOk());
        }
}
