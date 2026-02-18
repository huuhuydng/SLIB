package slib.com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import slib.com.example.controller.hce.HCEController;
import slib.com.example.dto.hce.AccessLogDTO;
import slib.com.example.dto.hce.AccessLogStatsDTO;
import slib.com.example.dto.hce.CheckInRequest;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.CheckInService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for HCEController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 * Test Type: @WebMvcTest (Unit Tests only - no full context)
 */
@WebMvcTest(value = HCEController.class, 
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, 
    classes = {slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"gate.secret=test-secret-key-123"})
@DisplayName("HCEController Unit Tests")
class HCEControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckInService checkInService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String VALID_API_KEY = "test-secret-key-123";
    private static final String INVALID_API_KEY = "wrong-key";

    // Test data
    private List<AccessLogDTO> mockAccessLogs;
    private AccessLogStatsDTO mockStats;

    @BeforeEach
    void setUp() {
        // Setup mock access logs
        mockAccessLogs = new ArrayList<>();
        
        AccessLogDTO log1 = AccessLogDTO.builder()
                .logId(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
                .userId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                .userName("Nguyen Van A")
                .userCode("SL000001")
                .action("CHECK_IN")
                .checkInTime(LocalDateTime.now().minusHours(2))
                .checkOutTime(null)
                .deviceId("GATE-001")
                .build();
        
        AccessLogDTO log2 = AccessLogDTO.builder()
                .logId(UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901"))
                .userId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"))
                .userName("Tran Thi B")
                .userCode("SL000002")
                .action("CHECK_OUT")
                .checkInTime(LocalDateTime.now().minusHours(3))
                .checkOutTime(LocalDateTime.now().minusHours(1))
                .deviceId("GATE-002")
                .build();
        
        mockAccessLogs.add(log1);
        mockAccessLogs.add(log2);

        // Setup mock stats
        mockStats = AccessLogStatsDTO.builder()
                .totalCheckInsToday(15L)
                .totalCheckOutsToday(10L)
                .currentlyInLibrary(5L)
                .build();
    }

    // =============================================
    // === CHECK IN ENDPOINT ===
    // =============================================

    @Test
    @DisplayName("checkIn_validRequestWithValidApiKey_returns200WithSuccess")
    void checkIn_validRequestWithValidApiKey_returns200WithSuccess() throws Exception {
        // Arrange
        CheckInRequest request = new CheckInRequest();
        request.setToken("valid-hce-token-abc123");
        request.setGateId("GATE_001");

        Map<String, String> serviceResponse = new HashMap<>();
        serviceResponse.put("status", "SUCCESS");
        serviceResponse.put("message", "Check-in thành công");
        serviceResponse.put("userName", "Nguyễn Văn A");
        serviceResponse.put("studentCode", "SV001");

        when(checkInService.processCheckIn(any(CheckInRequest.class))).thenReturn(serviceResponse);

        // Act & Assert
        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Check-in thành công"))
                .andExpect(jsonPath("$.userName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.studentCode").value("SV001"));

        verify(checkInService, times(1)).processCheckIn(any(CheckInRequest.class));
    }

    @Test
    @DisplayName("checkIn_missingApiKey_returns403Forbidden")
    void checkIn_missingApiKey_returns403Forbidden() throws Exception {
        // Arrange
        CheckInRequest request = new CheckInRequest();
        request.setToken("valid-token");
        request.setGateId("GATE_001");

        // Act & Assert
        mockMvc.perform(post("/slib/hce/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Truy cập bị từ chối: Sai API Key bảo mật"));

        verify(checkInService, never()).processCheckIn(any());
    }

    @Test
    @DisplayName("checkIn_invalidApiKey_returns403Forbidden")
    void checkIn_invalidApiKey_returns403Forbidden() throws Exception {
        // Arrange
        CheckInRequest request = new CheckInRequest();
        request.setToken("valid-token");
        request.setGateId("GATE_001");

        // Act & Assert
        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", INVALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Truy cập bị từ chối: Sai API Key bảo mật"));

        verify(checkInService, never()).processCheckIn(any());
    }

    @Test
    @DisplayName("checkIn_emptyApiKey_returns403Forbidden")
    void checkIn_emptyApiKey_returns403Forbidden() throws Exception {
        // Arrange
        CheckInRequest request = new CheckInRequest();
        request.setToken("valid-token");
        request.setGateId("GATE_001");

        // Act & Assert
        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Truy cập bị từ chối: Sai API Key bảo mật"));

        verify(checkInService, never()).processCheckIn(any());
    }

    @Test
    @DisplayName("checkIn_invalidToken_returns400WithError")
    void checkIn_invalidToken_returns400WithError() throws Exception {
        // Arrange
        CheckInRequest request = new CheckInRequest();
        request.setToken("invalid-token");
        request.setGateId("GATE_001");

        when(checkInService.processCheckIn(any(CheckInRequest.class)))
                .thenThrow(new RuntimeException("Token không hợp lệ"));

        // Act & Assert
        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Token không hợp lệ"));

        verify(checkInService, times(1)).processCheckIn(any(CheckInRequest.class));
    }

    @Test
    @DisplayName("checkIn_userAlreadyCheckedIn_returns400WithError")
    void checkIn_userAlreadyCheckedIn_returns400WithError() throws Exception {
        // Arrange
        CheckInRequest request = new CheckInRequest();
        request.setToken("valid-token");
        request.setGateId("GATE_001");

        when(checkInService.processCheckIn(any(CheckInRequest.class)))
                .thenThrow(new RuntimeException("Người dùng đã check-in rồi"));

        // Act & Assert
        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Người dùng đã check-in rồi"));

        verify(checkInService, times(1)).processCheckIn(any(CheckInRequest.class));
    }

    @Test
    @DisplayName("checkIn_emptyRequestBody_returns400")
    void checkIn_emptyRequestBody_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        verify(checkInService, never()).processCheckIn(any());
    }

    @Test
    @DisplayName("checkIn_invalidJson_returns400")
    void checkIn_invalidJson_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verify(checkInService, never()).processCheckIn(any());
    }

    @Test
    @DisplayName("checkIn_missingTokenField_returns400WithError")
    void checkIn_missingTokenField_returns400WithError() throws Exception {
        // Arrange
        CheckInRequest request = new CheckInRequest();
        request.setGateId("GATE_001");
        // token is null

        when(checkInService.processCheckIn(any(CheckInRequest.class)))
                .thenThrow(new RuntimeException("Token không được để trống"));

        // Act & Assert
        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Token không được để trống"));

        verify(checkInService, times(1)).processCheckIn(any(CheckInRequest.class));
    }

    @Test
    @DisplayName("checkIn_missingGateIdField_returns400WithError")
    void checkIn_missingGateIdField_returns400WithError() throws Exception {
        // Arrange
        CheckInRequest request = new CheckInRequest();
        request.setToken("valid-token");
        // gateId is null

        when(checkInService.processCheckIn(any(CheckInRequest.class)))
                .thenThrow(new RuntimeException("Gate ID không được để trống"));

        // Act & Assert
        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Gate ID không được để trống"));

        verify(checkInService, times(1)).processCheckIn(any(CheckInRequest.class));
    }

    @Test
    @DisplayName("checkIn_userNotActive_returns400WithError")
    void checkIn_userNotActive_returns400WithError() throws Exception {
        // Arrange
        CheckInRequest request = new CheckInRequest();
        request.setToken("valid-token-inactive-user");
        request.setGateId("GATE_001");

        when(checkInService.processCheckIn(any(CheckInRequest.class)))
                .thenThrow(new RuntimeException("Tài khoản không hoạt động"));

        // Act & Assert
        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Tài khoản không hoạt động"));

        verify(checkInService, times(1)).processCheckIn(any(CheckInRequest.class));
    }

    @Test
    @DisplayName("checkIn_validCheckout_returns200WithSuccess")
    void checkIn_validCheckout_returns200WithSuccess() throws Exception {
        // Arrange
        CheckInRequest request = new CheckInRequest();
        request.setToken("valid-token-for-checkout");
        request.setGateId("GATE_EXIT");

        Map<String, String> serviceResponse = new HashMap<>();
        serviceResponse.put("status", "SUCCESS");
        serviceResponse.put("message", "Check-out thành công");
        serviceResponse.put("userName", "Trần Thị B");
        serviceResponse.put("duration", "2 giờ 30 phút");

        when(checkInService.processCheckIn(any(CheckInRequest.class))).thenReturn(serviceResponse);

        // Act & Assert
        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Check-out thành công"))
                .andExpect(jsonPath("$.duration").value("2 giờ 30 phút"));

        verify(checkInService, times(1)).processCheckIn(any(CheckInRequest.class));
    }

    @Test
    @DisplayName("checkIn_databaseError_returns400WithError")
    void checkIn_databaseError_returns400WithError() throws Exception {
        // Arrange
        CheckInRequest request = new CheckInRequest();
        request.setToken("valid-token");
        request.setGateId("GATE_001");

        when(checkInService.processCheckIn(any(CheckInRequest.class)))
                .thenThrow(new RuntimeException("Lỗi kết nối cơ sở dữ liệu"));

        // Act & Assert
        mockMvc.perform(post("/slib/hce/checkin")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Lỗi kết nối cơ sở dữ liệu"));

        verify(checkInService, times(1)).processCheckIn(any(CheckInRequest.class));
    }

    // =============================================
    // === ACCESS LOGS ENDPOINTS - CheckInOut Page ===
    // =============================================

    @Test
    @DisplayName("getAllAccessLogs_returnsListOfLogs_200OK")
    void getAllAccessLogs_returnsListOfLogs_200OK() throws Exception {
        // Arrange
        when(checkInService.getAllAccessLogs()).thenReturn(mockAccessLogs);

        // Act & Assert
        mockMvc.perform(get("/slib/hce/access-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userName").value("Nguyen Van A"))
                .andExpect(jsonPath("$[0].action").value("CHECK_IN"))
                .andExpect(jsonPath("$[1].userName").value("Tran Thi B"))
                .andExpect(jsonPath("$[1].action").value("CHECK_OUT"));

        verify(checkInService, times(1)).getAllAccessLogs();
    }

    @Test
    @DisplayName("getAllAccessLogs_emptyList_returns200")
    void getAllAccessLogs_emptyList_returns200() throws Exception {
        // Arrange
        when(checkInService.getAllAccessLogs()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/slib/hce/access-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(checkInService, times(1)).getAllAccessLogs();
    }

    @Test
    @DisplayName("getAllAccessLogs_serviceThrowsException_returns400")
    void getAllAccessLogs_serviceThrowsException_returns400() throws Exception {
        // Arrange
        when(checkInService.getAllAccessLogs()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/slib/hce/access-logs"))
                .andExpect(status().isBadRequest());

        verify(checkInService, times(1)).getAllAccessLogs();
    }

    @Test
    @DisplayName("getTodayAccessLogs_returnsListOfLogs_200OK")
    void getTodayAccessLogs_returnsListOfLogs_200OK() throws Exception {
        // Arrange
        when(checkInService.getTodayAccessLogs()).thenReturn(mockAccessLogs);

        // Act & Assert
        mockMvc.perform(get("/slib/hce/access-logs/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(checkInService, times(1)).getTodayAccessLogs();
    }

    @Test
    @DisplayName("getTodayAccessLogs_emptyList_returns200")
    void getTodayAccessLogs_emptyList_returns200() throws Exception {
        // Arrange
        when(checkInService.getTodayAccessLogs()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/slib/hce/access-logs/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(checkInService, times(1)).getTodayAccessLogs();
    }

    @Test
    @DisplayName("getTodayStats_returnsStats_200OK")
    void getTodayStats_returnsStats_200OK() throws Exception {
        // Arrange
        when(checkInService.getTodayStats()).thenReturn(mockStats);

        // Act & Assert
        mockMvc.perform(get("/slib/hce/access-logs/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCheckInsToday").value(15))
                .andExpect(jsonPath("$.totalCheckOutsToday").value(10))
                .andExpect(jsonPath("$.currentlyInLibrary").value(5));

        verify(checkInService, times(1)).getTodayStats();
    }

    @Test
    @DisplayName("getTodayStats_serviceThrowsException_returns400")
    void getTodayStats_serviceThrowsException_returns400() throws Exception {
        // Arrange
        when(checkInService.getTodayStats()).thenThrow(new RuntimeException("Error calculating stats"));

        // Act & Assert
        mockMvc.perform(get("/slib/hce/access-logs/stats"))
                .andExpect(status().isBadRequest());

        verify(checkInService, times(1)).getTodayStats();
    }

    @Test
    @DisplayName("filterAccessLogs_withBothDates_returnsFilteredLogs_200OK")
    void filterAccessLogs_withBothDates_returnsFilteredLogs_200OK() throws Exception {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 10);
        when(checkInService.getAccessLogsByDateRange(startDate, endDate)).thenReturn(mockAccessLogs);

        // Act & Assert
        mockMvc.perform(get("/slib/hce/access-logs/filter")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(checkInService, times(1)).getAccessLogsByDateRange(eq(startDate), eq(endDate));
    }

    @Test
    @DisplayName("filterAccessLogs_onlyStartDate_usesTodayAsEndDate_200OK")
    void filterAccessLogs_onlyStartDate_usesTodayAsEndDate_200OK() throws Exception {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        when(checkInService.getAccessLogsByDateRange(eq(startDate), any(LocalDate.class)))
                .thenReturn(mockAccessLogs);

        // Act & Assert
        mockMvc.perform(get("/slib/hce/access-logs/filter")
                        .param("startDate", "2026-02-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(checkInService, times(1)).getAccessLogsByDateRange(eq(startDate), any(LocalDate.class));
    }

    @Test
    @DisplayName("filterAccessLogs_onlyEndDate_uses30DaysBeforeAsStartDate_200OK")
    void filterAccessLogs_onlyEndDate_uses30DaysBeforeAsStartDate_200OK() throws Exception {
        // Arrange
        LocalDate endDate = LocalDate.of(2026, 2, 10);
        LocalDate expectedStartDate = endDate.minusDays(30);
        when(checkInService.getAccessLogsByDateRange(expectedStartDate, endDate))
                .thenReturn(mockAccessLogs);

        // Act & Assert
        mockMvc.perform(get("/slib/hce/access-logs/filter")
                        .param("endDate", "2026-02-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(checkInService, times(1)).getAccessLogsByDateRange(eq(expectedStartDate), eq(endDate));
    }

    @Test
    @DisplayName("filterAccessLogs_noDates_returnsAllLogs_200OK")
    void filterAccessLogs_noDates_returnsAllLogs_200OK() throws Exception {
        // Arrange
        when(checkInService.getAllAccessLogs()).thenReturn(mockAccessLogs);

        // Act & Assert
        mockMvc.perform(get("/slib/hce/access-logs/filter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(checkInService, times(1)).getAllAccessLogs();
        verify(checkInService, never()).getAccessLogsByDateRange(any(), any());
    }

    @Test
    @DisplayName("filterAccessLogs_invalidDateFormat_returns400")
    void filterAccessLogs_invalidDateFormat_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/slib/hce/access-logs/filter")
                        .param("startDate", "invalid-date"))
                .andExpect(status().isBadRequest());

        verify(checkInService, never()).getAccessLogsByDateRange(any(), any());
    }

    @Test
    @DisplayName("exportToExcel_withDateRange_returnsExcelFile_200OK")
    void exportToExcel_withDateRange_returnsExcelFile_200OK() throws Exception {
        // Arrange
        byte[] mockExcelData = "Mock Excel Content".getBytes();
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 10);
        when(checkInService.exportAccessLogsToExcel(startDate, endDate)).thenReturn(mockExcelData);

        // Act & Assert
        mockMvc.perform(get("/slib/hce/access-logs/export")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-10"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(mockExcelData));

        verify(checkInService, times(1)).exportAccessLogsToExcel(eq(startDate), eq(endDate));
    }

    @Test
    @DisplayName("exportToExcel_withoutDates_returnsAllDataExcel_200OK")
    void exportToExcel_withoutDates_returnsAllDataExcel_200OK() throws Exception {
        // Arrange
        byte[] mockExcelData = "Mock Excel All Data".getBytes();
        when(checkInService.exportAccessLogsToExcel(null, null)).thenReturn(mockExcelData);

        // Act & Assert
        mockMvc.perform(get("/slib/hce/access-logs/export"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));

        verify(checkInService, times(1)).exportAccessLogsToExcel(null, null);
    }

    @Test
    @DisplayName("exportToExcel_serviceThrowsException_returns400")
    void exportToExcel_serviceThrowsException_returns400() throws Exception {
        // Arrange
        when(checkInService.exportAccessLogsToExcel(any(), any()))
                .thenThrow(new RuntimeException("Export error"));

        // Act & Assert
        mockMvc.perform(get("/slib/hce/access-logs/export")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-10"))
                .andExpect(status().isBadRequest());

        verify(checkInService, times(1)).exportAccessLogsToExcel(any(LocalDate.class), any(LocalDate.class));
    }
}
