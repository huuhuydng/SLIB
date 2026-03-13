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
import slib.com.example.service.HceStationService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-40: View HCE Scan Stations
 * Test Report: doc/Report/UnitTestReport/FE40_TestReport.md
 */
@WebMvcTest(value = HceStationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-40: View HCE Scan Stations - Unit Tests")
class FE40_ViewHCEScanStationsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private HceStationService hceStationService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Get all stations without filters - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Get all stations without filters returns 200 OK")
        void getAllStations_noFilters_returns200OK() throws Exception {
                HceStationResponse station = HceStationResponse.builder()
                                .id(1)
                                .deviceId("RPI-001")
                                .deviceName("Cong vao 1")
                                .deviceType("ENTRY_GATE")
                                .status("ACTIVE")
                                .online(true)
                                .lastHeartbeat(LocalDateTime.now())
                                .build();

                when(hceStationService.getAllStations(isNull(), isNull(), isNull()))
                                .thenReturn(List.of(station));

                mockMvc.perform(get("/slib/hce/stations"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].deviceId").value("RPI-001"));

                verify(hceStationService, times(1)).getAllStations(isNull(), isNull(), isNull());
        }

        // =========================================
        // === UTCID02: Get stations with filters - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get stations with valid filters returns 200 OK")
        void getAllStations_withFilters_returns200OK() throws Exception {
                HceStationResponse station = HceStationResponse.builder()
                                .id(2)
                                .deviceId("RPI-002")
                                .deviceName("Cong ra 1")
                                .deviceType("EXIT_GATE")
                                .status("ACTIVE")
                                .build();

                when(hceStationService.getAllStations(eq("RPI"), eq("ACTIVE"), eq("EXIT_GATE")))
                                .thenReturn(List.of(station));

                mockMvc.perform(get("/slib/hce/stations")
                                .param("search", "RPI")
                                .param("status", "ACTIVE")
                                .param("deviceType", "EXIT_GATE"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].deviceType").value("EXIT_GATE"));

                verify(hceStationService, times(1)).getAllStations("RPI", "ACTIVE", "EXIT_GATE");
        }

        // =========================================
        // === UTCID03: Invalid filter values ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get stations with invalid filter values returns 400 Bad Request")
        void getAllStations_invalidFilters_returns400BadRequest() throws Exception {
                when(hceStationService.getAllStations(any(), eq("INVALID_STATUS"), any()))
                                .thenThrow(new RuntimeException("Trang thai khong hop le"));

                mockMvc.perform(get("/slib/hce/stations")
                                .param("status", "INVALID_STATUS"))
                                .andExpect(status().isBadRequest());

                verify(hceStationService, times(1)).getAllStations(any(), eq("INVALID_STATUS"), any());
        }

        // =========================================
        // === UTCID04: No matching stations (empty list) ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Get stations with no matching results returns 200 OK with empty list")
        void getAllStations_noMatching_returns200OKEmpty() throws Exception {
                when(hceStationService.getAllStations(eq("NONEXISTENT"), isNull(), isNull()))
                                .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/hce/stations")
                                .param("search", "NONEXISTENT"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$").isEmpty());

                verify(hceStationService, times(1)).getAllStations("NONEXISTENT", null, null);
        }

        // =========================================
        // === UTCID05: Runtime service failure ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get stations with service failure returns 400 Bad Request")
        void getAllStations_serviceFailure_returns400BadRequest() throws Exception {
                when(hceStationService.getAllStations(any(), any(), any()))
                                .thenThrow(new RuntimeException("Loi he thong"));

                mockMvc.perform(get("/slib/hce/stations"))
                                .andExpect(status().isBadRequest());

                verify(hceStationService, times(1)).getAllStations(any(), any(), any());
        }
}
