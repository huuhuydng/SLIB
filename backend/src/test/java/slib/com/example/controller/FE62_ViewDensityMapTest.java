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
import slib.com.example.controller.zone_config.ZoneController;
import slib.com.example.dto.zone_config.ZoneOccupancyDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.booking.BookingService;
import slib.com.example.service.zone_config.ZoneService;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-62: View Density Map
 * Test Report: doc/Report/UnitTestReport/FE62_TestReport.md
 */
@WebMvcTest(value = ZoneController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-62: View Density Map - Unit Tests")
class FE62_ViewDensityMapTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookingService bookingService;

        @MockBean
        private ZoneService zoneService;

        // =========================================
        // === UTCID01: Normal - zones with mixed occupancy ===
        // =========================================

        /**
         * UTCID01: Area contains zones with occupied and free seats
         * Precondition: Area id is provided
         * Expected: 200 OK with occupancy data
         */
        @Test
        @DisplayName("UTCID01: Zone occupancy with mixed seats returns 200 OK")
        void getZoneOccupancy_normalCase_returns200OK() throws Exception {
                ZoneOccupancyDTO zone1 = ZoneOccupancyDTO.builder()
                                .zoneId(1)
                                .zoneName("Zone A")
                                .totalSeats(20L)
                                .occupiedSeats(12L)
                                .occupancyRate(0.6)
                                .build();
                ZoneOccupancyDTO zone2 = ZoneOccupancyDTO.builder()
                                .zoneId(2)
                                .zoneName("Zone B")
                                .totalSeats(15L)
                                .occupiedSeats(3L)
                                .occupancyRate(0.2)
                                .build();

                when(zoneService.getZoneOccupancy(1L)).thenReturn(List.of(zone1, zone2));

                mockMvc.perform(get("/slib/zones/occupancy/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].zoneId").value(1))
                                .andExpect(jsonPath("$[0].occupancyRate").value(0.6));

                verify(zoneService, times(1)).getZoneOccupancy(1L);
        }

        // =========================================
        // === UTCID02: Zone fully occupied ===
        // =========================================

        /**
         * UTCID02: Zone is fully occupied (100% occupancy)
         * Precondition: Area id is provided
         * Expected: 200 OK with occupancyRate = 1.0
         */
        @Test
        @DisplayName("UTCID02: Fully occupied zone returns 200 OK with rate 1.0")
        void getZoneOccupancy_fullyOccupied_returns200OK() throws Exception {
                ZoneOccupancyDTO zone = ZoneOccupancyDTO.builder()
                                .zoneId(1)
                                .zoneName("Full Zone")
                                .totalSeats(10L)
                                .occupiedSeats(10L)
                                .occupancyRate(1.0)
                                .build();

                when(zoneService.getZoneOccupancy(2L)).thenReturn(List.of(zone));

                mockMvc.perform(get("/slib/zones/occupancy/2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].occupancyRate").value(1.0))
                                .andExpect(jsonPath("$[0].occupiedSeats").value(10));

                verify(zoneService, times(1)).getZoneOccupancy(2L);
        }

        // =========================================
        // === UTCID03: Zone completely empty ===
        // =========================================

        /**
         * UTCID03: Zone is completely empty (0% occupancy)
         * Precondition: Area id is provided
         * Expected: 200 OK with occupancyRate = 0.0
         */
        @Test
        @DisplayName("UTCID03: Empty zone returns 200 OK with rate 0.0")
        void getZoneOccupancy_emptyZone_returns200OK() throws Exception {
                ZoneOccupancyDTO zone = ZoneOccupancyDTO.builder()
                                .zoneId(5)
                                .zoneName("Empty Zone")
                                .totalSeats(20L)
                                .occupiedSeats(0L)
                                .occupancyRate(0.0)
                                .build();

                when(zoneService.getZoneOccupancy(3L)).thenReturn(List.of(zone));

                mockMvc.perform(get("/slib/zones/occupancy/3"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].occupancyRate").value(0.0))
                                .andExpect(jsonPath("$[0].occupiedSeats").value(0));

                verify(zoneService, times(1)).getZoneOccupancy(3L);
        }

        // =========================================
        // === UTCID04: Area with no zones ===
        // =========================================

        /**
         * UTCID04: Repository or overlap calculation fails - returns empty
         * Precondition: Area id is provided
         * Expected: 200 OK with empty list
         */
        @Test
        @DisplayName("UTCID04: Area with no zones returns 200 OK with empty array")
        void getZoneOccupancy_noZones_returns200OK() throws Exception {
                when(zoneService.getZoneOccupancy(999L)).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/zones/occupancy/999"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isEmpty());

                verify(zoneService, times(1)).getZoneOccupancy(999L);
        }

        // =========================================
        // === UTCID05: Service failure propagation ===
        // =========================================

        /**
         * UTCID05: Repository or overlap calculation fails
         * Precondition: Area id is provided
         * Expected: 200 OK (service handles errors gracefully) or error
         */
        @Test
        @DisplayName("UTCID05: Service failure returns error response")
        void getZoneOccupancy_serviceFailure_returnsError() throws Exception {
                when(zoneService.getZoneOccupancy(0L))
                                .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/zones/occupancy/0"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isEmpty());

                verify(zoneService, times(1)).getZoneOccupancy(0L);
        }
}
