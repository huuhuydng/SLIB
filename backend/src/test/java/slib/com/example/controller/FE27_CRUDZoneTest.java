package slib.com.example.controller;

import java.util.List;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import slib.com.example.controller.zone_config.ZoneController;
import slib.com.example.dto.zone_config.ZoneOccupancyDTO;
import slib.com.example.dto.zone_config.ZoneResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.booking.BookingService;
import slib.com.example.service.zone_config.ZoneService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ZoneController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-27: CRUD zone - Unit Tests")
class FE27_CRUDZoneTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ZoneService zoneService;

    @MockBean
    private BookingService bookingService;

    @Test
    @DisplayName("UTCID01: List all zones")
    void listAllZones() throws Exception {
        when(zoneService.getAllZones()).thenReturn(List.of(zoneResponse(1, "Quiet Zone", 1L, 2, 4, 8, 6, false)));

        mockMvc.perform(get("/slib/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zoneId").value(1))
                .andExpect(jsonPath("$[0].zoneName").value("Quiet Zone"));

        verify(zoneService).getAllZones();
    }

    @Test
    @DisplayName("UTCID02: List zones by areaId")
    void listZonesByAreaId() throws Exception {
        when(zoneService.getZonesByAreaId(2L))
                .thenReturn(List.of(zoneResponse(2, "Group Zone", 2L, 6, 3, 10, 8, false)));

        mockMvc.perform(get("/slib/zones").param("areaId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].areaId").value(2))
                .andExpect(jsonPath("$[0].zoneName").value("Group Zone"));

        verify(zoneService).getZonesByAreaId(2L);
    }

    @Test
    @DisplayName("UTCID03: View zone details by zoneId")
    void viewZoneDetailsByZoneId() throws Exception {
        when(zoneService.getZoneById(3)).thenReturn(zoneResponse(3, "Window Zone", 1L, 8, 5, 12, 7, true));

        mockMvc.perform(get("/slib/zones/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value(3))
                .andExpect(jsonPath("$.zoneName").value("Window Zone"))
                .andExpect(jsonPath("$.isLocked").value(true));

        verify(zoneService).getZoneById(3);
    }

    @Test
    @DisplayName("UTCID04: View zone details with invalid zoneId format")
    void viewZoneDetailsWithInvalidZoneIdFormat() throws Exception {
        mockMvc.perform(get("/slib/zones/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("UTCID05: View zone details for non-existent zoneId")
    void viewZoneDetailsForNonExistentZoneId() throws Exception {
        when(zoneService.getZoneById(999))
                .thenThrow(new ResourceNotFoundException("Zone not found with id: 999"));

        mockMvc.perform(get("/slib/zones/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Zone not found with id: 999"));

        verify(zoneService).getZoneById(999);
    }

    @Test
    @DisplayName("UTCID06: Create zone with valid payload")
    void createZoneWithValidPayload() throws Exception {
        ZoneResponse request = zoneResponse(null, "Focus Zone", 1L, 1, 2, 9, 6, false);
        when(zoneService.createZone(any(ZoneResponse.class)))
                .thenReturn(zoneResponse(6, "Focus Zone", 1L, 1, 2, 9, 6, false));

        mockMvc.perform(post("/slib/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value(6))
                .andExpect(jsonPath("$.zoneName").value("Focus Zone"));

        verify(zoneService).createZone(any(ZoneResponse.class));
    }

    @Test
    @DisplayName("UTCID07: Create zone with negative positionX")
    void createZoneWithNegativePositionX() throws Exception {
        ZoneResponse request = zoneResponse(null, "Focus Zone", 1L, -1, 2, 9, 6, false);

        mockMvc.perform(post("/slib/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.positionX").value("Vị trí X không được âm"));
    }

    @Test
    @DisplayName("UTCID08: Create zone when service throws runtime exception")
    void createZoneWhenServiceThrowsRuntimeException() throws Exception {
        ZoneResponse request = zoneResponse(null, "Focus Zone", 99L, 1, 2, 9, 6, false);
        when(zoneService.createZone(any(ZoneResponse.class)))
                .thenThrow(new RuntimeException("Area not found"));

        mockMvc.perform(post("/slib/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Area not found"));

        verify(zoneService).createZone(any(ZoneResponse.class));
    }

    @Test
    @DisplayName("UTCID09: Update zone with full payload")
    void updateZoneWithFullPayload() throws Exception {
        ZoneResponse request = zoneResponse(null, "Focus Zone 2", 1L, 3, 4, 12, 8, false);
        when(zoneService.updateZoneFull(any(Integer.class), any(ZoneResponse.class)))
                .thenReturn(zoneResponse(6, "Focus Zone 2", 1L, 3, 4, 12, 8, false));

        mockMvc.perform(put("/slib/zones/6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneName").value("Focus Zone 2"))
                .andExpect(jsonPath("$.width").value(12));

        verify(zoneService).updateZoneFull(any(Integer.class), any(ZoneResponse.class));
    }

    @Test
    @DisplayName("UTCID10: Update zone with invalid width")
    void updateZoneWithInvalidWidth() throws Exception {
        ZoneResponse request = zoneResponse(null, "Focus Zone 2", 1L, 3, 4, 0, 8, false);

        mockMvc.perform(put("/slib/zones/6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.width").value("Chiều rộng khu vực phải lớn hơn 0"));
    }

    @Test
    @DisplayName("UTCID11: Update non-existent zone")
    void updateNonExistentZone() throws Exception {
        ZoneResponse request = zoneResponse(null, "Focus Zone 2", 1L, 3, 4, 12, 8, false);
        when(zoneService.updateZoneFull(any(Integer.class), any(ZoneResponse.class)))
                .thenThrow(new ResourceNotFoundException("Zone not found with id: 999"));

        mockMvc.perform(put("/slib/zones/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Zone not found with id: 999"));

        verify(zoneService).updateZoneFull(any(Integer.class), any(ZoneResponse.class));
    }

    @Test
    @DisplayName("UTCID12: Update zone position")
    void updateZonePosition() throws Exception {
        ZoneResponse request = zoneResponse(null, null, null, 7, 9, null, null, false);
        when(zoneService.updateZonePosition(any(Integer.class), any(ZoneResponse.class)))
                .thenReturn(zoneResponse(6, "Focus Zone", 1L, 7, 9, 9, 6, false));

        mockMvc.perform(put("/slib/zones/6/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionX").value(7))
                .andExpect(jsonPath("$.positionY").value(9));

        verify(zoneService).updateZonePosition(any(Integer.class), any(ZoneResponse.class));
    }

    @Test
    @DisplayName("UTCID13: Update zone dimensions")
    void updateZoneDimensions() throws Exception {
        ZoneResponse request = zoneResponse(null, null, null, null, null, 14, 9, false);
        when(zoneService.updateZoneDimensions(any(Integer.class), any(ZoneResponse.class)))
                .thenReturn(zoneResponse(6, "Focus Zone", 1L, 1, 2, 14, 9, false));

        mockMvc.perform(put("/slib/zones/6/dimensions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.width").value(14))
                .andExpect(jsonPath("$.height").value(9));

        verify(zoneService).updateZoneDimensions(any(Integer.class), any(ZoneResponse.class));
    }

    @Test
    @DisplayName("UTCID14: Update zone position and dimensions")
    void updateZonePositionAndDimensions() throws Exception {
        ZoneResponse request = zoneResponse(null, null, null, 5, 6, 15, 10, false);
        when(zoneService.updateZonePositionAndDimensions(any(Integer.class), any(ZoneResponse.class)))
                .thenReturn(zoneResponse(6, "Focus Zone", 1L, 5, 6, 15, 10, false));

        mockMvc.perform(put("/slib/zones/6/position-and-dimensions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionX").value(5))
                .andExpect(jsonPath("$.width").value(15));

        verify(zoneService).updateZonePositionAndDimensions(any(Integer.class), any(ZoneResponse.class));
    }

    @Test
    @DisplayName("UTCID15: Delete existing zone")
    void deleteExistingZone() throws Exception {
        mockMvc.perform(delete("/slib/zones/6"))
                .andExpect(status().isOk())
                .andExpect(content().string("Deleted zone with id = 6"));

        verify(zoneService).deleteZone(6);
    }

    @Test
    @DisplayName("UTCID16: Delete zone when service throws runtime exception")
    void deleteZoneWhenServiceThrowsRuntimeException() throws Exception {
        doThrow(new RuntimeException("Không thể xóa khu vực ghế đang bị khóa"))
                .when(zoneService).deleteZone(6);

        mockMvc.perform(delete("/slib/zones/6"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Không thể xóa khu vực ghế đang bị khóa"));

        verify(zoneService).deleteZone(6);
    }

    @Test
    @DisplayName("UTCID17: View zone occupancy by areaId")
    void viewZoneOccupancyByAreaId() throws Exception {
        when(zoneService.getZoneOccupancy(3L)).thenReturn(List.of(
                ZoneOccupancyDTO.builder()
                        .zoneId(4)
                        .zoneName("Reading Zone")
                        .color("green")
                        .totalSeats(20L)
                        .occupiedSeats(8L)
                        .occupancyRate(0.4)
                        .build()));

        mockMvc.perform(get("/slib/zones/occupancy/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zoneId").value(4))
                .andExpect(jsonPath("$[0].zoneName").value("Reading Zone"))
                .andExpect(jsonPath("$[0].occupancyRate").value(0.4));

        verify(zoneService).getZoneOccupancy(3L);
    }

    private ZoneResponse zoneResponse(Integer zoneId, String zoneName, Long areaId,
            Integer positionX, Integer positionY, Integer width, Integer height, Boolean isLocked) {
        ZoneResponse response = new ZoneResponse();
        response.setZoneId(zoneId);
        response.setZoneName(zoneName);
        response.setAreaId(areaId);
        response.setPositionX(positionX);
        response.setPositionY(positionY);
        response.setWidth(width);
        response.setHeight(height);
        response.setIsLocked(isLocked);
        return response;
    }
}
