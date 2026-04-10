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
import slib.com.example.service.zone_config.AreaService;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-23: CRUD area
 * Test Report: doc/Report/UnitTestReport/FE22_TestReport.md
 */
@WebMvcTest(value = AreaController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-23: CRUD area - Unit Tests")
class FE23_CRUDAreaTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AreaService areaService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === GET ALL AREAS ===
        // =========================================

        /**
         * UTCD01: Get all areas with valid token and ADMIN role
         * Expected: 200 OK
         */
        @Test
        @DisplayName("UTCD01: Get all areas with valid ADMIN token returns 200 OK")
        void getAllAreas_validAdminToken_returns200OK() throws Exception {
                when(areaService.getAllAreas()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/areas"))
                        .andExpect(status().isOk());

                verify(areaService, times(1)).getAllAreas();
        }

        /**
         * UTCD02: Get all areas - service throws RuntimeException
         * Expected: 500 Internal Server Error
         */
        @Test
        @DisplayName("UTCD02: Get all areas with service error returns 500 Internal Server Error")
        void getAllAreas_serviceError_returns500() throws Exception {
                when(areaService.getAllAreas()).thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(get("/slib/areas"))
                        .andExpect(status().isInternalServerError());
        }

        /**
         * UTCD03: Get area by ID - not found
         * Expected: 404 Not Found
         */
        @Test
        @DisplayName("UTCD03: Get area by non-existent ID returns 404 Not Found")
        void getAreaById_notFound_returns404() throws Exception {
                when(areaService.getAreaById(999L))
                        .thenThrow(new ResourceNotFoundException("Area not found with id: 999"));

                mockMvc.perform(get("/slib/areas/999"))
                        .andExpect(status().isNotFound());

                verify(areaService, times(1)).getAreaById(999L);
        }

        // =========================================
        // === CREATE AREA ===
        // =========================================

        /**
         * UTCD04: Create area with valid data
         * Expected: 201 Created
         */
        @Test
        @DisplayName("UTCD04: Create area with valid data returns 201 Created")
        void createArea_validData_returns201Created() throws Exception {
                AreaResponse response = new AreaResponse();
                when(areaService.createArea(any(AreaResponse.class))).thenReturn(response);

                mockMvc.perform(post("/slib/areas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"areaName\":\"Khu A\",\"width\":100,\"height\":100,\"positionX\":0,\"positionY\":0}"))
                        .andExpect(status().isCreated());

                verify(areaService, times(1)).createArea(any(AreaResponse.class));
        }

        /**
         * UTCD05: Create area with empty name
         * Expected: 400 Bad Request (validation error from @Valid @NotBlank)
         */
        @Test
        @DisplayName("UTCD05: Create area with empty name returns 400 Bad Request")
        void createArea_emptyName_returns400BadRequest() throws Exception {
                mockMvc.perform(post("/slib/areas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"areaName\":\"\",\"width\":100,\"height\":100}"))
                        .andExpect(status().isBadRequest());
        }

        /**
         * UTCD06: Create area with duplicate name
         * Expected: 500 Internal Server Error (IllegalStateException handled by GlobalExceptionHandler)
         */
        @Test
        @DisplayName("UTCD06: Create area with duplicate name returns 500 Internal Server Error")
        void createArea_duplicateName_returns500() throws Exception {
                when(areaService.createArea(any(AreaResponse.class)))
                        .thenThrow(new IllegalStateException("Ten khu vuc da ton tai"));

                mockMvc.perform(post("/slib/areas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"areaName\":\"Existing Area\",\"width\":100,\"height\":100,\"positionX\":0,\"positionY\":0}"))
                        .andExpect(status().isInternalServerError());
        }

        /**
         * UTCD07: Create area with missing required fields (width, height)
         * Expected: 400 Bad Request (validation)
         */
        @Test
        @DisplayName("UTCD07: Create area with missing required fields returns 400 Bad Request")
        void createArea_missingRequiredFields_returns400BadRequest() throws Exception {
                mockMvc.perform(post("/slib/areas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"areaName\":\"Test Area\"}"))
                        .andExpect(status().isBadRequest());
        }

        /**
         * UTCD08: Create area with invalid JSON body
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCD08: Create area with invalid JSON returns 400 Bad Request")
        void createArea_invalidJson_returns400BadRequest() throws Exception {
                mockMvc.perform(post("/slib/areas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{invalid json}"))
                        .andExpect(status().isBadRequest());
        }

        // =========================================
        // === UPDATE AREA ===
        // =========================================

        /**
         * UTCD09: Update area with valid data
         * Expected: 200 OK
         */
        @Test
        @DisplayName("UTCD09: Update area with valid data returns 200 OK")
        void updateArea_validData_returns200OK() throws Exception {
                AreaResponse response = new AreaResponse();
                when(areaService.updateAreaFull(eq(1L), any(AreaResponse.class))).thenReturn(response);

                mockMvc.perform(put("/slib/areas/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"areaName\":\"Updated Area\",\"width\":200,\"height\":150,\"positionX\":10,\"positionY\":20}"))
                        .andExpect(status().isOk());

                verify(areaService, times(1)).updateAreaFull(eq(1L), any(AreaResponse.class));
        }

        /**
         * UTCD10: Update area with empty name
         * Expected: 400 Bad Request (validation error from @Valid @NotBlank)
         */
        @Test
        @DisplayName("UTCD10: Update area with empty name returns 400 Bad Request")
        void updateArea_emptyName_returns400BadRequest() throws Exception {
                mockMvc.perform(put("/slib/areas/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"areaName\":\"\",\"width\":100,\"height\":100}"))
                        .andExpect(status().isBadRequest());
        }

        /**
         * UTCD11: Update area with duplicate name
         * Expected: 500 Internal Server Error (IllegalStateException)
         */
        @Test
        @DisplayName("UTCD11: Update area with duplicate name returns 500 Internal Server Error")
        void updateArea_duplicateName_returns500() throws Exception {
                when(areaService.updateAreaFull(eq(1L), any(AreaResponse.class)))
                        .thenThrow(new IllegalStateException("Ten khu vuc da ton tai"));

                mockMvc.perform(put("/slib/areas/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"areaName\":\"Existing Area\",\"width\":100,\"height\":100,\"positionX\":0,\"positionY\":0}"))
                        .andExpect(status().isInternalServerError());
        }

        /**
         * UTCD12: Update area with missing required fields
         * Expected: 400 Bad Request (validation)
         */
        @Test
        @DisplayName("UTCD12: Update area with missing required fields returns 400 Bad Request")
        void updateArea_missingRequiredFields_returns400BadRequest() throws Exception {
                mockMvc.perform(put("/slib/areas/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"areaName\":\"Updated Area\"}"))
                        .andExpect(status().isBadRequest());
        }

        /**
         * UTCD13: Update area with invalid ID format
         * Expected: 400 Bad Request (type mismatch)
         */
        @Test
        @DisplayName("UTCD13: Update area with invalid ID format returns 400 Bad Request")
        void updateArea_invalidIdFormat_returns400BadRequest() throws Exception {
                mockMvc.perform(put("/slib/areas/abc")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"areaName\":\"Updated Area\",\"width\":100,\"height\":100}"))
                        .andExpect(status().isBadRequest());
        }

        /**
         * UTCD14: Update area - system error
         * Expected: 500 Internal Server Error
         */
        @Test
        @DisplayName("UTCD14: Update area with system error returns 500 Internal Server Error")
        void updateArea_systemError_returns500() throws Exception {
                when(areaService.updateAreaFull(eq(1L), any(AreaResponse.class)))
                        .thenThrow(new RuntimeException("Database connection failed"));

                mockMvc.perform(put("/slib/areas/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"areaName\":\"Test Area\",\"width\":100,\"height\":100,\"positionX\":0,\"positionY\":0}"))
                        .andExpect(status().isInternalServerError());
        }

        // =========================================
        // === DELETE AREA ===
        // =========================================

        /**
         * UTCD15: Delete area with valid ID
         * Expected: 204 No Content
         */
        @Test
        @DisplayName("UTCD15: Delete area with valid ID returns 204 No Content")
        void deleteArea_validId_returns204NoContent() throws Exception {
                doNothing().when(areaService).deleteArea(1L);

                mockMvc.perform(delete("/slib/areas/1"))
                        .andExpect(status().isNoContent());

                verify(areaService, times(1)).deleteArea(1L);
        }

        /**
         * UTCD16: Delete area with invalid ID format
         * Expected: 400 Bad Request (type mismatch)
         */
        @Test
        @DisplayName("UTCD16: Delete area with invalid ID format returns 400 Bad Request")
        void deleteArea_invalidIdFormat_returns400BadRequest() throws Exception {
                mockMvc.perform(delete("/slib/areas/abc"))
                        .andExpect(status().isBadRequest());
        }

        /**
         * UTCD17: Delete area - service throws BadRequestException
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCD17: Delete area with active zones returns 400 Bad Request")
        void deleteArea_activeZones_returns400BadRequest() throws Exception {
                doThrow(new BadRequestException("Khong the xoa khu vuc co zone dang hoat dong"))
                        .when(areaService).deleteArea(1L);

                mockMvc.perform(delete("/slib/areas/1"))
                        .andExpect(status().isBadRequest());

                verify(areaService, times(1)).deleteArea(1L);
        }

        /**
         * UTCD18: Delete area with non-existent ID
         * Expected: 404 Not Found
         */
        @Test
        @DisplayName("UTCD18: Delete area with non-existent ID returns 404 Not Found")
        void deleteArea_notFound_returns404() throws Exception {
                doThrow(new ResourceNotFoundException("Area not found with id: 999"))
                        .when(areaService).deleteArea(999L);

                mockMvc.perform(delete("/slib/areas/999"))
                        .andExpect(status().isNotFound());

                verify(areaService, times(1)).deleteArea(999L);
        }
}
