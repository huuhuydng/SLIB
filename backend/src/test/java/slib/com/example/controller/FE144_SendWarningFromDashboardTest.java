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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.dashboard.DashboardController;
import slib.com.example.dto.dashboard.DashboardStatsDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.dashboard.DashboardService;

import java.util.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-144: Send warning to student from AI analytics dashboard
 */
@WebMvcTest(value = DashboardController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-144: Send warning to student from AI analytics dashboard - Unit Tests")
class FE144_SendWarningFromDashboardTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private DashboardService dashboardService;

        @MockBean
        private SimpMessagingTemplate messagingTemplate;

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD01: View chart stats for analytics - returns 200")
        void getChartStats_validRange_returns200() throws Exception {
                List<Map<String, Object>> chartData = List.of(
                                Map.of("label", "Mon", "value", 5));
                when(dashboardService.getChartStats("week")).thenReturn(chartData);

                mockMvc.perform(get("/slib/dashboard/chart-stats?range=week"))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD02: View dashboard stats for analytics overview - returns 200")
        void getDashboardStats_returns200() throws Exception {
                DashboardStatsDTO stats = DashboardStatsDTO.builder()
                                .totalUsers(100L)
                                .totalBookingsToday(50L)
                                .build();
                when(dashboardService.getDashboardStats()).thenReturn(stats);

                mockMvc.perform(get("/slib/dashboard/stats"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalUsers").value(100));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD03: Test broadcast - admin sends warning broadcast - returns 200")
        void testBroadcast_admin_returns200() throws Exception {
                mockMvc.perform(post("/slib/dashboard/test-broadcast"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("OK"));

                verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/dashboard"), any(Map.class));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD04: Chart stats - service error returns 500")
        void getChartStats_serviceError_returns500() throws Exception {
                when(dashboardService.getChartStats("month"))
                                .thenThrow(new RuntimeException("Service unavailable"));

                mockMvc.perform(get("/slib/dashboard/chart-stats?range=month"))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD05: Chart stats - default range returns 200")
        void getChartStats_defaultRange_returns200() throws Exception {
                when(dashboardService.getChartStats("week")).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/dashboard/chart-stats"))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD06: Dashboard stats - service error returns 500")
        void getDashboardStats_serviceError_returns500() throws Exception {
                when(dashboardService.getDashboardStats())
                                .thenThrow(new RuntimeException("DB Connection failed"));

                mockMvc.perform(get("/slib/dashboard/stats"))
                                .andExpect(status().isInternalServerError());
        }
}
