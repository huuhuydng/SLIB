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

import slib.com.example.controller.zone_config.ZoneController;
import slib.com.example.dto.zone_config.ZoneResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.booking.BookingService;
import slib.com.example.service.zone_config.ZoneService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ZoneController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-26: View zone map - Unit Tests")
class FE26_ViewZoneMapTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ZoneService zoneService;

    @MockBean
    private BookingService bookingService;

    @Test
    @DisplayName("UTCID01: View all zones")
    void viewAllZones() throws Exception {
        when(zoneService.getAllZones()).thenReturn(List.of(zoneResponse(1, "Quiet Zone", 1L, 2, 3, 8, 6, false)));

        mockMvc.perform(get("/slib/zones").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zoneId").value(1))
                .andExpect(jsonPath("$[0].zoneName").value("Quiet Zone"))
                .andExpect(jsonPath("$[0].areaId").value(1))
                .andExpect(jsonPath("$[0].isLocked").value(false));

        verify(zoneService).getAllZones();
    }

    @Test
    @DisplayName("UTCID02: View zones filtered by areaId")
    void viewZonesFilteredByAreaId() throws Exception {
        when(zoneService.getZonesByAreaId(2L))
                .thenReturn(List.of(zoneResponse(3, "Silent Zone", 2L, 4, 5, 10, 7, true)));

        mockMvc.perform(get("/slib/zones").param("areaId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zoneId").value(3))
                .andExpect(jsonPath("$[0].zoneName").value("Silent Zone"))
                .andExpect(jsonPath("$[0].areaId").value(2))
                .andExpect(jsonPath("$[0].isLocked").value(true));

        verify(zoneService).getZonesByAreaId(2L);
    }

    @Test
    @DisplayName("UTCID03: View zone map with invalid areaId format")
    void viewZonesWithInvalidAreaIdFormat() throws Exception {
        mockMvc.perform(get("/slib/zones").param("areaId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("UTCID04: View zone map when service throws runtime exception")
    void viewZoneMapWhenServiceThrowsRuntimeException() throws Exception {
        when(zoneService.getAllZones()).thenThrow(new RuntimeException("Database temporarily unavailable"));

        mockMvc.perform(get("/slib/zones"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Database temporarily unavailable"));

        verify(zoneService).getAllZones();
    }

    @Test
    @DisplayName("UTCID05: View zone map for non-existent zoneId")
    void viewZoneMapForNonExistentZoneId() throws Exception {
        when(zoneService.getZoneById(999))
                .thenThrow(new ResourceNotFoundException("Zone not found with id: 999"));

        mockMvc.perform(get("/slib/zones/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Zone not found with id: 999"));

        verify(zoneService).getZoneById(999);
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
