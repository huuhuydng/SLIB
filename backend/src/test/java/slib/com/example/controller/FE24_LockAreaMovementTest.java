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
import slib.com.example.controller.zone_config.AreaController;
import slib.com.example.dto.zone_config.AreaResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.AreaService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-24: Lock Area Movement
 * Test Report: doc/Report/UnitTestReport/FE24_TestReport.md
 */
@WebMvcTest(value = AreaController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-24: Lock Area Movement - Unit Tests")
class FE24_LockAreaMovementTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AreaService areaService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCD01: Valid request - Success ===
        // =========================================

        /**
         * UTCD01: Lock area movement with valid token and ADMIN role
         * Precondition: Authorized, Role = ADMIN, Area exists
         * Expected: 200 OK
         */
        @Test
        @DisplayName("UTCD01: Lock area movement with valid ADMIN token returns 200 OK")
        void lockAreaMovement_validAdminToken_returns200OK() throws Exception {
                AreaResponse response = new AreaResponse();
                when(areaService.updateAreaLocked(eq(1L), any(AreaResponse.class))).thenReturn(response);

                mockMvc.perform(put("/slib/areas/1/locked")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"locked\":true}"))
                        .andExpect(status().isOk());

                verify(areaService, times(1)).updateAreaLocked(eq(1L), any(AreaResponse.class));
        }

        // =========================================
        // === UTCD02: Invalid ID format ===
        // =========================================

        /**
         * UTCD02: Lock area movement with invalid ID format
         * Expected: 400 Bad Request (type mismatch)
         */
        @Test
        @DisplayName("UTCD02: Lock area movement with invalid ID format returns 400 Bad Request")
        void lockAreaMovement_invalidIdFormat_returns400() throws Exception {
                mockMvc.perform(put("/slib/areas/abc/locked")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"locked\":true}"))
                        .andExpect(status().isBadRequest());
        }

        // =========================================
        // === UTCD03: Area not found ===
        // =========================================

        /**
         * UTCD03: Lock area movement for non-existent area
         * Precondition: Authorized, Role = ADMIN, Area not found
         * Expected: 404 Not Found
         */
        @Test
        @DisplayName("UTCD03: Lock area movement for non-existent area returns 404 Not Found")
        void lockAreaMovement_areaNotFound_returns404() throws Exception {
                when(areaService.updateAreaLocked(eq(999L), any(AreaResponse.class)))
                        .thenThrow(new ResourceNotFoundException("Area not found with id: 999"));

                mockMvc.perform(put("/slib/areas/999/locked")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"locked\":true}"))
                        .andExpect(status().isNotFound());

                verify(areaService, times(1)).updateAreaLocked(eq(999L), any(AreaResponse.class));
        }

        // =========================================
        // === UTCD04: System error ===
        // =========================================

        /**
         * UTCD04: Lock area movement - system error
         * Expected: 500 Internal Server Error
         */
        @Test
        @DisplayName("UTCD04: Lock area movement with system error returns 500 Internal Server Error")
        void lockAreaMovement_systemError_returns500() throws Exception {
                when(areaService.updateAreaLocked(eq(1L), any(AreaResponse.class)))
                        .thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(put("/slib/areas/1/locked")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"locked\":true}"))
                        .andExpect(status().isInternalServerError());
        }
}
