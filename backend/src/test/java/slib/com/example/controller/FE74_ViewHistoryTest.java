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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.hce.HCEController;
import slib.com.example.dto.hce.AccessLogDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.hce.CheckInService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-74: View History
 * Test Report: doc/Report/UnitTestReport/FE74_TestReport.md
 */
@WebMvcTest(value = HCEController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = { "gate.secret=test-secret" })
@DisplayName("FE-74: View History - Unit Tests")
class FE74_ViewHistoryTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private CheckInService checkInService;

        // =========================================
        // === UTCID01: Get all access logs - Normal ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Get all access logs returns 200 OK with log list")
        void getAllAccessLogs_withData_returns200OK() throws Exception {
                AccessLogDTO log1 = new AccessLogDTO();
                AccessLogDTO log2 = new AccessLogDTO();

                when(checkInService.getAllAccessLogs()).thenReturn(List.of(log1, log2));

                mockMvc.perform(get("/slib/hce/access-logs"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2));

                verify(checkInService, times(1)).getAllAccessLogs();
        }

        // =========================================
        // === UTCID02: Get access logs with date range - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get access logs filtered by date range returns 200 OK")
        void getAccessLogsByDateRange_validDates_returns200OK() throws Exception {
                AccessLogDTO log1 = new AccessLogDTO();

                when(checkInService.getAccessLogsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(List.of(log1));

                mockMvc.perform(get("/slib/hce/access-logs/filter")
                                .param("startDate", "2026-03-01")
                                .param("endDate", "2026-03-10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());

                verify(checkInService, times(1)).getAccessLogsByDateRange(any(LocalDate.class), any(LocalDate.class));
        }

        // =========================================
        // === UTCID03: Get access logs with only start date - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get access logs with only start date returns 200 OK")
        void getAccessLogsByDateRange_onlyStartDate_returns200OK() throws Exception {
                AccessLogDTO log1 = new AccessLogDTO();

                when(checkInService.getAccessLogsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(List.of(log1));

                mockMvc.perform(get("/slib/hce/access-logs/filter")
                                .param("startDate", "2026-03-01"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());

                verify(checkInService, times(1)).getAccessLogsByDateRange(any(LocalDate.class), any(LocalDate.class));
        }

        // =========================================
        // === UTCID04: Get access logs with no dates returns all logs - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Get access logs without date params returns 200 OK with all logs")
        void getAccessLogsByDateRange_noDates_returnsAll200OK() throws Exception {
                when(checkInService.getAllAccessLogs()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/hce/access-logs/filter"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());

                verify(checkInService, times(1)).getAllAccessLogs();
        }

        // =========================================
        // === UTCID05: Invalid date format - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get access logs with invalid date format returns 400 Bad Request")
        void getAccessLogsByDateRange_invalidDate_returns400BadRequest() throws Exception {
                mockMvc.perform(get("/slib/hce/access-logs/filter")
                                .param("startDate", "not-a-date")
                                .param("endDate", "2026-03-10"))
                                .andExpect(status().isBadRequest());
        }
}
