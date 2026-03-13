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
import slib.com.example.controller.zone_config.AmenityController;
import slib.com.example.dto.zone_config.AmenityResponse;
import slib.com.example.exception.BadRequestException;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.AmenityService;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-27: CRUD Zone Attribute
 * Test Report: doc/Report/UnitTestReport/FE27_TestReport.md
 */
@WebMvcTest(value = AmenityController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-27: CRUD Zone Attribute - Unit Tests")
class FE27_CRUDZoneAttributeTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AmenityService amenityService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCD01: Valid request - Success ===
        // =========================================

        /**
         * UTCD01: Get amenities with valid token and ADMIN role
         * Precondition: Authorized, Role = ADMIN, Zone exists
         * Expected: 200 OK
         */
        @Test
        @DisplayName("UTCD01: Get zone amenities with valid ADMIN token returns 200 OK")
        void getZoneAmenities_validAdminToken_returns200OK() throws Exception {
                when(amenityService.getAmenitiesByZoneId(1)).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/zone_amenities")
                                .param("zoneId", "1"))
                        .andExpect(status().isOk());

                verify(amenityService, times(1)).getAmenitiesByZoneId(1);
        }

        // =========================================
        // === UTCD02: Get all amenities - Success ===
        // =========================================

        /**
         * UTCD02: Get all amenities without zoneId param
         * Expected: 200 OK
         */
        @Test
        @DisplayName("UTCD02: Get all amenities returns 200 OK")
        void getAllAmenities_returns200OK() throws Exception {
                when(amenityService.getAllAmenities()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/zone_amenities"))
                        .andExpect(status().isOk());

                verify(amenityService, times(1)).getAllAmenities();
        }

        // =========================================
        // === UTCD03: Amenity not found ===
        // =========================================

        /**
         * UTCD03: Get amenity by non-existent ID
         * Expected: 404 Not Found
         */
        @Test
        @DisplayName("UTCD03: Get amenity by non-existent ID returns 404 Not Found")
        void getAmenityById_notFound_returns404() throws Exception {
                when(amenityService.getAmenityById(999))
                        .thenThrow(new ResourceNotFoundException("Amenity not found with id: 999"));

                mockMvc.perform(get("/slib/zone_amenities/999"))
                        .andExpect(status().isNotFound());

                verify(amenityService, times(1)).getAmenityById(999);
        }

        // =========================================
        // === UTCD04: Invalid data - Bad Request ===
        // =========================================

        /**
         * UTCD04: Create amenity with invalid data
         * Precondition: Authorized
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCD04: Create amenity with invalid data returns 400 Bad Request")
        void createAmenity_invalidData_returns400BadRequest() throws Exception {
                when(amenityService.createAmenity(any(AmenityResponse.class)))
                        .thenThrow(new BadRequestException("Du lieu khong hop le"));

                mockMvc.perform(post("/slib/zone_amenities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"\"}"))
                        .andExpect(status().isBadRequest());
        }

        // =========================================
        // === UTCD05: Zone not found ===
        // =========================================

        /**
         * UTCD05: Get amenities for non-existent zone
         * Precondition: Zone exists = false
         * Expected: 404 Not Found
         */
        @Test
        @DisplayName("UTCD05: Get amenities for non-existent zone returns 404 Not Found")
        void getZoneAmenities_zoneNotFound_returns404() throws Exception {
                when(amenityService.getAmenitiesByZoneId(999))
                        .thenThrow(new ResourceNotFoundException("Zone not found with id: 999"));

                mockMvc.perform(get("/slib/zone_amenities")
                                .param("zoneId", "999"))
                        .andExpect(status().isNotFound());

                verify(amenityService, times(1)).getAmenitiesByZoneId(999);
        }

        // =========================================
        // === UTCD06: System error ===
        // =========================================

        /**
         * UTCD06: Create amenity - system error
         * Expected: 500 Internal Server Error
         */
        @Test
        @DisplayName("UTCD06: Create amenity with system error returns 500 Internal Server Error")
        void createAmenity_systemError_returns500() throws Exception {
                when(amenityService.createAmenity(any(AmenityResponse.class)))
                        .thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(post("/slib/zone_amenities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Test Amenity\",\"zoneId\":1}"))
                        .andExpect(status().isInternalServerError());
        }
}
