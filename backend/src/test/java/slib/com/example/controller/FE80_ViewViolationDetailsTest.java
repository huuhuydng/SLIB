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
import slib.com.example.controller.feedback.SeatViolationReportController;
import slib.com.example.dto.feedback.ViolationReportResponse;
import slib.com.example.entity.feedback.SeatViolationReportEntity.ReportStatus;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.feedback.SeatViolationReportService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-80: View Violation Details
 * Test Report: doc/Report/UnitTestReport/FE80_TestReport.md
 */
@WebMvcTest(value = SeatViolationReportController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-80: View Violation Details - Unit Tests")
class FE80_ViewViolationDetailsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SeatViolationReportService violationReportService;

        @MockBean
        private UserRepository userRepository;

        // =========================================
        // === UTCID01: Get all violation reports - Normal ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Get all violation reports returns 200 OK with full list")
        void getAll_withData_returns200OK() throws Exception {
                ViolationReportResponse report1 = ViolationReportResponse.builder()
                                .id(UUID.randomUUID())
                                .violationType("NOISE")
                                .description("Gay on ao trong khu yeu cau im lang")
                                .status("PENDING")
                                .build();
                ViolationReportResponse report2 = ViolationReportResponse.builder()
                                .id(UUID.randomUUID())
                                .violationType("FOOD")
                                .description("An uong trong thu vien")
                                .status("VERIFIED")
                                .build();

                when(violationReportService.getAll()).thenReturn(List.of(report1, report2));

                mockMvc.perform(get("/slib/violation-reports"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].violationType").value("NOISE"));

                verify(violationReportService, times(1)).getAll();
        }

        // =========================================
        // === UTCID02: Get violation reports filtered by PENDING status - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get violation reports by PENDING status returns 200 OK")
        void getByStatus_pending_returns200OK() throws Exception {
                ViolationReportResponse report = ViolationReportResponse.builder()
                                .id(UUID.randomUUID())
                                .violationType("NOISE")
                                .status("PENDING")
                                .build();

                when(violationReportService.getByStatus(eq(ReportStatus.PENDING)))
                                .thenReturn(List.of(report));

                mockMvc.perform(get("/slib/violation-reports")
                                .param("status", "PENDING"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(1));

                verify(violationReportService, times(1)).getByStatus(eq(ReportStatus.PENDING));
        }

        // =========================================
        // === UTCID03: Get violation reports filtered by VERIFIED status - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get violation reports by VERIFIED status returns 200 OK")
        void getByStatus_verified_returns200OK() throws Exception {
                ViolationReportResponse report = ViolationReportResponse.builder()
                                .id(UUID.randomUUID())
                                .violationType("DAMAGE")
                                .status("VERIFIED")
                                .build();

                when(violationReportService.getByStatus(eq(ReportStatus.VERIFIED)))
                                .thenReturn(List.of(report));

                mockMvc.perform(get("/slib/violation-reports")
                                .param("status", "VERIFIED"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());

                verify(violationReportService, times(1)).getByStatus(eq(ReportStatus.VERIFIED));
        }

        // =========================================
        // === UTCID04: Invalid status parameter - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Get violation reports with invalid status returns 400 Bad Request")
        void getByStatus_invalidStatus_returns400BadRequest() throws Exception {
                mockMvc.perform(get("/slib/violation-reports")
                                .param("status", "INVALID_STATUS"))
                                .andExpect(status().isBadRequest());
        }

        // =========================================
        // === UTCID05: Service throws exception on getAll - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get all violation reports when service fails returns 500 Internal Server Error")
        void getAll_serviceFails_returns500InternalServerError() throws Exception {
                when(violationReportService.getAll())
                                .thenThrow(new RuntimeException("Database connection failed"));

                mockMvc.perform(get("/slib/violation-reports"))
                                .andExpect(status().isInternalServerError());

                verify(violationReportService, times(1)).getAll();
        }
}
