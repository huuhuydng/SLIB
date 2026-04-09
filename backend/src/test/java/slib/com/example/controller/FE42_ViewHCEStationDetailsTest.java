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
import slib.com.example.controller.hce.HceStationController;
import slib.com.example.dto.hce.HceStationResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import org.springframework.test.context.TestPropertySource;
import slib.com.example.service.hce.HceStationService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-42: View HCE scan stations details
 * Test Report: doc/Report/UnitTestReport/FE41_TestReport.md
 */
@WebMvcTest(value = HceStationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "gate.secret=test-secret")
@DisplayName("FE-42: View HCE scan stations details - Unit Tests")
class FE42_ViewHCEStationDetailsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private HceStationService hceStationService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Get station details - Online ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Get existing station details (online) returns 200 OK")
        void getStationById_existingOnline_returns200OK() throws Exception {
                HceStationResponse response = HceStationResponse.builder()
                                .id(1)
                                .deviceId("RPI-001")
                                .deviceName("Cong vao 1")
                                .location("Tang 1 - Loi vao chinh")
                                .deviceType("ENTRY_GATE")
                                .status("ACTIVE")
                                .online(true)
                                .lastHeartbeat(LocalDateTime.now())
                                .todayScanCount(42)
                                .build();

                when(hceStationService.getStationById(1)).thenReturn(response);

                mockMvc.perform(get("/slib/hce/stations/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.deviceId").value("RPI-001"))
                                .andExpect(jsonPath("$.online").value(true));

                verify(hceStationService, times(1)).getStationById(1);
        }

        // =========================================
        // === UTCID02: Get station details - Offline ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get existing station details (offline) returns 200 OK")
        void getStationById_existingOffline_returns200OK() throws Exception {
                HceStationResponse response = HceStationResponse.builder()
                                .id(2)
                                .deviceId("RPI-002")
                                .deviceName("Cong ra 1")
                                .location("Tang 1 - Loi ra")
                                .deviceType("EXIT_GATE")
                                .status("ACTIVE")
                                .online(false)
                                .lastHeartbeat(LocalDateTime.now().minusHours(2))
                                .todayScanCount(0)
                                .build();

                when(hceStationService.getStationById(2)).thenReturn(response);

                mockMvc.perform(get("/slib/hce/stations/2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.deviceId").value("RPI-002"))
                                .andExpect(jsonPath("$.online").value(false));

                verify(hceStationService, times(1)).getStationById(2);
        }

        // =========================================
        // === UTCID03: Non-existing station ID ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get station with non-existing ID returns 404 Not Found")
        void getStationById_nonExisting_returns404NotFound() throws Exception {
                when(hceStationService.getStationById(999))
                                .thenThrow(new RuntimeException("Khong tim thay tram quet voi id: 999"));

                mockMvc.perform(get("/slib/hce/stations/999"))
                                .andExpect(status().isNotFound());

                verify(hceStationService, times(1)).getStationById(999);
        }

        // =========================================
        // === UTCID04: Invalid path ID ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Get station with invalid path ID returns 400 Bad Request")
        void getStationById_invalidPathId_returns400BadRequest() throws Exception {
                mockMvc.perform(get("/slib/hce/stations/abc"))
                                .andExpect(status().isBadRequest());

                verify(hceStationService, never()).getStationById(any());
        }

        // =========================================
        // === UTCID05: Unexpected service failure ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get station with unexpected service failure returns 404 Not Found")
        void getStationById_serviceFailure_returns404NotFound() throws Exception {
                when(hceStationService.getStationById(1))
                                .thenThrow(new RuntimeException("Loi he thong bat ngo"));

                mockMvc.perform(get("/slib/hce/stations/1"))
                                .andExpect(status().isNotFound());

                verify(hceStationService, times(1)).getStationById(1);
        }
}
