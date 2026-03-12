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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.SeatViolationReportService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-98: View Violation Report Details
 * Test Report: doc/Report/UnitTestReport/FE98_TestReport.md
 */
@WebMvcTest(value = SeatViolationReportController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-98: View Violation Report Details - Unit Tests")
class FE98_ViewViolationReportDetailsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeatViolationReportService violationReportService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================
    // === UTCID01: Violation report exists with full detail fields ===
    // =========================================
    @Test
    @DisplayName("UTCID01: Violation report exists with full detail fields - returns 200 OK")
    void getAll_reportWithFullDetails_returns200() throws Exception {
        UUID reportId = UUID.randomUUID();
        ViolationReportResponse dto = ViolationReportResponse.builder()
                .id(reportId)
                .reporterId(UUID.randomUUID())
                .reporterName("Nguyen Van A")
                .seatId(101)
                .seatCode("A-101")
                .violationType("NOISE")
                .violationTypeLabel("Gay on ao")
                .description("Nguoi ngoi canh gay on ao")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        when(violationReportService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/slib/violation-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reportId.toString()))
                .andExpect(jsonPath("$[0].violationType").value("NOISE"))
                .andExpect(jsonPath("$[0].description").value("Nguoi ngoi canh gay on ao"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    // =========================================
    // === UTCID02: Report with violator and verification info ===
    // =========================================
    @Test
    @DisplayName("UTCID02: Report with violator and verification info - returns 200 OK")
    void getAll_reportWithVerification_returns200() throws Exception {
        ViolationReportResponse dto = ViolationReportResponse.builder()
                .id(UUID.randomUUID())
                .reporterName("Nguyen Van A")
                .violatorId(UUID.randomUUID())
                .violatorName("Le Thi B")
                .violationType("LEFT_BELONGINGS")
                .status("VERIFIED")
                .verifiedByName("Thu Thu A")
                .pointDeducted(10)
                .verifiedAt(LocalDateTime.now())
                .build();
        when(violationReportService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/slib/violation-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].violatorName").value("Le Thi B"))
                .andExpect(jsonPath("$[0].verifiedByName").value("Thu Thu A"))
                .andExpect(jsonPath("$[0].pointDeducted").value(10));
    }

    // =========================================
    // === UTCID03: Report with evidence URL ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Report with evidence URL - returns 200 OK")
    void getAll_reportWithEvidence_returns200() throws Exception {
        ViolationReportResponse dto = ViolationReportResponse.builder()
                .id(UUID.randomUUID())
                .violationType("FOOD_DRINK")
                .evidenceUrl("https://cloudinary.com/evidence.jpg")
                .status("PENDING")
                .build();
        when(violationReportService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/slib/violation-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evidenceUrl").value("https://cloudinary.com/evidence.jpg"));
    }

    // =========================================
    // === UTCID04: Empty violation report list ===
    // =========================================
    @Test
    @DisplayName("UTCID04: No violation reports found - returns 200 OK with empty list")
    void getAll_noReports_returns200Empty() throws Exception {
        when(violationReportService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/slib/violation-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =========================================
    // === UTCID05: Repository failure ===
    // =========================================
    @Test
    @DisplayName("UTCID05: Repository failure - returns 500 Internal Server Error")
    void getAll_repositoryFailure_returns500() throws Exception {
        when(violationReportService.getAll()).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/slib/violation-reports"))
                .andExpect(status().isInternalServerError());
    }
}
