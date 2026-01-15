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
import slib.com.example.service.AreaService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for AreaController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 * Test Type: @WebMvcTest (Unit Tests only - no full context)
 */
@WebMvcTest(value = AreaController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AreaController Unit Tests")
class AreaControllerUnitTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AreaService areaService;

        @Autowired
        private ObjectMapper objectMapper;

        // ===================================
        // === GET ALL AREAS ENDPOINT ===
        // ===================================

        @Test
        @DisplayName("getAllAreas_success_returns200WithAreasList")
        void getAllAreas_success_returns200WithAreasList() throws Exception {
                // Arrange
                AreaResponse area1 = createAreaResponse(1L, "Study Area A", 800, 600, 100, 100, true, false);
                AreaResponse area2 = createAreaResponse(2L, "Reading Zone B", 600, 400, 200, 200, true, false);
                List<AreaResponse> areas = Arrays.asList(area1, area2);

                when(areaService.getAllAreas()).thenReturn(areas);

                // Act & Assert
                mockMvc.perform(get("/slib/areas")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].areaId").value(1))
                                .andExpect(jsonPath("$[0].areaName").value("Study Area A"))
                                .andExpect(jsonPath("$[1].areaId").value(2))
                                .andExpect(jsonPath("$[1].areaName").value("Reading Zone B"));

                verify(areaService, times(1)).getAllAreas();
        }

        @Test
        @DisplayName("getAllAreas_emptyList_returns200WithEmptyArray")
        void getAllAreas_emptyList_returns200WithEmptyArray() throws Exception {
                // Arrange
                when(areaService.getAllAreas()).thenReturn(List.of());

                // Act & Assert
                mockMvc.perform(get("/slib/areas")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(areaService, times(1)).getAllAreas();
        }

        @Test
        @DisplayName("getAllAreas_serviceThrowsException_returns500")
        void getAllAreas_serviceThrowsException_returns500() throws Exception {
                // Arrange
                when(areaService.getAllAreas())
                                .thenThrow(new RuntimeException("Database connection error"));

                // Act & Assert
                mockMvc.perform(get("/slib/areas")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isInternalServerError());

                verify(areaService, times(1)).getAllAreas();
        }

        // =======================================
        // === GET AREA BY ID ENDPOINT ===
        // =======================================

        @Test
        @DisplayName("getAreaById_validId_returns200WithArea")
        void getAreaById_validId_returns200WithArea() throws Exception {
                // Arrange
                Long areaId = 5L;
                AreaResponse area = createAreaResponse(areaId, "Computer Lab", 1000, 800, 50, 50, true, false);

                when(areaService.getAreaById(areaId)).thenReturn(area);

                // Act & Assert
                mockMvc.perform(get("/slib/areas/{id}", areaId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.areaId").value(areaId))
                                .andExpect(jsonPath("$.areaName").value("Computer Lab"))
                                .andExpect(jsonPath("$.width").value(1000))
                                .andExpect(jsonPath("$.height").value(800));

                verify(areaService, times(1)).getAreaById(areaId);
        }

        @Test
        @DisplayName("getAreaById_notFound_throwsRuntimeException")
        void getAreaById_notFound_throwsRuntimeException() throws Exception {
                // Arrange
                Long areaId = 999L;
                when(areaService.getAreaById(areaId))
                                .thenThrow(new RuntimeException("Area not found with id: " + areaId));

                // Act & Assert
                mockMvc.perform(get("/slib/areas/{id}", areaId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isInternalServerError());

                verify(areaService, times(1)).getAreaById(areaId);
        }

        @Test
        @DisplayName("getAreaById_invalidIdType_returns400")
        void getAreaById_invalidIdType_returns400() throws Exception {
                // Act & Assert
                mockMvc.perform(get("/slib/areas/{id}", "invalid")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());

                verify(areaService, never()).getAreaById(any());
        }

        // ====================================
        // === CREATE AREA ENDPOINT ===
        // ====================================

        @Test
        @DisplayName("createArea_validData_returns201WithCreatedArea")
        void createArea_validData_returns201WithCreatedArea() throws Exception {
                // Arrange
                AreaResponse request = createAreaResponse(null, "New Study Hall", 800, 600, 0, 0, true, false);
                AreaResponse response = createAreaResponse(1L, "New Study Hall", 800, 600, 0, 0, true, false);

                when(areaService.createArea(org.mockito.ArgumentMatchers.<AreaResponse>any())).thenReturn(response);

                // Act & Assert
                mockMvc.perform(post("/slib/areas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.areaId").value(1))
                                .andExpect(jsonPath("$.areaName").value("New Study Hall"))
                                .andExpect(jsonPath("$.width").value(800))
                                .andExpect(jsonPath("$.height").value(600));

                verify(areaService, times(1)).createArea(org.mockito.ArgumentMatchers.<AreaResponse>any());
        }

        @Test
        @DisplayName("createArea_emptyRequestBody_returns400")
        void createArea_emptyRequestBody_returns400() throws Exception {
                // Act & Assert
                mockMvc.perform(post("/slib/areas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(""))
                                .andExpect(status().isBadRequest());

                verify(areaService, never()).createArea(org.mockito.ArgumentMatchers.<AreaResponse>any());
        }

        @Test
        @DisplayName("createArea_invalidJson_returns400")
        void createArea_invalidJson_returns400() throws Exception {
                // Act & Assert
                mockMvc.perform(post("/slib/areas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{invalid json}"))
                                .andExpect(status().isBadRequest());

                verify(areaService, never()).createArea(org.mockito.ArgumentMatchers.<AreaResponse>any());
        }

        // ====================================
        // === UPDATE AREA (FULL) ENDPOINT ===
        // ====================================

        @Test
        @DisplayName("updateArea_validData_returns200WithUpdatedArea")
        void updateArea_validData_returns200WithUpdatedArea() throws Exception {
                // Arrange
                Long areaId = 5L;
                AreaResponse request = createAreaResponse(null, "Updated Area Name", 900, 700, 100, 100, true, false);
                AreaResponse response = createAreaResponse(areaId, "Updated Area Name", 900, 700, 100, 100, true,
                                false);

                when(areaService.updateAreaFull(eq(areaId), any(AreaResponse.class))).thenReturn(response);

                // Act & Assert
                mockMvc.perform(put("/slib/areas/{id}", areaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.areaId").value(areaId))
                                .andExpect(jsonPath("$.areaName").value("Updated Area Name"))
                                .andExpect(jsonPath("$.width").value(900));

                verify(areaService, times(1)).updateAreaFull(eq(areaId), any(AreaResponse.class));
        }

        @Test
        @DisplayName("updateArea_notFound_throwsRuntimeException")
        void updateArea_notFound_throwsRuntimeException() throws Exception {
                // Arrange
                Long areaId = 999L;
                AreaResponse request = createAreaResponse(null, "Updated Area", 800, 600, 0, 0, true, false);

                when(areaService.updateAreaFull(eq(areaId), any(AreaResponse.class)))
                                .thenThrow(new RuntimeException("Area not found"));

                // Act & Assert
                mockMvc.perform(put("/slib/areas/{id}", areaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(areaService, times(1)).updateAreaFull(eq(areaId), any(AreaResponse.class));
        }

        // ============================================
        // === UPDATE AREA POSITION ENDPOINT ===
        // ============================================

        @Test
        @DisplayName("updateAreaPosition_validData_returns200")
        void updateAreaPosition_validData_returns200() throws Exception {
                // Arrange
                Long areaId = 3L;
                AreaResponse request = createAreaResponse(null, null, null, null, 150, 250, null, null);
                AreaResponse response = createAreaResponse(areaId, "Study Area", 800, 600, 150, 250, true, false);

                when(areaService.updateAreaPosition(eq(areaId), any(AreaResponse.class))).thenReturn(response);

                // Act & Assert
                mockMvc.perform(put("/slib/areas/{id}/position", areaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.areaId").value(areaId))
                                .andExpect(jsonPath("$.positionX").value(150))
                                .andExpect(jsonPath("$.positionY").value(250));

                verify(areaService, times(1)).updateAreaPosition(eq(areaId), any(AreaResponse.class));
        }

        @Test
        @DisplayName("updateAreaPosition_lockedArea_throwsRuntimeException")
        void updateAreaPosition_lockedArea_throwsRuntimeException() throws Exception {
                // Arrange
                Long areaId = 3L;
                AreaResponse request = createAreaResponse(null, null, null, null, 150, 250, null, null);

                when(areaService.updateAreaPosition(eq(areaId), any(AreaResponse.class)))
                                .thenThrow(new RuntimeException("Area is locked"));

                // Act & Assert
                mockMvc.perform(put("/slib/areas/{id}/position", areaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(areaService, times(1)).updateAreaPosition(eq(areaId), any(AreaResponse.class));
        }

        // ==============================================
        // === UPDATE AREA DIMENSIONS ENDPOINT ===
        // ==============================================

        @Test
        @DisplayName("updateAreaDimensions_validData_returns200")
        void updateAreaDimensions_validData_returns200() throws Exception {
                // Arrange
                Long areaId = 4L;
                AreaResponse request = createAreaResponse(null, null, 1000, 800, null, null, null, null);
                AreaResponse response = createAreaResponse(areaId, "Reading Area", 1000, 800, 100, 100, true, false);

                when(areaService.updateAreaDimensions(eq(areaId), any(AreaResponse.class))).thenReturn(response);

                // Act & Assert
                mockMvc.perform(put("/slib/areas/{id}/dimensions", areaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.areaId").value(areaId))
                                .andExpect(jsonPath("$.width").value(1000))
                                .andExpect(jsonPath("$.height").value(800));

                verify(areaService, times(1)).updateAreaDimensions(eq(areaId), any(AreaResponse.class));
        }

        @Test
        @DisplayName("updateAreaDimensions_lockedArea_throwsRuntimeException")
        void updateAreaDimensions_lockedArea_throwsRuntimeException() throws Exception {
                // Arrange
                Long areaId = 4L;
                AreaResponse request = createAreaResponse(null, null, 1000, 800, null, null, null, null);

                when(areaService.updateAreaDimensions(eq(areaId), any(AreaResponse.class)))
                                .thenThrow(new RuntimeException("Area is locked"));

                // Act & Assert
                mockMvc.perform(put("/slib/areas/{id}/dimensions", areaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(areaService, times(1)).updateAreaDimensions(eq(areaId), any(AreaResponse.class));
        }

        // ========================================================
        // === UPDATE AREA POSITION AND DIMENSIONS ENDPOINT ===
        // ========================================================

        @Test
        @DisplayName("updateAreaPositionAndDimensions_validData_returns200")
        void updateAreaPositionAndDimensions_validData_returns200() throws Exception {
                // Arrange
                Long areaId = 6L;
                AreaResponse request = createAreaResponse(null, null, 1200, 900, 200, 300, null, null);
                AreaResponse response = createAreaResponse(areaId, "Computer Lab", 1200, 900, 200, 300, true, false);

                when(areaService.updateAreaPositionAndDimensions(eq(areaId), any(AreaResponse.class)))
                                .thenReturn(response);

                // Act & Assert
                mockMvc.perform(put("/slib/areas/{id}/position-and-dimensions", areaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.areaId").value(areaId))
                                .andExpect(jsonPath("$.width").value(1200))
                                .andExpect(jsonPath("$.height").value(900))
                                .andExpect(jsonPath("$.positionX").value(200))
                                .andExpect(jsonPath("$.positionY").value(300));

                verify(areaService, times(1)).updateAreaPositionAndDimensions(eq(areaId), any(AreaResponse.class));
        }

        // ==========================================
        // === UPDATE AREA LOCKED STATUS ENDPOINT ===
        // ==========================================

        @Test
        @DisplayName("updateAreaLocked_validData_returns200")
        void updateAreaLocked_validData_returns200() throws Exception {
                // Arrange
                Long areaId = 7L;
                AreaResponse request = createAreaResponse(null, null, null, null, null, null, null, true);
                AreaResponse response = createAreaResponse(areaId, "Study Area", 800, 600, 100, 100, true, true);

                when(areaService.updateAreaLocked(eq(areaId), any(AreaResponse.class))).thenReturn(response);

                // Act & Assert
                mockMvc.perform(put("/slib/areas/{id}/locked", areaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.areaId").value(areaId))
                                .andExpect(jsonPath("$.locked").value(true));

                verify(areaService, times(1)).updateAreaLocked(eq(areaId), any(AreaResponse.class));
        }

        @Test
        @DisplayName("updateAreaLocked_notFound_throwsRuntimeException")
        void updateAreaLocked_notFound_throwsRuntimeException() throws Exception {
                // Arrange
                Long areaId = 999L;
                AreaResponse request = createAreaResponse(null, null, null, null, null, null, null, true);

                when(areaService.updateAreaLocked(eq(areaId), any(AreaResponse.class)))
                                .thenThrow(new RuntimeException("Area not found"));

                // Act & Assert
                mockMvc.perform(put("/slib/areas/{id}/locked", areaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(areaService, times(1)).updateAreaLocked(eq(areaId), any(AreaResponse.class));
        }

        // ===========================================
        // === UPDATE AREA ACTIVE STATUS ENDPOINT ===
        // ===========================================

        @Test
        @DisplayName("updateAreaIsActive_validData_returns200")
        void updateAreaIsActive_validData_returns200() throws Exception {
                // Arrange
                Long areaId = 8L;
                AreaResponse request = createAreaResponse(null, null, null, null, null, null, false, null);
                AreaResponse response = createAreaResponse(areaId, "Closed Area", 800, 600, 100, 100, false, false);

                when(areaService.updateAreaIsActive(eq(areaId), any(AreaResponse.class))).thenReturn(response);

                // Act & Assert
                mockMvc.perform(put("/slib/areas/{id}/active", areaId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.areaId").value(areaId))
                                .andExpect(jsonPath("$.isActive").value(false));

                verify(areaService, times(1)).updateAreaIsActive(eq(areaId), any(AreaResponse.class));
        }

        // ====================================
        // === DELETE AREA ENDPOINT ===
        // ====================================

        @Test
        @DisplayName("deleteArea_validId_returns204NoContent")
        void deleteArea_validId_returns204NoContent() throws Exception {
                // Arrange
                Long areaId = 5L;
                doNothing().when(areaService).deleteArea(areaId);

                // Act & Assert
                mockMvc.perform(delete("/slib/areas/{id}", areaId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNoContent());

                verify(areaService, times(1)).deleteArea(areaId);
        }

        @Test
        @DisplayName("deleteArea_notFound_throwsRuntimeException")
        void deleteArea_notFound_throwsRuntimeException() throws Exception {
                // Arrange
                Long areaId = 999L;
                doThrow(new RuntimeException("Area not found"))
                                .when(areaService).deleteArea(areaId);

                // Act & Assert
                mockMvc.perform(delete("/slib/areas/{id}", areaId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isInternalServerError());

                verify(areaService, times(1)).deleteArea(areaId);
        }

        @Test
        @DisplayName("deleteArea_invalidIdType_returns400")
        void deleteArea_invalidIdType_returns400() throws Exception {
                // Act & Assert
                mockMvc.perform(delete("/slib/areas/{id}", "invalid")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());

                verify(areaService, never()).deleteArea(any());
        }

        @Test
        @DisplayName("createArea_invalidName_returns400")
        void createArea_invalidName_returns400() throws Exception {
                AreaResponse request = createAreaResponse(null, "", -1, -1, 0, 0, true, false); // Tên trống, size âm

                mockMvc.perform(post("/slib/areas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // ==========================================
        // === HELPER METHOD TO CREATE TEST DATA ===
        // ==========================================

        /**
         * Helper method to create AreaResponse objects for testing
         * Matches the SLIB project's DTO structure
         */
        private AreaResponse createAreaResponse(Long areaId, String areaName, Integer width, Integer height,
                        Integer positionX, Integer positionY, Boolean isActive, Boolean locked) {
                AreaResponse response = new AreaResponse();
                response.setAreaId(areaId);
                response.setAreaName(areaName);
                response.setWidth(width);
                response.setHeight(height);
                response.setPositionX(positionX);
                response.setPositionY(positionY);
                response.setIsActive(isActive);
                response.setLocked(locked);
                return response;
        }
}
