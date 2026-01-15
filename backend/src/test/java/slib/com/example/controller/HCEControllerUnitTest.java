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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import slib.com.example.controller.hce.HCEController;
import slib.com.example.dto.hce.CheckInRequest;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.CheckInService;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
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

    @MockBean
    private CheckInService checkInService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String VALID_API_KEY = "test-secret-key-123";
    private static final String INVALID_API_KEY = "wrong-key";

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
}
