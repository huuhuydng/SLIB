package slib.com.example.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
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
@DisplayName("FE-29: View zone details - Unit Tests")
class FE29_ViewZoneDetailsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ZoneService zoneService;

    @MockBean
    private BookingService bookingService;

    @Test
    @DisplayName("UTCID01: View zone details with full fields")
    void viewZoneDetailsWithFullFields() throws Exception {
        when(zoneService.getZoneById(1)).thenReturn(zoneResponse(1, "Quiet Zone", "Near window", 2, 4, 8, 6, false));

        mockMvc.perform(get("/slib/zones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value(1))
                .andExpect(jsonPath("$.zoneName").value("Quiet Zone"))
                .andExpect(jsonPath("$.zoneDes").value("Near window"));

        verify(zoneService).getZoneById(1);
    }

    @Test
    @DisplayName("UTCID02: View zone details when optional fields are null")
    void viewZoneDetailsWhenOptionalFieldsAreNull() throws Exception {
        when(zoneService.getZoneById(2)).thenReturn(zoneResponse(2, "Group Zone", null, 6, 3, 10, 8, true));

        mockMvc.perform(get("/slib/zones/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value(2))
                .andExpect(jsonPath("$.zoneName").value("Group Zone"))
                .andExpect(jsonPath("$.isLocked").value(true));

        verify(zoneService).getZoneById(2);
    }

    @Test
    @DisplayName("UTCID03: View zone details with invalid zoneId format")
    void viewZoneDetailsWithInvalidZoneIdFormat() throws Exception {
        mockMvc.perform(get("/slib/zones/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("UTCID04: View zone details for non-existent zoneId")
    void viewZoneDetailsForNonExistentZoneId() throws Exception {
        when(zoneService.getZoneById(999))
                .thenThrow(new ResourceNotFoundException("Zone not found with id: 999"));

        mockMvc.perform(get("/slib/zones/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Zone not found with id: 999"));

        verify(zoneService).getZoneById(999);
    }

    @Test
    @DisplayName("UTCID05: View zone details when service throws runtime exception")
    void viewZoneDetailsWhenServiceThrowsRuntimeException() throws Exception {
        when(zoneService.getZoneById(5)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/slib/zones/5"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Database error"));

        verify(zoneService).getZoneById(5);
    }

    private ZoneResponse zoneResponse(Integer zoneId, String zoneName, String zoneDes,
            Integer positionX, Integer positionY, Integer width, Integer height, Boolean isLocked) {
        ZoneResponse response = new ZoneResponse();
        response.setZoneId(zoneId);
        response.setZoneName(zoneName);
        response.setZoneDes(zoneDes);
        response.setPositionX(positionX);
        response.setPositionY(positionY);
        response.setWidth(width);
        response.setHeight(height);
        response.setIsLocked(isLocked);
        response.setAreaId(1L);
        return response;
    }
}
