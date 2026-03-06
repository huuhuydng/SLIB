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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.SeedDataService;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for SeedDataController
 */
@WebMvcTest(value = SeedDataController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SeedDataController Unit Tests")
class SeedDataControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeedDataService seedDataService;

    private Map<String, Object> createSuccessResult(String message, int count) {
        return Map.of("status", "SUCCESS", "message", message, "count", count);
    }

    // =========================================
    // === SEED ALL ===
    // =========================================

    @Test
    @DisplayName("seedAll_defaultParams_returns200")
    void seedAll_defaultParams_returns200() throws Exception {
        Map<String, Object> result = Map.of("status", "SUCCESS", "message", "All seeded");
        when(seedDataService.seedAll(15, 8, 8)).thenReturn(result);

        mockMvc.perform(post("/slib/seed/all")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(seedDataService).seedAll(15, 8, 8);
    }

    @Test
    @DisplayName("seedAll_customParams_returns200")
    void seedAll_customParams_returns200() throws Exception {
        Map<String, Object> result = Map.of("status", "SUCCESS", "message", "All seeded");
        when(seedDataService.seedAll(20, 10, 5)).thenReturn(result);

        mockMvc.perform(post("/slib/seed/all")
                .param("bookings", "20")
                .param("violations", "10")
                .param("supports", "5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(seedDataService).seedAll(20, 10, 5);
    }

    // =========================================
    // === SEED BOOKINGS ===
    // =========================================

    @Test
    @DisplayName("seedBookings_defaultCount_returns200")
    void seedBookings_defaultCount_returns200() throws Exception {
        when(seedDataService.seedBookings(15)).thenReturn(createSuccessResult("Bookings seeded", 15));

        mockMvc.perform(post("/slib/seed/bookings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(15));

        verify(seedDataService).seedBookings(15);
    }

    @Test
    @DisplayName("seedBookings_customCount_returns200")
    void seedBookings_customCount_returns200() throws Exception {
        when(seedDataService.seedBookings(30)).thenReturn(createSuccessResult("Bookings seeded", 30));

        mockMvc.perform(post("/slib/seed/bookings")
                .param("count", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(30));
    }

    // =========================================
    // === SEED ACCESS LOGS ===
    // =========================================

    @Test
    @DisplayName("seedAccessLogs_defaultCount_returns200")
    void seedAccessLogs_defaultCount_returns200() throws Exception {
        when(seedDataService.seedAccessLogs(50)).thenReturn(createSuccessResult("Access logs seeded", 50));

        mockMvc.perform(post("/slib/seed/access-logs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(50));

        verify(seedDataService).seedAccessLogs(50);
    }

    // =========================================
    // === SEED VIOLATIONS ===
    // =========================================

    @Test
    @DisplayName("seedViolations_defaultCount_returns200")
    void seedViolations_defaultCount_returns200() throws Exception {
        when(seedDataService.seedViolations(8)).thenReturn(createSuccessResult("Violations seeded", 8));

        mockMvc.perform(post("/slib/seed/violations")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(8));

        verify(seedDataService).seedViolations(8);
    }

    // =========================================
    // === SEED SUPPORT REQUESTS ===
    // =========================================

    @Test
    @DisplayName("seedSupportRequests_defaultCount_returns200")
    void seedSupportRequests_defaultCount_returns200() throws Exception {
        when(seedDataService.seedSupportRequests(8)).thenReturn(createSuccessResult("Supports seeded", 8));

        mockMvc.perform(post("/slib/seed/support-requests")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(8));

        verify(seedDataService).seedSupportRequests(8);
    }

    // =========================================
    // === SEED COMPLAINTS ===
    // =========================================

    @Test
    @DisplayName("seedComplaints_defaultCount_returns200")
    void seedComplaints_defaultCount_returns200() throws Exception {
        when(seedDataService.seedComplaints(5)).thenReturn(createSuccessResult("Complaints seeded", 5));

        mockMvc.perform(post("/slib/seed/complaints")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));

        verify(seedDataService).seedComplaints(5);
    }

    // =========================================
    // === SEED FEEDBACKS ===
    // =========================================

    @Test
    @DisplayName("seedFeedbacks_defaultCount_returns200")
    void seedFeedbacks_defaultCount_returns200() throws Exception {
        when(seedDataService.seedFeedbacks(8)).thenReturn(createSuccessResult("Feedbacks seeded", 8));

        mockMvc.perform(post("/slib/seed/feedbacks")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(8));

        verify(seedDataService).seedFeedbacks(8);
    }

    // =========================================
    // === CLEAR SEED DATA ===
    // =========================================

    @Test
    @DisplayName("clearSeedData_returns200")
    void clearSeedData_returns200() throws Exception {
        Map<String, Object> result = Map.of("status", "SUCCESS", "message", "Seed data cleared");
        when(seedDataService.clearSeedData()).thenReturn(result);

        mockMvc.perform(delete("/slib/seed/clear")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(seedDataService).clearSeedData();
    }

    // =========================================
    // === CLEAR ALL BOOKINGS ===
    // =========================================

    @Test
    @DisplayName("clearAllBookings_returns200")
    void clearAllBookings_returns200() throws Exception {
        Map<String, Object> result = Map.of("status", "SUCCESS", "message", "All bookings cleared");
        when(seedDataService.clearAllBookings()).thenReturn(result);

        mockMvc.perform(delete("/slib/seed/bookings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(seedDataService).clearAllBookings();
    }

    // =========================================
    // === ERROR HANDLING ===
    // =========================================

    @Test
    @DisplayName("seedBookings_serviceThrowsException_returns4xx")
    void seedBookings_serviceThrowsException_returns4xx() throws Exception {
        when(seedDataService.seedBookings(anyInt())).thenThrow(new RuntimeException("No users found"));

        mockMvc.perform(post("/slib/seed/bookings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }
}
