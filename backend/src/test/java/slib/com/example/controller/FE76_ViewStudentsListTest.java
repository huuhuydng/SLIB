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
import slib.com.example.service.CheckInService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-76: View Students List
 * Test Report: doc/Report/UnitTestReport/FE76_TestReport.md
 */
@WebMvcTest(value = HCEController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = { "gate.secret=test-secret" })
@DisplayName("FE-76: View Students List - Unit Tests")
class FE76_ViewStudentsListTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private CheckInService checkInService;

        // =========================================
        // === UTCID01: Get latest logs with data - Normal ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Get latest student access logs returns 200 OK with log list")
        void getLatestLogs_withData_returns200OK() throws Exception {
                List<Map<String, Object>> mockLogs = List.of(
                                Map.of("userName", "Nguyen Van A", "action", "CHECK_IN"),
                                Map.of("userName", "Le Thi B", "action", "CHECK_OUT"));

                when(checkInService.getLatest10Logs()).thenReturn(mockLogs);

                mockMvc.perform(get("/slib/hce/latest-logs"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2));

                verify(checkInService, times(1)).getLatest10Logs();
        }

        // =========================================
        // === UTCID02: Get latest logs returns exactly 10 records - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get latest student logs returns up to 10 records")
        void getLatestLogs_maxRecords_returns200OK() throws Exception {
                List<Map<String, Object>> logs = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                        logs.add(Map.of("userName", "Student " + i, "action", "CHECK_IN"));
                }

                when(checkInService.getLatest10Logs()).thenReturn(logs);

                mockMvc.perform(get("/slib/hce/latest-logs"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(10));

                verify(checkInService, times(1)).getLatest10Logs();
        }

        // =========================================
        // === UTCID03: Get all access logs for full student list - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get all access logs for complete student list returns 200 OK")
        void getAllAccessLogs_withData_returns200OK() throws Exception {
                AccessLogDTO log1 = new AccessLogDTO();
                AccessLogDTO log2 = new AccessLogDTO();
                AccessLogDTO log3 = new AccessLogDTO();

                when(checkInService.getAllAccessLogs()).thenReturn(List.of(log1, log2, log3));

                mockMvc.perform(get("/slib/hce/access-logs"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(3));

                verify(checkInService, times(1)).getAllAccessLogs();
        }

        // =========================================
        // === UTCID04: Get latest logs returns empty - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Get latest student logs with no data returns 200 OK with empty list")
        void getLatestLogs_noData_returns200OKEmptyList() throws Exception {
                when(checkInService.getLatest10Logs()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/hce/latest-logs"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(checkInService, times(1)).getLatest10Logs();
        }

        // =========================================
        // === UTCID05: Service throws exception - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get latest logs when service fails returns 200 OK with empty array")
        void getLatestLogs_serviceFails_returns200OKEmptyArray() throws Exception {
                when(checkInService.getLatest10Logs()).thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(get("/slib/hce/latest-logs"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(checkInService, times(1)).getLatest10Logs();
        }
}
