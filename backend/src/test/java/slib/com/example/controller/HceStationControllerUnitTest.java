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

import slib.com.example.controller.hce.HceStationController;
import slib.com.example.dto.hce.HceStationRequest;
import slib.com.example.dto.hce.HceStationResponse;
import slib.com.example.dto.hce.HceStationStatusRequest;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.HceStationService;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for HceStationController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 * Covers: FE-39 (View Stations), FE-40 (Manage Registration), FE-41 (View
 * Details)
 */
@WebMvcTest(value = HceStationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = { "gate.secret=test-secret-key-123" })
@DisplayName("HceStationController Unit Tests")
class HceStationControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HceStationService hceStationService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String VALID_API_KEY = "test-secret-key-123";
    private static final String INVALID_API_KEY = "wrong-key";

    private HceStationResponse mockStation1;
    private HceStationResponse mockStation2;

    @BeforeEach
    void setUp() {
        mockStation1 = HceStationResponse.builder()
                .id(1)
                .deviceId("GATE_01")
                .deviceName("Cổng vào chính")
                .location("Tầng 1 - Cửa vào")
                .deviceType("ENTRY_GATE")
                .status("ACTIVE")
                .lastHeartbeat(LocalDateTime.now().minusSeconds(30))
                .online(true)
                .areaId(1L)
                .areaName("Khu vực A")
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now())
                .todayScanCount(25)
                .lastAccessTime(LocalDateTime.now().minusMinutes(5))
                .build();

        mockStation2 = HceStationResponse.builder()
                .id(2)
                .deviceId("GATE_02")
                .deviceName("Cổng ra tầng 2")
                .location("Tầng 2 - Cửa ra")
                .deviceType("EXIT_GATE")
                .status("ACTIVE")
                .lastHeartbeat(LocalDateTime.now().minusMinutes(5))
                .online(false)
                .createdAt(LocalDateTime.now().minusDays(15))
                .updatedAt(LocalDateTime.now())
                .todayScanCount(10)
                .build();
    }

    // =============================================
    // === GET LIST STATIONS ===
    // =============================================

    @Test
    @DisplayName("getAllStations_returnsListOfStations_200OK")
    void getAllStations_returnsListOfStations_200OK() throws Exception {
        when(hceStationService.getAllStations(any(), any(), any()))
                .thenReturn(List.of(mockStation1, mockStation2));

        mockMvc.perform(get("/slib/hce/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].deviceId").value("GATE_01"))
                .andExpect(jsonPath("$[0].deviceName").value("Cổng vào chính"))
                .andExpect(jsonPath("$[0].online").value(true))
                .andExpect(jsonPath("$[1].deviceId").value("GATE_02"));

        verify(hceStationService, times(1)).getAllStations(any(), any(), any());
    }

    @Test
    @DisplayName("getAllStations_emptyList_returns200")
    void getAllStations_emptyList_returns200() throws Exception {
        when(hceStationService.getAllStations(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/slib/hce/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("getAllStations_withSearchFilter_returns200")
    void getAllStations_withSearchFilter_returns200() throws Exception {
        when(hceStationService.getAllStations(eq("GATE"), any(), any()))
                .thenReturn(List.of(mockStation1));

        mockMvc.perform(get("/slib/hce/stations")
                .param("search", "GATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(hceStationService, times(1)).getAllStations(eq("GATE"), any(), any());
    }

    @Test
    @DisplayName("getAllStations_withStatusFilter_returns200")
    void getAllStations_withStatusFilter_returns200() throws Exception {
        when(hceStationService.getAllStations(any(), eq("ACTIVE"), any()))
                .thenReturn(List.of(mockStation1, mockStation2));

        mockMvc.perform(get("/slib/hce/stations")
                .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(hceStationService, times(1)).getAllStations(any(), eq("ACTIVE"), any());
    }

    // =============================================
    // === GET STATION BY ID ===
    // =============================================

    @Test
    @DisplayName("getStationById_existingStation_returns200")
    void getStationById_existingStation_returns200() throws Exception {
        when(hceStationService.getStationById(1)).thenReturn(mockStation1);

        mockMvc.perform(get("/slib/hce/stations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("GATE_01"))
                .andExpect(jsonPath("$.deviceName").value("Cổng vào chính"))
                .andExpect(jsonPath("$.online").value(true))
                .andExpect(jsonPath("$.todayScanCount").value(25));

        verify(hceStationService, times(1)).getStationById(1);
    }

    @Test
    @DisplayName("getStationById_notFound_returns404")
    void getStationById_notFound_returns404() throws Exception {
        when(hceStationService.getStationById(999))
                .thenThrow(new RuntimeException("Không tìm thấy trạm quét với ID: 999"));

        mockMvc.perform(get("/slib/hce/stations/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy trạm quét với ID: 999"));
    }

    // =============================================
    // === CREATE STATION ===
    // =============================================

    @Test
    @DisplayName("createStation_validRequest_returns201")
    void createStation_validRequest_returns201() throws Exception {
        HceStationRequest request = HceStationRequest.builder()
                .deviceId("GATE_03")
                .deviceName("Cổng vào tầng 3")
                .deviceType("ENTRY_GATE")
                .location("Tầng 3 - Cửa vào")
                .status("ACTIVE")
                .build();

        HceStationResponse response = HceStationResponse.builder()
                .id(3)
                .deviceId("GATE_03")
                .deviceName("Cổng vào tầng 3")
                .deviceType("ENTRY_GATE")
                .status("ACTIVE")
                .online(false)
                .build();

        when(hceStationService.createStation(any(HceStationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/slib/hce/stations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.deviceId").value("GATE_03"));

        verify(hceStationService, times(1)).createStation(any(HceStationRequest.class));
    }

    @Test
    @DisplayName("createStation_duplicateDeviceId_returns400")
    void createStation_duplicateDeviceId_returns400() throws Exception {
        HceStationRequest request = HceStationRequest.builder()
                .deviceId("GATE_01")
                .deviceName("Duplicate Gate")
                .deviceType("ENTRY_GATE")
                .build();

        when(hceStationService.createStation(any(HceStationRequest.class)))
                .thenThrow(new RuntimeException("Mã trạm 'GATE_01' đã tồn tại trong hệ thống"));

        mockMvc.perform(post("/slib/hce/stations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Mã trạm 'GATE_01' đã tồn tại trong hệ thống"));
    }

    @Test
    @DisplayName("createStation_missingDeviceId_returns400")
    void createStation_missingDeviceId_returns400() throws Exception {
        HceStationRequest request = HceStationRequest.builder()
                .deviceName("Gate without ID")
                .deviceType("ENTRY_GATE")
                .build();

        when(hceStationService.createStation(any(HceStationRequest.class)))
                .thenThrow(new RuntimeException("Mã trạm (deviceId) không được để trống"));

        mockMvc.perform(post("/slib/hce/stations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Mã trạm (deviceId) không được để trống"));
    }

    @Test
    @DisplayName("createStation_invalidDeviceType_returns400")
    void createStation_invalidDeviceType_returns400() throws Exception {
        HceStationRequest request = HceStationRequest.builder()
                .deviceId("GATE_NEW")
                .deviceName("Invalid Type Gate")
                .deviceType("INVALID_TYPE")
                .build();

        when(hceStationService.createStation(any(HceStationRequest.class)))
                .thenThrow(new RuntimeException("Loại trạm không hợp lệ: INVALID_TYPE"));

        mockMvc.perform(post("/slib/hce/stations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Loại trạm không hợp lệ: INVALID_TYPE"));
    }

    // =============================================
    // === UPDATE STATION ===
    // =============================================

    @Test
    @DisplayName("updateStation_validRequest_returns200")
    void updateStation_validRequest_returns200() throws Exception {
        HceStationRequest request = HceStationRequest.builder()
                .deviceName("Cổng vào chính - Updated")
                .location("Tầng 1 - Cửa vào chính (mới)")
                .build();

        HceStationResponse response = HceStationResponse.builder()
                .id(1)
                .deviceId("GATE_01")
                .deviceName("Cổng vào chính - Updated")
                .location("Tầng 1 - Cửa vào chính (mới)")
                .deviceType("ENTRY_GATE")
                .status("ACTIVE")
                .online(true)
                .build();

        when(hceStationService.updateStation(eq(1), any(HceStationRequest.class))).thenReturn(response);

        mockMvc.perform(put("/slib/hce/stations/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceName").value("Cổng vào chính - Updated"));

        verify(hceStationService, times(1)).updateStation(eq(1), any(HceStationRequest.class));
    }

    @Test
    @DisplayName("updateStation_notFound_returns400")
    void updateStation_notFound_returns400() throws Exception {
        HceStationRequest request = HceStationRequest.builder()
                .deviceName("Non-existent")
                .build();

        when(hceStationService.updateStation(eq(999), any(HceStationRequest.class)))
                .thenThrow(new RuntimeException("Không tìm thấy trạm quét với ID: 999"));

        mockMvc.perform(put("/slib/hce/stations/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không tìm thấy trạm quét với ID: 999"));
    }

    // =============================================
    // === PATCH STATUS ===
    // =============================================

    @Test
    @DisplayName("updateStationStatus_validStatus_returns200")
    void updateStationStatus_validStatus_returns200() throws Exception {
        HceStationStatusRequest request = new HceStationStatusRequest("MAINTENANCE");

        HceStationResponse response = HceStationResponse.builder()
                .id(1)
                .deviceId("GATE_01")
                .status("MAINTENANCE")
                .build();

        when(hceStationService.updateStationStatus(eq(1), any(HceStationStatusRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/slib/hce/stations/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));
    }

    @Test
    @DisplayName("updateStationStatus_invalidStatus_returns400")
    void updateStationStatus_invalidStatus_returns400() throws Exception {
        HceStationStatusRequest request = new HceStationStatusRequest("INVALID");

        when(hceStationService.updateStationStatus(eq(1), any(HceStationStatusRequest.class)))
                .thenThrow(new RuntimeException("Trạng thái không hợp lệ: INVALID"));

        mockMvc.perform(patch("/slib/hce/stations/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Trạng thái không hợp lệ: INVALID"));
    }

    // =============================================
    // === HEARTBEAT ===
    // =============================================

    @Test
    @DisplayName("heartbeat_validApiKey_returns200")
    void heartbeat_validApiKey_returns200() throws Exception {
        doNothing().when(hceStationService).processHeartbeat("GATE_01");

        mockMvc.perform(post("/slib/hce/stations/GATE_01/heartbeat")
                .header("X-API-KEY", VALID_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.deviceId").value("GATE_01"));

        verify(hceStationService, times(1)).processHeartbeat("GATE_01");
    }

    @Test
    @DisplayName("heartbeat_missingApiKey_returns403")
    void heartbeat_missingApiKey_returns403() throws Exception {
        mockMvc.perform(post("/slib/hce/stations/GATE_01/heartbeat"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"));

        verify(hceStationService, never()).processHeartbeat(any());
    }

    @Test
    @DisplayName("heartbeat_invalidApiKey_returns403")
    void heartbeat_invalidApiKey_returns403() throws Exception {
        mockMvc.perform(post("/slib/hce/stations/GATE_01/heartbeat")
                .header("X-API-KEY", INVALID_API_KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"));

        verify(hceStationService, never()).processHeartbeat(any());
    }

    @Test
    @DisplayName("heartbeat_stationNotFound_returns400")
    void heartbeat_stationNotFound_returns400() throws Exception {
        doThrow(new RuntimeException("Trạm quét 'GATE_UNKNOWN' chưa được đăng ký trong hệ thống"))
                .when(hceStationService).processHeartbeat("GATE_UNKNOWN");

        mockMvc.perform(post("/slib/hce/stations/GATE_UNKNOWN/heartbeat")
                .header("X-API-KEY", VALID_API_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Trạm quét 'GATE_UNKNOWN' chưa được đăng ký trong hệ thống"));
    }

    @Test
    @DisplayName("heartbeat_stationInactive_returns400")
    void heartbeat_stationInactive_returns400() throws Exception {
        doThrow(new RuntimeException("Trạm quét 'GATE_01' đang bị vô hiệu hóa (INACTIVE)"))
                .when(hceStationService).processHeartbeat("GATE_01");

        mockMvc.perform(post("/slib/hce/stations/GATE_01/heartbeat")
                .header("X-API-KEY", VALID_API_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Trạm quét 'GATE_01' đang bị vô hiệu hóa (INACTIVE)"));
    }
}
