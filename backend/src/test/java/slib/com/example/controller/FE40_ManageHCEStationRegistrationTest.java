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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.hce.HceStationController;
import slib.com.example.dto.hce.HceStationRequest;
import slib.com.example.dto.hce.HceStationResponse;
import slib.com.example.dto.hce.HceStationStatusRequest;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.HceStationService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-40: Manage HCE Station Registration
 * Test Report: doc/Report/UnitTestReport/FE40_TestReport.md
 */
@WebMvcTest(value = HceStationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-40: Manage HCE Station Registration - Unit Tests")
class FE40_ManageHCEStationRegistrationTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private HceStationService hceStationService;

        @Autowired
        private ObjectMapper objectMapper;

        private HceStationResponse createMockResponse(Integer id, String deviceId) {
                return HceStationResponse.builder()
                                .id(id)
                                .deviceId(deviceId)
                                .deviceName("Tram quet " + id)
                                .deviceType("ENTRY_GATE")
                                .status("ACTIVE")
                                .online(true)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
        }

        // =========================================
        // === UTCID01: Create station - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Create station with valid payload returns 201 Created")
        void createStation_validPayload_returns201Created() throws Exception {
                HceStationRequest request = HceStationRequest.builder()
                                .deviceId("RPI-NEW-001")
                                .deviceName("Cong vao moi")
                                .deviceType("ENTRY_GATE")
                                .location("Tang 1")
                                .status("ACTIVE")
                                .build();

                HceStationResponse response = createMockResponse(1, "RPI-NEW-001");

                when(hceStationService.createStation(any(HceStationRequest.class))).thenReturn(response);

                mockMvc.perform(post("/slib/hce/stations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.deviceId").value("RPI-NEW-001"));

                verify(hceStationService, times(1)).createStation(any(HceStationRequest.class));
        }

        // =========================================
        // === UTCID02: Update station - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Update station with valid payload returns 200 OK")
        void updateStation_validPayload_returns200OK() throws Exception {
                HceStationRequest request = HceStationRequest.builder()
                                .deviceId("RPI-001")
                                .deviceName("Cong vao cap nhat")
                                .deviceType("ENTRY_GATE")
                                .location("Tang 2")
                                .build();

                HceStationResponse response = createMockResponse(1, "RPI-001");
                response.setDeviceName("Cong vao cap nhat");

                when(hceStationService.updateStation(eq(1), any(HceStationRequest.class))).thenReturn(response);

                mockMvc.perform(put("/slib/hce/stations/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(hceStationService, times(1)).updateStation(eq(1), any(HceStationRequest.class));
        }

        // =========================================
        // === UTCID03: Patch station status - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Patch station status with valid value returns 200 OK")
        void patchStationStatus_validStatus_returns200OK() throws Exception {
                HceStationStatusRequest statusRequest = new HceStationStatusRequest("MAINTENANCE");

                HceStationResponse response = createMockResponse(1, "RPI-001");
                response.setStatus("MAINTENANCE");

                when(hceStationService.updateStationStatus(eq(1), any(HceStationStatusRequest.class)))
                                .thenReturn(response);

                mockMvc.perform(patch("/slib/hce/stations/1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(statusRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("MAINTENANCE"));

                verify(hceStationService, times(1)).updateStationStatus(eq(1),
                                any(HceStationStatusRequest.class));
        }

        // =========================================
        // === UTCID04: Delete station - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Delete existing station returns 200 OK")
        void deleteStation_existing_returns200OK() throws Exception {
                doNothing().when(hceStationService).deleteStation(1);

                mockMvc.perform(delete("/slib/hce/stations/1"))
                                .andExpect(status().isOk());

                verify(hceStationService, times(1)).deleteStation(1);
        }

        // =========================================
        // === UTCID05: Duplicate deviceId - Bad Request ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Create station with duplicate deviceId returns 400 Bad Request")
        void createStation_duplicateDeviceId_returns400BadRequest() throws Exception {
                HceStationRequest request = HceStationRequest.builder()
                                .deviceId("RPI-EXISTING")
                                .deviceName("Duplicate")
                                .deviceType("ENTRY_GATE")
                                .build();

                when(hceStationService.createStation(any(HceStationRequest.class)))
                                .thenThrow(new RuntimeException("DeviceId da ton tai"));

                mockMvc.perform(post("/slib/hce/stations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(hceStationService, times(1)).createStation(any(HceStationRequest.class));
        }

        // =========================================
        // === UTCID06: Update non-existent station ===
        // =========================================

        @Test
        @DisplayName("UTCID06: Update non-existent station returns 400 Bad Request")
        void updateStation_nonExistent_returns400BadRequest() throws Exception {
                HceStationRequest request = HceStationRequest.builder()
                                .deviceId("RPI-999")
                                .deviceName("Non-existent")
                                .build();

                when(hceStationService.updateStation(eq(999), any(HceStationRequest.class)))
                                .thenThrow(new RuntimeException("Khong tim thay tram quet voi id: 999"));

                mockMvc.perform(put("/slib/hce/stations/999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(hceStationService, times(1)).updateStation(eq(999), any(HceStationRequest.class));
        }

        // =========================================
        // === UTCID07: Patch status non-existent station ===
        // =========================================

        @Test
        @DisplayName("UTCID07: Patch status of non-existent station returns 400 Bad Request")
        void patchStatus_nonExistent_returns400BadRequest() throws Exception {
                HceStationStatusRequest statusRequest = new HceStationStatusRequest("ACTIVE");

                when(hceStationService.updateStationStatus(eq(999), any(HceStationStatusRequest.class)))
                                .thenThrow(new RuntimeException("Khong tim thay tram quet voi id: 999"));

                mockMvc.perform(patch("/slib/hce/stations/999/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(statusRequest)))
                                .andExpect(status().isBadRequest());

                verify(hceStationService, times(1)).updateStationStatus(eq(999),
                                any(HceStationStatusRequest.class));
        }

        // =========================================
        // === UTCID08: Delete non-existent station ===
        // =========================================

        @Test
        @DisplayName("UTCID08: Delete non-existent station returns 404 Not Found")
        void deleteStation_nonExistent_returns404NotFound() throws Exception {
                doThrow(new RuntimeException("Khong tim thay tram quet voi id: 999"))
                                .when(hceStationService).deleteStation(999);

                mockMvc.perform(delete("/slib/hce/stations/999"))
                                .andExpect(status().isNotFound());

                verify(hceStationService, times(1)).deleteStation(999);
        }
}
