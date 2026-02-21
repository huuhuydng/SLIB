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

import java.util.Collections;

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
}
