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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.dto.DashboardStatsDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.DashboardService;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import slib.com.example.dto.DashboardStatsDTO.TopStudentDTO;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for DashboardController
 */
@WebMvcTest(value = DashboardController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DashboardController Unit Tests")
class DashboardControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    // =========================================
    // === GET DASHBOARD STATS ===
    // =========================================

    @Test
    @DisplayName("getDashboardStats_success_returns200WithStats")
    void getDashboardStats_success_returns200WithStats() throws Exception {
        DashboardStatsDTO stats = DashboardStatsDTO.builder()
                .totalSeats(50)
                .occupiedSeats(20)
                .occupancyRate(40.0)
                .totalCheckInsToday(10)
                .totalCheckOutsToday(5)
                .currentlyInLibrary(5)
                .totalBookingsToday(8)
                .activeBookings(3)
                .pendingBookings(2)
                .totalUsers(100)
                .recentBookings(Collections.emptyList())
                .topStudents(Collections.emptyList())
                .recentViolations(Collections.emptyList())
                .build();

        when(dashboardService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/slib/dashboard/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSeats").value(50))
                .andExpect(jsonPath("$.occupiedSeats").value(20))
                .andExpect(jsonPath("$.occupancyRate").value(40.0))
                .andExpect(jsonPath("$.totalUsers").value(100));

        verify(dashboardService).getDashboardStats();
    }

    @Test
    @DisplayName("getDashboardStats_serviceThrowsException_returns500")
    void getDashboardStats_serviceThrowsException_returns500() throws Exception {
        when(dashboardService.getDashboardStats()).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/slib/dashboard/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("getDashboardStats_returnsEmptyLists_returns200")
    void getDashboardStats_returnsEmptyLists_returns200() throws Exception {
        DashboardStatsDTO stats = DashboardStatsDTO.builder()
                .totalSeats(0)
                .occupiedSeats(0)
                .occupancyRate(0.0)
                .totalCheckInsToday(0)
                .totalCheckOutsToday(0)
                .currentlyInLibrary(0)
                .totalUsers(0)
                .recentBookings(Collections.emptyList())
                .topStudents(Collections.emptyList())
                .recentViolations(Collections.emptyList())
                .build();

        when(dashboardService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/slib/dashboard/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSeats").value(0))
                .andExpect(jsonPath("$.recentBookings").isArray())
                .andExpect(jsonPath("$.recentBookings").isEmpty());

        verify(dashboardService, times(1)).getDashboardStats();
    }

    // =========================================
    // === GET LIBRARY STATUS ===
    // =========================================

    @Test
    @DisplayName("getLibraryStatus_success_returns200WithStatus")
    void getLibraryStatus_success_returns200WithStatus() throws Exception {
        Map<String, Object> status = new java.util.HashMap<>();
        status.put("currentlyInLibrary", 50);
        status.put("totalSeats", 100);
        status.put("occupancyRate", 50.0);

        when(dashboardService.getLibraryStatus()).thenReturn(status);

        mockMvc.perform(get("/slib/dashboard/library-status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentlyInLibrary").value(50))
                .andExpect(jsonPath("$.totalSeats").value(100))
                .andExpect(jsonPath("$.occupancyRate").value(50.0));

        verify(dashboardService).getLibraryStatus();
    }

    // =========================================
    // === TEST BROADCAST ===
    // =========================================

    @Test
    @DisplayName("testBroadcast_success_returns200")
    void testBroadcast_success_returns200() throws Exception {
        mockMvc.perform(post("/slib/dashboard/test-broadcast")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("Broadcast sent to /topic/dashboard"));
    }

    // =========================================
    // === GET CHART STATS ===
    // =========================================

    @Test
    @DisplayName("getChartStats_defaultRange_returns200WithData")
    void getChartStats_defaultRange_returns200WithData() throws Exception {
        List<Map<String, Object>> chartData = List.of(
                Map.of("label", "Mon", "value", 10),
                Map.of("label", "Tue", "value", 20),
                Map.of("label", "Wed", "value", 15));

        when(dashboardService.getChartStats("week")).thenReturn(chartData);

        mockMvc.perform(get("/slib/dashboard/chart-stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(dashboardService).getChartStats("week");
    }

    @Test
    @DisplayName("getChartStats_monthRange_returns200")
    void getChartStats_monthRange_returns200() throws Exception {
        List<Map<String, Object>> chartData = List.of(
                Map.of("label", "Week 1", "value", 100));

        when(dashboardService.getChartStats("month")).thenReturn(chartData);

        mockMvc.perform(get("/slib/dashboard/chart-stats")
                .param("range", "month")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(dashboardService).getChartStats("month");
    }

    @Test
    @DisplayName("getChartStats_yearRange_returns200")
    void getChartStats_yearRange_returns200() throws Exception {
        List<Map<String, Object>> chartData = List.of(
                Map.of("label", "Jan", "value", 500));

        when(dashboardService.getChartStats("year")).thenReturn(chartData);

        mockMvc.perform(get("/slib/dashboard/chart-stats")
                .param("range", "year")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(dashboardService).getChartStats("year");
    }

    // =========================================
    // === GET TOP STUDENTS ===
    // =========================================

    @Test
    @DisplayName("getTopStudents_defaultRange_returns200WithData")
    void getTopStudents_defaultRange_returns200WithData() throws Exception {
        TopStudentDTO student1 = TopStudentDTO.builder()
                .userId(UUID.randomUUID())
                .fullName("Student 1")
                .userCode("SE123456")
                .totalVisits(50)
                .totalMinutes(3000)
                .build();

        TopStudentDTO student2 = TopStudentDTO.builder()
                .userId(UUID.randomUUID())
                .fullName("Student 2")
                .userCode("SE123457")
                .totalVisits(40)
                .totalMinutes(2400)
                .build();

        when(dashboardService.getTopStudents("month")).thenReturn(List.of(student1, student2));

        mockMvc.perform(get("/slib/dashboard/top-students")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Student 1"))
                .andExpect(jsonPath("$[1].fullName").value("Student 2"));

        verify(dashboardService).getTopStudents("month");
    }

    @Test
    @DisplayName("getTopStudents_weekRange_returns200")
    void getTopStudents_weekRange_returns200() throws Exception {
        when(dashboardService.getTopStudents("week")).thenReturn(List.of());

        mockMvc.perform(get("/slib/dashboard/top-students")
                .param("range", "week")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(dashboardService).getTopStudents("week");
    }

    @Test
    @DisplayName("getTopStudents_yearRange_returns200")
    void getTopStudents_yearRange_returns200() throws Exception {
        when(dashboardService.getTopStudents("year")).thenReturn(List.of());

        mockMvc.perform(get("/slib/dashboard/top-students")
                .param("range", "year")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(dashboardService).getTopStudents("year");
    }
}
