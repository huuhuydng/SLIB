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
import slib.com.example.dto.ZoneResponse;
import slib.com.example.entity.ZoneEntity;
import slib.com.example.service.BookingService;
import slib.com.example.service.ZoneService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for ZoneController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 * Test Type: @WebMvcTest (Unit Tests only - no full context)
 */
@WebMvcTest(value = ZoneController.class, 
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, 
    classes = {slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ZoneController Unit Tests")
class ZoneControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ZoneService zoneService;

    @MockBean
    private BookingService bookingService;

    @Autowired
    private ObjectMapper objectMapper;

    // =============================================
    // === GET ALL ZONES (Legacy) ENDPOINT ===
    // =============================================

    @Test
    @DisplayName("getAllZones_success_returns200WithZonesList")
    void getAllZones_success_returns200WithZonesList() throws Exception {
        // Arrange
        ZoneEntity zone1 = new ZoneEntity();
        zone1.setZoneId(1);
        zone1.setZoneName("Zone A");

        ZoneEntity zone2 = new ZoneEntity();
        zone2.setZoneId(2);
        zone2.setZoneName("Zone B");

        List<ZoneEntity> zones = Arrays.asList(zone1, zone2);

        when(bookingService.getAllZones()).thenReturn(zones);

        // Act & Assert
        mockMvc.perform(get("/slib/zones/getAllZones")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].zoneId").value(1))
                .andExpect(jsonPath("$[0].zoneName").value("Zone A"));

        verify(bookingService, times(1)).getAllZones();
    }

    // ===============================================
    // === GET ZONES (WITH OPTIONAL FILTER) ENDPOINT ===
    // ===============================================

    @Test
    @DisplayName("getZones_withoutFilter_returns200WithAllZones")
    void getZones_withoutFilter_returns200WithAllZones() throws Exception {
        // Arrange
        ZoneResponse zone1 = createZoneResponse(1, "Reading Zone", "Quiet area", 100, 200, 800, 600, 5L, false, "#3498db");
        ZoneResponse zone2 = createZoneResponse(2, "Study Zone", "Group study", 200, 300, 700, 500, 5L, false, "#e74c3c");
        List<ZoneResponse> zones = Arrays.asList(zone1, zone2);

        when(zoneService.getAllZones()).thenReturn(zones);

        // Act & Assert
        mockMvc.perform(get("/slib/zones")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].zoneId").value(1))
                .andExpect(jsonPath("$[1].zoneId").value(2));

        verify(zoneService, times(1)).getAllZones();
        verify(zoneService, never()).getZonesByAreaId(any());
    }

    @Test
    @DisplayName("getZones_withAreaIdFilter_returns200WithFilteredZones")
    void getZones_withAreaIdFilter_returns200WithFilteredZones() throws Exception {
        // Arrange
        Long areaId = 10L;
        ZoneResponse zone1 = createZoneResponse(3, "Computer Lab", "Tech zone", 50, 100, 900, 700, areaId, false, "#2ecc71");
        List<ZoneResponse> zones = List.of(zone1);

        when(zoneService.getZonesByAreaId(areaId)).thenReturn(zones);

        // Act & Assert
        mockMvc.perform(get("/slib/zones")
                        .param("areaId", areaId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].areaId").value(areaId));

        verify(zoneService, times(1)).getZonesByAreaId(areaId);
        verify(zoneService, never()).getAllZones();
    }

    @Test
    @DisplayName("getZones_emptyResult_returns200WithEmptyArray")
    void getZones_emptyResult_returns200WithEmptyArray() throws Exception {
        // Arrange
        when(zoneService.getAllZones()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/slib/zones")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(zoneService, times(1)).getAllZones();
    }

    // =========================================
    // === GET ZONE BY ID ENDPOINT ===
    // =========================================

    @Test
    @DisplayName("getZoneById_validId_returns200WithZone")
    void getZoneById_validId_returns200WithZone() throws Exception {
        // Arrange
        Integer zoneId = 5;
        ZoneResponse zone = createZoneResponse(zoneId, "VIP Zone", "Premium seating", 300, 400, 1000, 800, 15L, true, "#9b59b6");

        when(zoneService.getZoneById(zoneId)).thenReturn(zone);

        // Act & Assert
        mockMvc.perform(get("/slib/zones/{id}", zoneId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value(zoneId))
                .andExpect(jsonPath("$.zoneName").value("VIP Zone"))
                .andExpect(jsonPath("$.isLocked").value(true));

        verify(zoneService, times(1)).getZoneById(zoneId);
    }

    @Test
    @DisplayName("getZoneById_notFound_throwsRuntimeException")
    void getZoneById_notFound_throwsRuntimeException() throws Exception {
        // Arrange
        Integer zoneId = 999;
        when(zoneService.getZoneById(zoneId))
                .thenThrow(new RuntimeException("Zone not found with id: " + zoneId));

        // Act & Assert
        mockMvc.perform(get("/slib/zones/{id}", zoneId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(zoneService, times(1)).getZoneById(zoneId);
    }

    // =====================================
    // === CREATE ZONE ENDPOINT ===
    // =====================================

    @Test
    @DisplayName("createZone_validData_returns200WithCreatedZone")
    void createZone_validData_returns200WithCreatedZone() throws Exception {
        // Arrange
        ZoneResponse request = createZoneResponse(null, "New Zone", "Brand new", 100, 100, 600, 400, 8L, false, "#16a085");
        ZoneResponse response = createZoneResponse(10, "New Zone", "Brand new", 100, 100, 600, 400, 8L, false, "#16a085");

        when(zoneService.createZone(any(ZoneResponse.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/slib/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value(10))
                .andExpect(jsonPath("$.zoneName").value("New Zone"))
                .andExpect(jsonPath("$.areaId").value(8));

        verify(zoneService, times(1)).createZone(any(ZoneResponse.class));
    }

    @Test
    @DisplayName("createZone_emptyRequestBody_returns400")
    void createZone_emptyRequestBody_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/slib/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        verify(zoneService, never()).createZone(any());
    }

    // ==========================================
    // === UPDATE ZONE (FULL) ENDPOINT ===
    // ==========================================

    @Test
    @DisplayName("updateZone_validData_returns200WithUpdatedZone")
    void updateZone_validData_returns200WithUpdatedZone() throws Exception {
        // Arrange
        Integer zoneId = 7;
        ZoneResponse request = createZoneResponse(null, "Updated Zone", "Updated description", 150, 250, 850, 650, 12L, true, "#c0392b");
        ZoneResponse response = createZoneResponse(zoneId, "Updated Zone", "Updated description", 150, 250, 850, 650, 12L, true, "#c0392b");

        when(zoneService.updateZoneFull(eq(zoneId), any(ZoneResponse.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/slib/zones/{id}", zoneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value(zoneId))
                .andExpect(jsonPath("$.zoneName").value("Updated Zone"))
                .andExpect(jsonPath("$.isLocked").value(true));

        verify(zoneService, times(1)).updateZoneFull(eq(zoneId), any(ZoneResponse.class));
    }

    @Test
    @DisplayName("updateZone_notFound_throwsRuntimeException")
    void updateZone_notFound_throwsRuntimeException() throws Exception {
        // Arrange
        Integer zoneId = 999;
        ZoneResponse request = createZoneResponse(null, "Non-existent", "N/A", 100, 100, 600, 400, 5L, false, "#95a5a6");

        when(zoneService.updateZoneFull(eq(zoneId), any(ZoneResponse.class)))
                .thenThrow(new RuntimeException("Zone not found"));

        // Act & Assert
        mockMvc.perform(put("/slib/zones/{id}", zoneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(zoneService, times(1)).updateZoneFull(eq(zoneId), any(ZoneResponse.class));
    }

    // =============================================
    // === UPDATE ZONE POSITION ENDPOINT ===
    // =============================================

    @Test
    @DisplayName("updateZonePosition_validData_returns200WithUpdatedPosition")
    void updateZonePosition_validData_returns200WithUpdatedPosition() throws Exception {
        // Arrange
        Integer zoneId = 4;
        ZoneResponse request = createZoneResponse(null, null, null, 200, 300, null, null, null, null, null);
        ZoneResponse response = createZoneResponse(zoneId, "Existing Zone", "Desc", 200, 300, 700, 500, 6L, false, "#34495e");

        when(zoneService.updateZonePosition(eq(zoneId), any(ZoneResponse.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/slib/zones/{id}/position", zoneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value(zoneId))
                .andExpect(jsonPath("$.positionX").value(200))
                .andExpect(jsonPath("$.positionY").value(300));

        verify(zoneService, times(1)).updateZonePosition(eq(zoneId), any(ZoneResponse.class));
    }

    // =============================================
    // === UPDATE ZONE DIMENSIONS ENDPOINT ===
    // =============================================

    @Test
    @DisplayName("updateZoneDimensions_validData_returns200WithUpdatedDimensions")
    void updateZoneDimensions_validData_returns200WithUpdatedDimensions() throws Exception {
        // Arrange
        Integer zoneId = 6;
        ZoneResponse request = createZoneResponse(null, null, null, null, null, 1000, 800, null, null, null);
        ZoneResponse response = createZoneResponse(zoneId, "Resized Zone", "Bigger", 100, 200, 1000, 800, 9L, false, "#27ae60");

        when(zoneService.updateZoneDimensions(eq(zoneId), any(ZoneResponse.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/slib/zones/{id}/dimensions", zoneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value(zoneId))
                .andExpect(jsonPath("$.width").value(1000))
                .andExpect(jsonPath("$.height").value(800));

        verify(zoneService, times(1)).updateZoneDimensions(eq(zoneId), any(ZoneResponse.class));
    }

    // ===========================================================
    // === UPDATE ZONE POSITION AND DIMENSIONS ENDPOINT ===
    // ===========================================================

    @Test
    @DisplayName("updateZonePositionAndDimensions_validData_returns200")
    void updateZonePositionAndDimensions_validData_returns200() throws Exception {
        // Arrange
        Integer zoneId = 8;
        ZoneResponse request = createZoneResponse(null, null, null, 300, 400, 1200, 900, null, null, null);
        ZoneResponse response = createZoneResponse(zoneId, "Combined Update Zone", "Both", 300, 400, 1200, 900, 11L, false, "#8e44ad");

        when(zoneService.updateZonePositionAndDimensions(eq(zoneId), any(ZoneResponse.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/slib/zones/{id}/position-and-dimensions", zoneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value(zoneId))
                .andExpect(jsonPath("$.positionX").value(300))
                .andExpect(jsonPath("$.positionY").value(400))
                .andExpect(jsonPath("$.width").value(1200))
                .andExpect(jsonPath("$.height").value(900));

        verify(zoneService, times(1)).updateZonePositionAndDimensions(eq(zoneId), any(ZoneResponse.class));
    }

    // ======================================
    // === DELETE ZONE ENDPOINT ===
    // ======================================

    @Test
    @DisplayName("deleteZone_validId_returns200WithSuccessMessage")
    void deleteZone_validId_returns200WithSuccessMessage() throws Exception {
        // Arrange
        Integer zoneId = 12;
        doNothing().when(zoneService).deleteZone(zoneId);

        // Act & Assert
        mockMvc.perform(delete("/slib/zones/{id}", zoneId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Deleted zone with id = " + zoneId));

        verify(zoneService, times(1)).deleteZone(zoneId);
    }

    @Test
    @DisplayName("deleteZone_notFound_throwsRuntimeException")
    void deleteZone_notFound_throwsRuntimeException() throws Exception {
        // Arrange
        Integer zoneId = 999;
        doThrow(new RuntimeException("Zone not found")).when(zoneService).deleteZone(zoneId);

        // Act & Assert
        mockMvc.perform(delete("/slib/zones/{id}", zoneId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(zoneService, times(1)).deleteZone(zoneId);
    }

    // ==========================================
    // === HELPER METHOD TO CREATE TEST DATA ===
    // ==========================================

    /**
     * Helper method to create ZoneResponse objects for testing
     */
    private ZoneResponse createZoneResponse(Integer zoneId, String zoneName, String zoneDes,
                                            Integer positionX, Integer positionY,
                                            Integer width, Integer height,
                                            Long areaId, Boolean isLocked, String color) {
        return new ZoneResponse(zoneId, zoneName, zoneDes, positionX, positionY, width, height, areaId, isLocked, color);
    }
}
