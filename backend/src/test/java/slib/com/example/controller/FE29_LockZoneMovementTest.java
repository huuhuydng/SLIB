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
import slib.com.example.controller.zone_config.ZoneController;
import slib.com.example.dto.zone_config.ZoneResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.BookingService;
import slib.com.example.service.ZoneService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-29: Lock Zone Movement
 * Test Report: doc/Report/UnitTestReport/FE29_TestReport.md
 */
@WebMvcTest(value = ZoneController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-29: Lock Zone Movement - Unit Tests")
class FE29_LockZoneMovementTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ZoneService zoneService;

        @MockBean
        private BookingService bookingService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCD01: Valid request - Success ===
        // =========================================

        /**
         * UTCD01: Lock zone movement with valid token and ADMIN role
         * Precondition: Authorized, Role = ADMIN, Zone exists
         * Expected: 200 OK
         */
        @Test
        @DisplayName("UTCD01: Lock zone movement with valid ADMIN token returns 200 OK")
        void lockZoneMovement_validAdminToken_returns200OK() throws Exception {
                ZoneResponse response = new ZoneResponse();
                when(zoneService.updateZoneFull(eq(1), any(ZoneResponse.class))).thenReturn(response);

                mockMvc.perform(put("/slib/zones/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"locked\":true}"))
                        .andExpect(status().isOk());

                verify(zoneService, times(1)).updateZoneFull(eq(1), any(ZoneResponse.class));
        }

        // =========================================
        // === UTCD02: Invalid ID format ===
        // =========================================

        /**
         * UTCD02: Lock zone movement with invalid ID format
         * Expected: 400 Bad Request (type mismatch)
         */
        @Test
        @DisplayName("UTCD02: Lock zone movement with invalid ID format returns 400 Bad Request")
        void lockZoneMovement_invalidIdFormat_returns400() throws Exception {
                mockMvc.perform(put("/slib/zones/abc")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"locked\":true}"))
                        .andExpect(status().isBadRequest());
        }

        // =========================================
        // === UTCD03: Zone not found ===
        // =========================================

        /**
         * UTCD03: Lock zone movement for non-existent zone
         * Precondition: Authorized, Zone not found
         * Expected: 404 Not Found
         */
        @Test
        @DisplayName("UTCD03: Lock zone movement for non-existent zone returns 404 Not Found")
        void lockZoneMovement_zoneNotFound_returns404() throws Exception {
                when(zoneService.updateZoneFull(eq(999), any(ZoneResponse.class)))
                        .thenThrow(new ResourceNotFoundException("Zone not found with id: 999"));

                mockMvc.perform(put("/slib/zones/999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"locked\":true}"))
                        .andExpect(status().isNotFound());

                verify(zoneService, times(1)).updateZoneFull(eq(999), any(ZoneResponse.class));
        }

        // =========================================
        // === UTCD04: Zone already locked - system error ===
        // =========================================

        /**
         * UTCD04: Lock zone movement for already locked zone
         * Precondition: Zone exists, Zone already locked
         * Expected: 500 Internal Server Error (IllegalStateException -> RuntimeException handler)
         */
        @Test
        @DisplayName("UTCD04: Lock zone movement for already locked zone returns 500 Internal Server Error")
        void lockZoneMovement_alreadyLocked_returns500() throws Exception {
                when(zoneService.updateZoneFull(eq(1), any(ZoneResponse.class)))
                        .thenThrow(new IllegalStateException("Zone da bi khoa di chuyen"));

                mockMvc.perform(put("/slib/zones/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"locked\":true}"))
                        .andExpect(status().isInternalServerError());
        }

        // =========================================
        // === UTCD05: Service throws AccessDeniedException ===
        // =========================================

        /**
         * UTCD05: Lock zone movement - service throws AccessDeniedException
         * Expected: 403 Forbidden
         */
        @Test
        @DisplayName("UTCD05: Lock zone movement with access denied returns 403 Forbidden")
        void lockZoneMovement_accessDenied_returns403Forbidden() throws Exception {
                when(zoneService.updateZoneFull(eq(1), any(ZoneResponse.class)))
                        .thenThrow(new org.springframework.security.access.AccessDeniedException("Khong co quyen truy cap"));

                mockMvc.perform(put("/slib/zones/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"locked\":true}"))
                        .andExpect(status().isForbidden());
        }
}
