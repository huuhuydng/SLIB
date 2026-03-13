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
import slib.com.example.dto.zone_config.ZoneResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.BookingService;
import slib.com.example.service.ZoneService;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.zone_config.ZoneController;

/**
 * Unit Tests for FE-26: CRUD Zone
 * Test Report: doc/Report/FE26_TestReport.md
 */
@WebMvcTest(value = ZoneController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-26: CRUD Zone - Unit Tests")
class FE26_CRUDZoneTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ZoneService zoneService;

        @MockBean
        private BookingService bookingService;

        // UTCD01: Create zone - Success
        @Test
        @DisplayName("UTCD01: Create zone with valid data returns 200 OK")
        void createZone_validData_returns200OK() throws Exception {
                ZoneResponse response = new ZoneResponse();
                when(zoneService.createZone(any(ZoneResponse.class))).thenReturn(response);

                mockMvc.perform(post("/slib/zones")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Zone A\",\"areaId\":1}"))
                        .andExpect(status().isOk());

                verify(zoneService, times(1)).createZone(any(ZoneResponse.class));
        }

        // UTCD02: Get all zones - Success
        @Test
        @DisplayName("UTCD02: Get all zones returns 200 OK")
        void getAllZones_returns200OK() throws Exception {
                when(zoneService.getAllZones()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/zones"))
                        .andExpect(status().isOk());

                verify(zoneService, times(1)).getAllZones();
        }

        // UTCD03: Get zone by non-existent ID - 404
        @Test
        @DisplayName("UTCD03: Get zone with non-existent ID returns 404 Not Found")
        void getZone_notFound_returns404() throws Exception {
                when(zoneService.getZoneById(999))
                        .thenThrow(new ResourceNotFoundException("Zone not found with id: 999"));

                mockMvc.perform(get("/slib/zones/999"))
                        .andExpect(status().isNotFound());

                verify(zoneService, times(1)).getZoneById(999);
        }
}
