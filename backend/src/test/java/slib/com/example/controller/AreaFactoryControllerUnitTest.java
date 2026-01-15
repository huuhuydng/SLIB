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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.dto.AreaFactoryResponse;
import slib.com.example.entity.AreaFactoryEntity;
import slib.com.example.service.AreaFactoryService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for AreaFactoryController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 * Test Type: @WebMvcTest (Unit Tests only - no full context)
 */
@WebMvcTest(value = AreaFactoryController.class, 
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, 
    classes = {slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AreaFactoryController Unit Tests")
class AreaFactoryControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AreaFactoryService areaFactoryService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==========================================
    // === GET ALL AREA FACTORIES ENDPOINT ===
    // ==========================================

    @Test
    @DisplayName("getAll_success_returns200WithFactoriesList")
    void getAll_success_returns200WithFactoriesList() throws Exception {
        // Arrange
        AreaFactoryResponse factory1 = createFactoryResponse(1L, "Table Group A", 1L, 100, 150, 200, 100, "#3498db");
        AreaFactoryResponse factory2 = createFactoryResponse(2L, "Chair Set B", 1L, 300, 200, 150, 80, "#e74c3c");
        List<AreaFactoryResponse> factories = Arrays.asList(factory1, factory2);

        when(areaFactoryService.getAllAreaFactories()).thenReturn(factories);

        // Act & Assert
        mockMvc.perform(get("/slib/area_factories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].factoryId").value(1))
                .andExpect(jsonPath("$[0].factoryName").value("Table Group A"))
                .andExpect(jsonPath("$[1].factoryId").value(2))
                .andExpect(jsonPath("$[1].factoryName").value("Chair Set B"));

        verify(areaFactoryService, times(1)).getAllAreaFactories();
    }

    @Test
    @DisplayName("getAll_emptyList_returns200WithEmptyArray")
    void getAll_emptyList_returns200WithEmptyArray() throws Exception {
        // Arrange
        when(areaFactoryService.getAllAreaFactories()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/slib/area_factories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(areaFactoryService, times(1)).getAllAreaFactories();
    }

    @Test
    @DisplayName("getAll_serviceThrowsException_returns500")
    void getAll_serviceThrowsException_returns500() throws Exception {
        // Arrange
        when(areaFactoryService.getAllAreaFactories())
                .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        mockMvc.perform(get("/slib/area_factories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(areaFactoryService, times(1)).getAllAreaFactories();
    }

    // =================================================
    // === GET FACTORIES BY AREA ID ENDPOINT ===
    // =================================================

    @Test
    @DisplayName("getByArea_validAreaId_returns200WithFactoriesList")
    void getByArea_validAreaId_returns200WithFactoriesList() throws Exception {
        // Arrange
        Long areaId = 5L;
        AreaFactoryResponse factory1 = createFactoryResponse(10L, "Desk A", areaId, 50, 100, 180, 90, "#2ecc71");
        AreaFactoryResponse factory2 = createFactoryResponse(11L, "Desk B", areaId, 250, 100, 180, 90, "#2ecc71");
        List<AreaFactoryResponse> factories = Arrays.asList(factory1, factory2);

        when(areaFactoryService.getFactoriesByAreaId(areaId)).thenReturn(factories);

        // Act & Assert
        mockMvc.perform(get("/slib/area_factories/area/{areaId}", areaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].areaId").value(areaId))
                .andExpect(jsonPath("$[1].areaId").value(areaId));

        verify(areaFactoryService, times(1)).getFactoriesByAreaId(areaId);
    }

    @Test
    @DisplayName("getByArea_noFactoriesFound_returns200WithEmptyArray")
    void getByArea_noFactoriesFound_returns200WithEmptyArray() throws Exception {
        // Arrange
        Long areaId = 99L;
        when(areaFactoryService.getFactoriesByAreaId(areaId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/slib/area_factories/area/{areaId}", areaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(areaFactoryService, times(1)).getFactoriesByAreaId(areaId);
    }

    @Test
    @DisplayName("getByArea_invalidAreaIdType_returns400")
    void getByArea_invalidAreaIdType_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/slib/area_factories/area/{areaId}", "invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(areaFactoryService, never()).getFactoriesByAreaId(any());
    }

    // ==============================================
    // === GET FACTORY BY ID ENDPOINT ===
    // ==============================================

    @Test
    @DisplayName("getById_validId_returns200WithFactory")
    void getById_validId_returns200WithFactory() throws Exception {
        // Arrange
        Long factoryId = 15L;
        AreaFactoryResponse factory = createFactoryResponse(factoryId, "Meeting Table", 3L, 400, 300, 300, 200, "#9b59b6");

        when(areaFactoryService.getAreaFactoryById(factoryId)).thenReturn(factory);

        // Act & Assert
        mockMvc.perform(get("/slib/area_factories/{id}", factoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factoryId").value(factoryId))
                .andExpect(jsonPath("$.factoryName").value("Meeting Table"))
                .andExpect(jsonPath("$.width").value(300))
                .andExpect(jsonPath("$.height").value(200));

        verify(areaFactoryService, times(1)).getAreaFactoryById(factoryId);
    }

    @Test
    @DisplayName("getById_notFound_throwsRuntimeException")
    void getById_notFound_throwsRuntimeException() throws Exception {
        // Arrange
        Long factoryId = 999L;
        when(areaFactoryService.getAreaFactoryById(factoryId))
                .thenThrow(new RuntimeException("Factory not found with id: " + factoryId));

        // Act & Assert
        mockMvc.perform(get("/slib/area_factories/{id}", factoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(areaFactoryService, times(1)).getAreaFactoryById(factoryId);
    }

    @Test
    @DisplayName("getById_invalidIdType_returns400")
    void getById_invalidIdType_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/slib/area_factories/{id}", "invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(areaFactoryService, never()).getAreaFactoryById(any());
    }

    // =========================================
    // === CREATE FACTORY ENDPOINT ===
    // =========================================

    @Test
    @DisplayName("create_validData_returns200WithCreatedFactory")
    void create_validData_returns200WithCreatedFactory() throws Exception {
        // Arrange
        Long areaId = 2L;
        AreaFactoryEntity requestEntity = createFactoryEntity(null, "New Workstation", 100, 200, 160, 80, "#34495e");
        AreaFactoryResponse response = createFactoryResponse(20L, "New Workstation", areaId, 100, 200, 160, 80, "#34495e");

        when(areaFactoryService.createAreaFactory(eq(areaId), any(AreaFactoryEntity.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/slib/area_factories/area/{areaId}", areaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestEntity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factoryId").value(20))
                .andExpect(jsonPath("$.factoryName").value("New Workstation"))
                .andExpect(jsonPath("$.areaId").value(areaId))
                .andExpect(jsonPath("$.positionX").value(100))
                .andExpect(jsonPath("$.positionY").value(200));

        verify(areaFactoryService, times(1)).createAreaFactory(eq(areaId), any(AreaFactoryEntity.class));
    }

    @Test
    @DisplayName("create_emptyRequestBody_returns400")
    void create_emptyRequestBody_returns400() throws Exception {
        // Arrange
        Long areaId = 2L;

        // Act & Assert
        mockMvc.perform(post("/slib/area_factories/area/{areaId}", areaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        verify(areaFactoryService, never()).createAreaFactory(any(), any());
    }

    @Test
    @DisplayName("create_invalidJson_returns400")
    void create_invalidJson_returns400() throws Exception {
        // Arrange
        Long areaId = 2L;

        // Act & Assert
        mockMvc.perform(post("/slib/area_factories/area/{areaId}", areaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verify(areaFactoryService, never()).createAreaFactory(any(), any());
    }

    @Test
    @DisplayName("create_areaNotFound_throwsRuntimeException")
    void create_areaNotFound_throwsRuntimeException() throws Exception {
        // Arrange
        Long areaId = 999L;
        AreaFactoryEntity requestEntity = createFactoryEntity(null, "New Table", 50, 50, 150, 100, "#1abc9c");

        when(areaFactoryService.createAreaFactory(eq(areaId), any(AreaFactoryEntity.class)))
                .thenThrow(new RuntimeException("Area not found with id: " + areaId));

        // Act & Assert
        mockMvc.perform(post("/slib/area_factories/area/{areaId}", areaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestEntity)))
                .andExpect(status().isInternalServerError());

        verify(areaFactoryService, times(1)).createAreaFactory(eq(areaId), any(AreaFactoryEntity.class));
    }

    // =========================================
    // === UPDATE FACTORY ENDPOINT ===
    // =========================================

    @Test
    @DisplayName("update_validData_returns200WithUpdatedFactory")
    void update_validData_returns200WithUpdatedFactory() throws Exception {
        // Arrange
        Long factoryId = 10L;
        AreaFactoryEntity requestEntity = createFactoryEntity(null, "Updated Desk", 150, 180, 200, 100, "#e67e22");
        AreaFactoryResponse response = createFactoryResponse(factoryId, "Updated Desk", 5L, 150, 180, 200, 100, "#e67e22");

        when(areaFactoryService.updateAreaFactory(eq(factoryId), any(AreaFactoryEntity.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/slib/area_factories/{id}", factoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestEntity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factoryId").value(factoryId))
                .andExpect(jsonPath("$.factoryName").value("Updated Desk"))
                .andExpect(jsonPath("$.positionX").value(150))
                .andExpect(jsonPath("$.width").value(200));

        verify(areaFactoryService, times(1)).updateAreaFactory(eq(factoryId), any(AreaFactoryEntity.class));
    }

    @Test
    @DisplayName("update_notFound_throwsRuntimeException")
    void update_notFound_throwsRuntimeException() throws Exception {
        // Arrange
        Long factoryId = 999L;
        AreaFactoryEntity requestEntity = createFactoryEntity(null, "Non-existent", 100, 100, 150, 100, "#95a5a6");

        when(areaFactoryService.updateAreaFactory(eq(factoryId), any(AreaFactoryEntity.class)))
                .thenThrow(new RuntimeException("Factory not found"));

        // Act & Assert
        mockMvc.perform(put("/slib/area_factories/{id}", factoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestEntity)))
                .andExpect(status().isInternalServerError());

        verify(areaFactoryService, times(1)).updateAreaFactory(eq(factoryId), any(AreaFactoryEntity.class));
    }

    @Test
    @DisplayName("update_emptyRequestBody_returns400")
    void update_emptyRequestBody_returns400() throws Exception {
        // Arrange
        Long factoryId = 10L;

        // Act & Assert
        mockMvc.perform(put("/slib/area_factories/{id}", factoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        verify(areaFactoryService, never()).updateAreaFactory(any(), any());
    }

    // ===========================================
    // === DRAG FACTORY (PATCH POSITION) ENDPOINT ===
    // ===========================================

    @Test
    @DisplayName("drag_validCoordinates_returns200WithUpdatedPosition")
    void drag_validCoordinates_returns200WithUpdatedPosition() throws Exception {
        // Arrange
        Long factoryId = 12L;
        Integer newX = 250;
        Integer newY = 350;
        AreaFactoryResponse response = createFactoryResponse(factoryId, "Dragged Table", 4L, newX, newY, 180, 90, "#16a085");

        when(areaFactoryService.dragAreaFactory(factoryId, newX, newY)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/slib/area_factories/{id}/drag", factoryId)
                        .param("x", String.valueOf(newX))
                        .param("y", String.valueOf(newY))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factoryId").value(factoryId))
                .andExpect(jsonPath("$.positionX").value(newX))
                .andExpect(jsonPath("$.positionY").value(newY));

        verify(areaFactoryService, times(1)).dragAreaFactory(factoryId, newX, newY);
    }

    @Test
    @DisplayName("drag_missingXParameter_returns400")
    void drag_missingXParameter_returns400() throws Exception {
        // Arrange
        Long factoryId = 12L;

        // Act & Assert
        mockMvc.perform(patch("/slib/area_factories/{id}/drag", factoryId)
                        .param("y", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(areaFactoryService, never()).dragAreaFactory(any(), any(), any());
    }

    @Test
    @DisplayName("drag_missingYParameter_returns400")
    void drag_missingYParameter_returns400() throws Exception {
        // Arrange
        Long factoryId = 12L;

        // Act & Assert
        mockMvc.perform(patch("/slib/area_factories/{id}/drag", factoryId)
                        .param("x", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(areaFactoryService, never()).dragAreaFactory(any(), any(), any());
    }

    @Test
    @DisplayName("drag_invalidParameterType_returns400")
    void drag_invalidParameterType_returns400() throws Exception {
        // Arrange
        Long factoryId = 12L;

        // Act & Assert
        mockMvc.perform(patch("/slib/area_factories/{id}/drag", factoryId)
                        .param("x", "invalid")
                        .param("y", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(areaFactoryService, never()).dragAreaFactory(any(), any(), any());
    }

    @Test
    @DisplayName("drag_factoryNotFound_throwsRuntimeException")
    void drag_factoryNotFound_throwsRuntimeException() throws Exception {
        // Arrange
        Long factoryId = 999L;
        Integer newX = 100;
        Integer newY = 200;

        when(areaFactoryService.dragAreaFactory(factoryId, newX, newY))
                .thenThrow(new RuntimeException("Factory not found"));

        // Act & Assert
        mockMvc.perform(patch("/slib/area_factories/{id}/drag", factoryId)
                        .param("x", String.valueOf(newX))
                        .param("y", String.valueOf(newY))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(areaFactoryService, times(1)).dragAreaFactory(factoryId, newX, newY);
    }

    // ===============================================
    // === RESIZE FACTORY (PATCH DIMENSIONS) ENDPOINT ===
    // ===============================================

    @Test
    @DisplayName("resize_validDimensions_returns200WithUpdatedSize")
    void resize_validDimensions_returns200WithUpdatedSize() throws Exception {
        // Arrange
        Long factoryId = 14L;
        Integer newWidth = 250;
        Integer newHeight = 150;
        AreaFactoryResponse response = createFactoryResponse(factoryId, "Resized Chair", 6L, 100, 100, newWidth, newHeight, "#c0392b");

        when(areaFactoryService.resizeAreaFactory(factoryId, newWidth, newHeight)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/slib/area_factories/{id}/resize", factoryId)
                        .param("width", String.valueOf(newWidth))
                        .param("height", String.valueOf(newHeight))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factoryId").value(factoryId))
                .andExpect(jsonPath("$.width").value(newWidth))
                .andExpect(jsonPath("$.height").value(newHeight));

        verify(areaFactoryService, times(1)).resizeAreaFactory(factoryId, newWidth, newHeight);
    }

    @Test
    @DisplayName("resize_missingWidthParameter_returns400")
    void resize_missingWidthParameter_returns400() throws Exception {
        // Arrange
        Long factoryId = 14L;

        // Act & Assert
        mockMvc.perform(patch("/slib/area_factories/{id}/resize", factoryId)
                        .param("height", "150")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(areaFactoryService, never()).resizeAreaFactory(any(), any(), any());
    }

    @Test
    @DisplayName("resize_missingHeightParameter_returns400")
    void resize_missingHeightParameter_returns400() throws Exception {
        // Arrange
        Long factoryId = 14L;

        // Act & Assert
        mockMvc.perform(patch("/slib/area_factories/{id}/resize", factoryId)
                        .param("width", "250")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(areaFactoryService, never()).resizeAreaFactory(any(), any(), any());
    }

    @Test
    @DisplayName("resize_invalidParameterType_returns400")
    void resize_invalidParameterType_returns400() throws Exception {
        // Arrange
        Long factoryId = 14L;

        // Act & Assert
        mockMvc.perform(patch("/slib/area_factories/{id}/resize", factoryId)
                        .param("width", "invalid")
                        .param("height", "150")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(areaFactoryService, never()).resizeAreaFactory(any(), any(), any());
    }

    @Test
    @DisplayName("resize_factoryNotFound_throwsRuntimeException")
    void resize_factoryNotFound_throwsRuntimeException() throws Exception {
        // Arrange
        Long factoryId = 999L;
        Integer newWidth = 200;
        Integer newHeight = 100;

        when(areaFactoryService.resizeAreaFactory(factoryId, newWidth, newHeight))
                .thenThrow(new RuntimeException("Factory not found"));

        // Act & Assert
        mockMvc.perform(patch("/slib/area_factories/{id}/resize", factoryId)
                        .param("width", String.valueOf(newWidth))
                        .param("height", String.valueOf(newHeight))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(areaFactoryService, times(1)).resizeAreaFactory(factoryId, newWidth, newHeight);
    }

    // ======================================
    // === DELETE FACTORY ENDPOINT ===
    // ======================================

    @Test
    @DisplayName("delete_validId_returns200")
    void delete_validId_returns200() throws Exception {
        // Arrange
        Long factoryId = 18L;
        doNothing().when(areaFactoryService).deleteAreaFactory(factoryId);

        // Act & Assert
        mockMvc.perform(delete("/slib/area_factories/{id}", factoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(areaFactoryService, times(1)).deleteAreaFactory(factoryId);
    }

    @Test
    @DisplayName("delete_notFound_throwsRuntimeException")
    void delete_notFound_throwsRuntimeException() throws Exception {
        // Arrange
        Long factoryId = 999L;
        doThrow(new RuntimeException("Factory not found"))
                .when(areaFactoryService).deleteAreaFactory(factoryId);

        // Act & Assert
        mockMvc.perform(delete("/slib/area_factories/{id}", factoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(areaFactoryService, times(1)).deleteAreaFactory(factoryId);
    }

    @Test
    @DisplayName("delete_invalidIdType_returns400")
    void delete_invalidIdType_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/slib/area_factories/{id}", "invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(areaFactoryService, never()).deleteAreaFactory(any());
    }

    // ==========================================
    // === HELPER METHODS TO CREATE TEST DATA ===
    // ==========================================

    /**
     * Helper method to create AreaFactoryResponse objects for testing
     */
    private AreaFactoryResponse createFactoryResponse(Long factoryId, String factoryName, Long areaId,
                                                      Integer positionX, Integer positionY,
                                                      Integer width, Integer height, String color) {
        return new AreaFactoryResponse(factoryId, factoryName, areaId, positionX, positionY, width, height, color);
    }

    /**
     * Helper method to create AreaFactoryEntity objects for request bodies
     */
    private AreaFactoryEntity createFactoryEntity(Long factoryId, String factoryName,
                                                  Integer positionX, Integer positionY,
                                                  Integer width, Integer height, String color) {
        return AreaFactoryEntity.builder()
                .factoryId(factoryId)
                .factoryName(factoryName)
                .positionX(positionX)
                .positionY(positionY)
                .width(width)
                .height(height)
                .color(color)
                .build();
    }
}
