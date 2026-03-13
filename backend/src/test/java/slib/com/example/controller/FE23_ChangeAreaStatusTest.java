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
import slib.com.example.exception.BadRequestException;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.AreaService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-23: Change Area Status
 * Test Report: doc/Report/UnitTestReport/FE23_TestReport.md
 */
@WebMvcTest(value = AreaController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-23: Change Area Status - Unit Tests")
class FE23_ChangeAreaStatusTest {

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
         * UTCD01: Update area isActive with valid token and ADMIN role
         * Precondition: Authorized, Role = ADMIN, Area exists
         * Expected: 200 OK
         */
        @Test
        @DisplayName("UTCD01: Change area status with valid ADMIN token returns 200 OK")
        void changeAreaStatus_validAdminToken_returns200OK() throws Exception {
                AreaResponse response = new AreaResponse();
                when(areaService.updateAreaIsActive(eq(1L), any(AreaResponse.class))).thenReturn(response);

                mockMvc.perform(put("/slib/areas/1/active")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"isActive\":true}"))
                        .andExpect(status().isOk());

                verify(areaService, times(1)).updateAreaIsActive(eq(1L), any(AreaResponse.class));
        }

        // =========================================
        // === UTCD02: Service throws BadRequestException ===
        // =========================================

        /**
         * UTCD02: Change area status with service throwing BadRequestException
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCD02: Change area status with invalid request returns 400 Bad Request")
        void changeAreaStatus_badRequest_returns400() throws Exception {
                when(areaService.updateAreaIsActive(eq(1L), any(AreaResponse.class)))
                        .thenThrow(new BadRequestException("Trang thai khong hop le"));

                mockMvc.perform(put("/slib/areas/1/active")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"isActive\":true}"))
                        .andExpect(status().isBadRequest());
        }

        // =========================================
        // === UTCD03: Area not found ===
        // =========================================

        /**
         * UTCD03: Change area status for non-existent area
         * Precondition: Authorized, Role = ADMIN, Area not found
         * Expected: 404 Not Found
         */
        @Test
        @DisplayName("UTCD03: Change area status for non-existent area returns 404 Not Found")
        void changeAreaStatus_areaNotFound_returns404() throws Exception {
                when(areaService.updateAreaIsActive(eq(999L), any(AreaResponse.class)))
                        .thenThrow(new ResourceNotFoundException("Area not found with id: 999"));

                mockMvc.perform(put("/slib/areas/999/active")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"isActive\":true}"))
                        .andExpect(status().isNotFound());

                verify(areaService, times(1)).updateAreaIsActive(eq(999L), any(AreaResponse.class));
        }

        // =========================================
        // === UTCD04: Invalid ID format ===
        // =========================================

        /**
         * UTCD04: Change area status with invalid ID format
         * Expected: 400 Bad Request (type mismatch)
         */
        @Test
        @DisplayName("UTCD04: Change area status with invalid ID format returns 400 Bad Request")
        void changeAreaStatus_invalidIdFormat_returns400BadRequest() throws Exception {
                mockMvc.perform(put("/slib/areas/abc/active")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"isActive\":true}"))
                        .andExpect(status().isBadRequest());
        }

        // =========================================
        // === UTCD05: System error ===
        // =========================================

        /**
         * UTCD05: Change area status - system error
         * Expected: 500 Internal Server Error
         */
        @Test
        @DisplayName("UTCD05: Change area status with system error returns 500 Internal Server Error")
        void changeAreaStatus_systemError_returns500() throws Exception {
                when(areaService.updateAreaIsActive(eq(1L), any(AreaResponse.class)))
                        .thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(put("/slib/areas/1/active")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"isActive\":true}"))
                        .andExpect(status().isInternalServerError());
        }
}
