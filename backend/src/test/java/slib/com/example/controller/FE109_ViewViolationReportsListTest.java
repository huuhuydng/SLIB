package slib.com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import slib.com.example.controller.feedback.SeatViolationReportController;
import slib.com.example.dto.feedback.ViolationReportResponse;
import slib.com.example.entity.feedback.SeatViolationReportEntity.ReportStatus;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.feedback.SeatViolationReportService;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-109: View list of seat violation reports
 * Test Report: doc/Report/UnitTestReport/FE96_TestReport.md
 */
@WebMvcTest(value = SeatViolationReportController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-109: View list of seat violation reports - Unit Tests")
class FE109_ViewViolationReportsListTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeatViolationReportService violationReportService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================
    // === UTCID01: Get all violation reports without filter ===
    // =========================================
    @Test
    @DisplayName("UTCID01: Get all violation reports without status filter - returns 200 OK")
    void getAll_noFilter_returns200() throws Exception {
        List<ViolationReportResponse> reports = List.of(
                ViolationReportResponse.builder().id(UUID.randomUUID()).violationType("NOISE").status("PENDING").build(),
                ViolationReportResponse.builder().id(UUID.randomUUID()).violationType("LEFT_BELONGINGS").status("VERIFIED").build());
        when(violationReportService.getAll()).thenReturn(reports);

        mockMvc.perform(get("/slib/violation-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(violationReportService, times(1)).getAll();
        verify(violationReportService, never()).getByStatus(any());
    }

    // =========================================
    // === UTCID02: Filter by PENDING status ===
    // =========================================
    @Test
    @DisplayName("UTCID02: Filter violation reports by PENDING status - returns 200 OK")
    void getAll_filterPending_returns200() throws Exception {
        List<ViolationReportResponse> pendingList = List.of(
                ViolationReportResponse.builder().id(UUID.randomUUID()).violationType("NOISE").status("PENDING").build());
        when(violationReportService.getByStatus(ReportStatus.PENDING)).thenReturn(pendingList);

        mockMvc.perform(get("/slib/violation-reports").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(violationReportService, times(1)).getByStatus(ReportStatus.PENDING);
        verify(violationReportService, never()).getAll();
    }

    // =========================================
    // === UTCID03: Filter by VERIFIED status ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Filter violation reports by VERIFIED status - returns 200 OK")
    void getAll_filterVerified_returns200() throws Exception {
        List<ViolationReportResponse> verifiedList = List.of(
                ViolationReportResponse.builder().id(UUID.randomUUID()).violationType("LEFT_BELONGINGS").status("VERIFIED").build());
        when(violationReportService.getByStatus(ReportStatus.VERIFIED)).thenReturn(verifiedList);

        mockMvc.perform(get("/slib/violation-reports").param("status", "VERIFIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("VERIFIED"));

        verify(violationReportService, times(1)).getByStatus(ReportStatus.VERIFIED);
    }

    // =========================================
    // === UTCID04: Empty list with valid status filter ===
    // =========================================
    @Test
    @DisplayName("UTCID04: No reports match filter - returns 400 (invalid status)")
    void getAll_invalidStatusFilter_returns400() throws Exception {
        mockMvc.perform(get("/slib/violation-reports").param("status", "NONEXISTENT"))
                .andExpect(status().isBadRequest());

        verify(violationReportService, never()).getAll();
        verify(violationReportService, never()).getByStatus(any());
    }

    // =========================================
    // === UTCID05: Repository failure ===
    // =========================================
    @Test
    @DisplayName("UTCID05: Repository failure - returns 500 Internal Server Error")
    void getAll_repositoryFailure_returns500() throws Exception {
        when(violationReportService.getAll()).thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/slib/violation-reports"))
                .andExpect(status().isInternalServerError());
    }
}
