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
import slib.com.example.controller.hce.HCEController;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.hce.CheckInService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-126: Export seat & maintenance report
 * Tests HCEController.exportAccessLogsToExcel() endpoint
 */
@WebMvcTest(value = HCEController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-126: Export seat & maintenance report - Unit Tests")
class FE126_ExportSeatMaintenanceReportTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CheckInService checkInService;

    @Test
    @DisplayName("UTCID01: Export access logs to Excel returns file content")
    void exportAccessLogs_validRequest_returnsExcelFile() throws Exception {
        byte[] excelContent = new byte[]{0x50, 0x4B, 0x03, 0x04}; // dummy xlsx header
        when(checkInService.exportAccessLogsToExcel(any(), any())).thenReturn(excelContent);

        mockMvc.perform(get("/slib/hce/access-logs/export"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("UTCID02: Export access logs with date range returns file")
    void exportAccessLogs_withDateRange_returnsExcelFile() throws Exception {
        byte[] excelContent = new byte[]{0x50, 0x4B, 0x03, 0x04};
        when(checkInService.exportAccessLogsToExcel(any(), any())).thenReturn(excelContent);

        mockMvc.perform(get("/slib/hce/access-logs/export")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("UTCID03: Export access logs when service throws returns bad request")
    void exportAccessLogs_serviceThrows_returnsBadRequest() throws Exception {
        when(checkInService.exportAccessLogsToExcel(any(), any()))
                .thenThrow(new RuntimeException("Export failed"));

        mockMvc.perform(get("/slib/hce/access-logs/export"))
                .andExpect(status().isBadRequest());
    }
}
