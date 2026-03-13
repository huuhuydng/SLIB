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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.BookingService;
import slib.com.example.service.ZoneService;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.zone_config.ZoneController;

/**
 * Unit Tests for FE-25: View Zone Map
 * Test Report: doc/Report/FE25_TestReport.md
 */
@WebMvcTest(value = ZoneController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-25: View Zone Map - Unit Tests")
class FE25_ViewZoneMapTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ZoneService zoneService;

        @MockBean
        private BookingService bookingService;

        @Test
        @DisplayName("UTCD01: View zone map returns 200 OK")
        void viewZoneMap_validToken_returns200OK() throws Exception {
                when(zoneService.getAllZones()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/zones"))
                        .andExpect(status().isOk());

                verify(zoneService, times(1)).getAllZones();
        }

        @Test
        @DisplayName("UTCD02: View zone map with non-existent zone ID returns 404 Not Found")
        void viewZoneMap_nonExistentId_returns404() throws Exception {
                when(zoneService.getZoneById(999))
                        .thenThrow(new ResourceNotFoundException("Zone not found with id: 999"));

                mockMvc.perform(get("/slib/zones/999"))
                        .andExpect(status().isNotFound());

                verify(zoneService, times(1)).getZoneById(999);
        }
}
