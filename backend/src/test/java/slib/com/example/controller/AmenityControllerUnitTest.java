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
import slib.com.example.dto.AmenityResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.AmenityService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for AmenityController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 * Test Type: @WebMvcTest (Unit Tests only - no full context)
 */
@WebMvcTest(value = AmenityController.class, 
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, 
    classes = {slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AmenityController Unit Tests")
class AmenityControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AmenityService amenityService;

    @Autowired
    private ObjectMapper objectMapper;

    // ===================================
    // === GET ALL AMENITIES ENDPOINT ===
    // ===================================

    @Test
    @DisplayName("getAmenities_withoutZoneId_returns200WithAllAmenities")
    void getAmenities_withoutZoneId_returns200WithAllAmenities() throws Exception {
        // Arrange
        AmenityResponse amenity1 = new AmenityResponse(1, 10, "WiFi");
        AmenityResponse amenity2 = new AmenityResponse(2, 20, "Power Outlet");
        List<AmenityResponse> amenities = Arrays.asList(amenity1, amenity2);

        when(amenityService.getAllAmenities()).thenReturn(amenities);

        // Act & Assert
        mockMvc.perform(get("/slib/zone_amenities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].amenityId").value(1))
                .andExpect(jsonPath("$[0].amenityName").value("WiFi"))
                .andExpect(jsonPath("$[1].amenityId").value(2))
                .andExpect(jsonPath("$[1].amenityName").value("Power Outlet"));

        verify(amenityService, times(1)).getAllAmenities();
        verify(amenityService, never()).getAmenitiesByZoneId(any());
    }

    @Test
    @DisplayName("getAmenities_withZoneId_returns200WithFilteredAmenities")
    void getAmenities_withZoneId_returns200WithFilteredAmenities() throws Exception {
        // Arrange
        Integer zoneId = 10;
        AmenityResponse amenity1 = new AmenityResponse(1, zoneId, "WiFi");
        AmenityResponse amenity2 = new AmenityResponse(3, zoneId, "AC");
        List<AmenityResponse> filteredAmenities = Arrays.asList(amenity1, amenity2);

        when(amenityService.getAmenitiesByZoneId(zoneId)).thenReturn(filteredAmenities);

        // Act & Assert
        mockMvc.perform(get("/slib/zone_amenities")
                        .param("zoneId", String.valueOf(zoneId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].zoneId").value(zoneId))
                .andExpect(jsonPath("$[1].zoneId").value(zoneId));

        verify(amenityService, times(1)).getAmenitiesByZoneId(zoneId);
        verify(amenityService, never()).getAllAmenities();
    }

    @Test
    @DisplayName("getAmenities_withZoneIdReturnsEmpty_returns200WithEmptyArray")
    void getAmenities_withZoneIdReturnsEmpty_returns200WithEmptyArray() throws Exception {
        // Arrange
        Integer zoneId = 999;
        when(amenityService.getAmenitiesByZoneId(zoneId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/slib/zone_amenities")
                        .param("zoneId", String.valueOf(zoneId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(amenityService, times(1)).getAmenitiesByZoneId(zoneId);
    }

    // =======================================
    // === GET AMENITY BY ID ENDPOINT ===
    // =======================================

    @Test
    @DisplayName("getAmenityById_validId_returns200WithAmenity")
    void getAmenityById_validId_returns200WithAmenity() throws Exception {
        // Arrange
        Integer amenityId = 5;
        AmenityResponse amenity = new AmenityResponse(amenityId, 10, "Reading Lamp");

        when(amenityService.getAmenityById(amenityId)).thenReturn(amenity);

        // Act & Assert
        mockMvc.perform(get("/slib/zone_amenities/{id}", amenityId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amenityId").value(amenityId))
                .andExpect(jsonPath("$.zoneId").value(10))
                .andExpect(jsonPath("$.amenityName").value("Reading Lamp"));

        verify(amenityService, times(1)).getAmenityById(amenityId);
    }

    @Test
    @DisplayName("getAmenityById_notFound_throwsRuntimeException")
    void getAmenityById_notFound_throwsRuntimeException() throws Exception {
        // Arrange
        Integer amenityId = 999;
        when(amenityService.getAmenityById(amenityId))
                .thenThrow(new RuntimeException("Amenity not found"));

        // Act & Assert - Verify exception is thrown (no exception handler in unit test context)
        mockMvc.perform(get("/slib/zone_amenities/{id}", amenityId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(amenityService, times(1)).getAmenityById(amenityId);
    }

    // ====================================
    // === CREATE AMENITY ENDPOINT ===
    // ====================================

    @Test
    @DisplayName("createAmenity_validData_returns200WithCreatedAmenity")
    void createAmenity_validData_returns200WithCreatedAmenity() throws Exception {
        // Arrange
        AmenityResponse request = new AmenityResponse(null, 10, "New WiFi Router");
        AmenityResponse response = new AmenityResponse(1, 10, "New WiFi Router");

        when(amenityService.createAmenity(any(AmenityResponse.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/slib/zone_amenities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amenityId").value(1))
                .andExpect(jsonPath("$.zoneId").value(10))
                .andExpect(jsonPath("$.amenityName").value("New WiFi Router"));

        verify(amenityService, times(1)).createAmenity(any(AmenityResponse.class));
    }

    @Test
    @DisplayName("createAmenity_invalidZoneId_throwsRuntimeException")
    void createAmenity_invalidZoneId_throwsRuntimeException() throws Exception {
        // Arrange
        AmenityResponse request = new AmenityResponse(null, 999, "New WiFi Router");

        when(amenityService.createAmenity(any(AmenityResponse.class)))
                .thenThrow(new RuntimeException("Zone not found"));

        // Act & Assert - Verify exception is thrown
        mockMvc.perform(post("/slib/zone_amenities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(amenityService, times(1)).createAmenity(any(AmenityResponse.class));
    }

    @Test
    @DisplayName("createAmenity_emptyRequestBody_returns400")
    void createAmenity_emptyRequestBody_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/slib/zone_amenities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        verify(amenityService, never()).createAmenity(any());
    }

    @Test
    @DisplayName("createAmenity_invalidJson_returns400")
    void createAmenity_invalidJson_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/slib/zone_amenities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verify(amenityService, never()).createAmenity(any());
    }

    // ====================================
    // === UPDATE AMENITY ENDPOINT ===
    // ====================================

    @Test
    @DisplayName("updateAmenity_validData_returns200WithUpdatedAmenity")
    void updateAmenity_validData_returns200WithUpdatedAmenity() throws Exception {
        // Arrange
        Integer amenityId = 5;
        AmenityResponse request = new AmenityResponse(null, 10, "Updated WiFi");
        AmenityResponse response = new AmenityResponse(amenityId, 10, "Updated WiFi");

        when(amenityService.updateAmenity(eq(amenityId), any(AmenityResponse.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/slib/zone_amenities/{id}", amenityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amenityId").value(amenityId))
                .andExpect(jsonPath("$.amenityName").value("Updated WiFi"));

        verify(amenityService, times(1))
                .updateAmenity(eq(amenityId), any(AmenityResponse.class));
    }

    @Test
    @DisplayName("updateAmenity_notFound_throwsRuntimeException")
    void updateAmenity_notFound_throwsRuntimeException() throws Exception {
        // Arrange
        Integer amenityId = 999;
        AmenityResponse request = new AmenityResponse(null, 10, "Updated WiFi");

        when(amenityService.updateAmenity(eq(amenityId), any(AmenityResponse.class)))
                .thenThrow(new RuntimeException("Amenity not found"));

        // Act & Assert
        mockMvc.perform(put("/slib/zone_amenities/{id}", amenityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(amenityService, times(1))
                .updateAmenity(eq(amenityId), any(AmenityResponse.class));
    }

    @Test
    @DisplayName("updateAmenity_emptyRequestBody_returns400")
    void updateAmenity_emptyRequestBody_returns400() throws Exception {
        // Arrange
        Integer amenityId = 5;

        // Act & Assert
        mockMvc.perform(put("/slib/zone_amenities/{id}", amenityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        verify(amenityService, never()).updateAmenity(any(), any());
    }

    @Test
    @DisplayName("updateAmenity_invalidJson_returns400")
    void updateAmenity_invalidJson_returns400() throws Exception {
        // Arrange
        Integer amenityId = 5;

        // Act & Assert
        mockMvc.perform(put("/slib/zone_amenities/{id}", amenityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid: json"))
                .andExpect(status().isBadRequest());

        verify(amenityService, never()).updateAmenity(any(), any());
    }

    // ====================================
    // === DELETE AMENITY ENDPOINT ===
    // ====================================

    @Test
    @DisplayName("deleteAmenity_validId_returns200WithSuccessMessage")
    void deleteAmenity_validId_returns200WithSuccessMessage() throws Exception {
        // Arrange
        Integer amenityId = 5;
        doNothing().when(amenityService).deleteAmenity(amenityId);

        // Act & Assert
        mockMvc.perform(delete("/slib/zone_amenities/{id}", amenityId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Deleted amenity with id = " + amenityId));

        verify(amenityService, times(1)).deleteAmenity(amenityId);
    }

    @Test
    @DisplayName("deleteAmenity_notFound_throwsRuntimeException")
    void deleteAmenity_notFound_throwsRuntimeException() throws Exception {
        // Arrange
        Integer amenityId = 999;
        doThrow(new RuntimeException("Amenity not found"))
                .when(amenityService).deleteAmenity(amenityId);

        // Act & Assert
        mockMvc.perform(delete("/slib/zone_amenities/{id}", amenityId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(amenityService, times(1)).deleteAmenity(amenityId);
    }

    @Test
    @DisplayName("deleteAmenity_invalidId_throwsRuntimeException")
    void deleteAmenity_invalidId_throwsRuntimeException() throws Exception {
        // Arrange
        Integer amenityId = -1;
        doThrow(new RuntimeException("Invalid amenity ID"))
                .when(amenityService).deleteAmenity(amenityId);

        // Act & Assert
        mockMvc.perform(delete("/slib/zone_amenities/{id}", amenityId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(amenityService, times(1)).deleteAmenity(amenityId);
    }

    // ==========================================
    // === ADDITIONAL EDGE CASE TESTS ===
    // ==========================================

    @Test
    @DisplayName("getAmenities_serviceThrowsException_returns500")
    void getAmenities_serviceThrowsException_returns500() throws Exception {
        // Arrange
        when(amenityService.getAllAmenities())
                .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        mockMvc.perform(get("/slib/zone_amenities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(amenityService, times(1)).getAllAmenities();
    }

    @Test
    @DisplayName("createAmenity_nullZoneId_processesByService")
    void createAmenity_nullZoneId_processesByService() throws Exception {
        // Arrange
        AmenityResponse request = new AmenityResponse(null, null, "WiFi");

        when(amenityService.createAmenity(any(AmenityResponse.class)))
                .thenThrow(new RuntimeException("Zone not found"));

        // Act & Assert
        mockMvc.perform(post("/slib/zone_amenities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(amenityService, times(1)).createAmenity(any(AmenityResponse.class));
    }

    @Test
    @DisplayName("updateAmenity_partialUpdate_returns200")
    void updateAmenity_partialUpdate_returns200() throws Exception {
        // Arrange
        Integer amenityId = 5;
        AmenityResponse request = new AmenityResponse(null, null, "Only Name Changed");
        AmenityResponse response = new AmenityResponse(amenityId, 10, "Only Name Changed");

        when(amenityService.updateAmenity(eq(amenityId), any(AmenityResponse.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/slib/zone_amenities/{id}", amenityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amenityId").value(amenityId))
                .andExpect(jsonPath("$.amenityName").value("Only Name Changed"));

        verify(amenityService, times(1))
                .updateAmenity(eq(amenityId), any(AmenityResponse.class));
    }

    @Test
    @DisplayName("getAmenityById_invalidIdType_returns400")
    void getAmenityById_invalidIdType_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/slib/zone_amenities/{id}", "invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(amenityService, never()).getAmenityById(any());
    }

    @Test
    @DisplayName("deleteAmenity_invalidIdType_returns400")
    void deleteAmenity_invalidIdType_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/slib/zone_amenities/{id}", "invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(amenityService, never()).deleteAmenity(any());
    }
}
