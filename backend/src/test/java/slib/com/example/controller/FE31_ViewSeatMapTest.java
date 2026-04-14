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
import org.springframework.test.web.servlet.MockMvc;

import slib.com.example.controller.zone_config.SeatController;
import slib.com.example.dto.zone_config.SeatResponse;
import slib.com.example.entity.zone_config.SeatStatus;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.booking.BookingService;
import slib.com.example.service.zone_config.SeatService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SeatController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-31: View seat map - Unit Tests")
class FE31_ViewSeatMapTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeatService seatService;

    @MockBean
    private BookingService bookingService;

    @Test
    @DisplayName("UTCID01: View all seats")
    void viewAllSeats() throws Exception {
        when(seatService.getAllSeats()).thenReturn(List.of(seatResponse(1, 2, "A1", SeatStatus.AVAILABLE, 1, 1, true)));

        mockMvc.perform(get("/slib/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seatId").value(1))
                .andExpect(jsonPath("$[0].seatCode").value("A1"))
                .andExpect(jsonPath("$[0].seatStatus").value("AVAILABLE"));

        verify(seatService).getAllSeats();
    }

    @Test
    @DisplayName("UTCID02: View seats by zoneId")
    void viewSeatsByZoneId() throws Exception {
        when(seatService.getSeatsByZoneId(2))
                .thenReturn(List.of(seatResponse(2, 2, "B3", SeatStatus.BOOKED, 2, 3, true)));

        mockMvc.perform(get("/slib/seats").param("zoneId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zoneId").value(2))
                .andExpect(jsonPath("$[0].seatCode").value("B3"))
                .andExpect(jsonPath("$[0].seatStatus").value("BOOKED"));

        verify(seatService).getSeatsByZoneId(2);
    }

    @Test
    @DisplayName("UTCID03: View seats by zoneId and time range")
    void viewSeatsByZoneIdAndTimeRange() throws Exception {
        when(seatService.getSeatsByTimeRange("2026-04-10T08:00:00", "2026-04-10T10:00:00", 2))
                .thenReturn(List.of(seatResponse(3, 2, "C2", SeatStatus.HOLDING, 3, 2, true)));

        mockMvc.perform(get("/slib/seats")
                        .param("zoneId", "2")
                        .param("startTime", "2026-04-10T08:00:00")
                        .param("endTime", "2026-04-10T10:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seatId").value(3))
                .andExpect(jsonPath("$[0].seatStatus").value("HOLDING"));

        verify(seatService).getSeatsByTimeRange("2026-04-10T08:00:00", "2026-04-10T10:00:00", 2);
    }

    @Test
    @DisplayName("UTCID04: View seat map with invalid time range")
    void viewSeatMapWithInvalidTimeRange() throws Exception {
        when(seatService.getSeatsByTimeRange("bad-time", "2026-04-10T10:00:00", 2))
                .thenThrow(new IllegalArgumentException("Text 'bad-time' could not be parsed"));

        mockMvc.perform(get("/slib/seats")
                        .param("zoneId", "2")
                        .param("startTime", "bad-time")
                        .param("endTime", "2026-04-10T10:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(jsonPath("$.message").value("Text 'bad-time' could not be parsed"));

        verify(seatService).getSeatsByTimeRange("bad-time", "2026-04-10T10:00:00", 2);
    }

    @Test
    @DisplayName("UTCID05: View seat details for non-existent seatId")
    void viewSeatDetailsForNonExistentSeatId() throws Exception {
        when(seatService.getSeatById(999))
                .thenThrow(new ResourceNotFoundException("Seat not found with id: 999"));

        mockMvc.perform(get("/slib/seats/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Seat not found with id: 999"));

        verify(seatService).getSeatById(999);
    }

    @Test
    @DisplayName("UTCID06: View seat details when service throws runtime exception")
    void viewSeatDetailsWhenServiceThrowsRuntimeException() throws Exception {
        when(seatService.getSeatById(5)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/slib/seats/5"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Database error"));

        verify(seatService).getSeatById(5);
    }

    private SeatResponse seatResponse(Integer seatId, Integer zoneId, String seatCode,
            SeatStatus seatStatus, Integer rowNumber, Integer columnNumber, Boolean isActive) {
        SeatResponse response = new SeatResponse();
        response.setSeatId(seatId);
        response.setZoneId(zoneId);
        response.setSeatCode(seatCode);
        response.setSeatStatus(seatStatus);
        response.setRowNumber(rowNumber);
        response.setColumnNumber(columnNumber);
        response.setIsActive(isActive);
        return response;
    }
}
