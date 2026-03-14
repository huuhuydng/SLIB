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
import slib.com.example.dto.zone_config.ZoneResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.booking.BookingService;
import slib.com.example.service.zone_config.ZoneService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-28: View Zone Details
 * Test Report: doc/Report/UnitTestReport/FE28_TestReport.md
 */
@WebMvcTest(value = ZoneController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-28: View Zone Details - Unit Tests")
class FE28_ViewZoneDetailsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ZoneService zoneService;

        @MockBean
        private BookingService bookingService;

        // =========================================
        // === UTCD01: Valid request - Success ===
        // =========================================

        /**
         * UTCD01: View zone details with valid token
         * Precondition: Authorized, Zone exists
         * Expected: 200 OK
         */
        @Test
        @DisplayName("UTCD01: View zone details with valid token returns 200 OK")
        void viewZoneDetails_validToken_returns200OK() throws Exception {
                ZoneResponse response = new ZoneResponse();
                when(zoneService.getZoneById(1)).thenReturn(response);

                mockMvc.perform(get("/slib/zones/1"))
                        .andExpect(status().isOk());

                verify(zoneService, times(1)).getZoneById(1);
        }

        // =========================================
        // === UTCD02: System error ===
        // =========================================

        /**
         * UTCD02: View zone details - service error
         * Expected: 500 Internal Server Error
         */
        @Test
        @DisplayName("UTCD02: View zone details with service error returns 500 Internal Server Error")
        void viewZoneDetails_serviceError_returns500() throws Exception {
                when(zoneService.getZoneById(1))
                        .thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(get("/slib/zones/1"))
                        .andExpect(status().isInternalServerError());
        }

        // =========================================
        // === UTCD03: Zone not found ===
        // =========================================

        /**
         * UTCD03: View zone details for non-existent zone
         * Precondition: Authorized, Zone not found
         * Expected: 404 Not Found
         */
        @Test
        @DisplayName("UTCD03: View zone details for non-existent zone returns 404 Not Found")
        void viewZoneDetails_zoneNotFound_returns404() throws Exception {
                when(zoneService.getZoneById(999))
                        .thenThrow(new ResourceNotFoundException("Zone not found with id: 999"));

                mockMvc.perform(get("/slib/zones/999"))
                        .andExpect(status().isNotFound());

                verify(zoneService, times(1)).getZoneById(999);
        }

        // =========================================
        // === UTCD04: Invalid zone ID format ===
        // =========================================

        /**
         * UTCD04: View zone details with invalid zone ID format
         * Precondition: Authorized, Invalid zone ID format
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCD04: View zone details with invalid zone ID format returns 400 Bad Request")
        void viewZoneDetails_invalidIdFormat_returns400BadRequest() throws Exception {
                mockMvc.perform(get("/slib/zones/abc"))
                        .andExpect(status().isBadRequest());
        }
}
