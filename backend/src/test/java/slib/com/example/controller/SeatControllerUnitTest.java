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
import slib.com.example.dto.SeatDTO;
import slib.com.example.dto.SeatResponse;
import slib.com.example.entity.SeatStatus;
import slib.com.example.service.BookingService;
import slib.com.example.service.SeatService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for SeatController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 * Test Type: @WebMvcTest (Unit Tests only - no full context)
 */
@WebMvcTest(value = SeatController.class, 
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, 
    classes = {slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SeatController Unit Tests")
class SeatControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeatService seatService;

    @MockBean
    private BookingService bookingService;

    @Autowired
    private ObjectMapper objectMapper;

    // ========================================
    // === CREATE SEAT ENDPOINT ===
    // ========================================

    @Test
    @DisplayName("createSeat_validData_returns200WithCreatedSeat")
    void createSeat_validData_returns200WithCreatedSeat() throws Exception {
        // Arrange
        SeatResponse request = createSeatResponse(null, 5, "A01", SeatStatus.AVAILABLE, 100, 150, 80, 60, 1, 1);
        SeatResponse response = createSeatResponse(10, 5, "A01", SeatStatus.AVAILABLE, 100, 150, 80, 60, 1, 1);

        when(seatService.createSeat(any(SeatResponse.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/slib/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatId").value(10))
                .andExpect(jsonPath("$.seatCode").value("A01"))
                .andExpect(jsonPath("$.seatStatus").value("AVAILABLE"));

        verify(seatService, times(1)).createSeat(any(SeatResponse.class));
    }

    @Test
    @DisplayName("createSeat_emptyRequestBody_returns400")
    void createSeat_emptyRequestBody_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/slib/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        verify(seatService, never()).createSeat(any());
    }

    // ===============================================
    // === GET SEATS (WITH OPTIONAL FILTER) ENDPOINT ===
    // ===============================================

    @Test
    @DisplayName("getSeats_withoutFilter_returns200WithAllSeats")
    void getSeats_withoutFilter_returns200WithAllSeats() throws Exception {
        // Arrange
        SeatResponse seat1 = createSeatResponse(1, 2, "B01", SeatStatus.AVAILABLE, 50, 100, 80, 60, 1, 1);
        SeatResponse seat2 = createSeatResponse(2, 3, "C02", SeatStatus.BOOKED, 150, 200, 80, 60, 2, 1);
        List<SeatResponse> seats = Arrays.asList(seat1, seat2);

        when(seatService.getAllSeats()).thenReturn(seats);

        // Act & Assert
        mockMvc.perform(get("/slib/seats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].seatCode").value("B01"))
                .andExpect(jsonPath("$[1].seatCode").value("C02"));

        verify(seatService, times(1)).getAllSeats();
        verify(seatService, never()).getSeatsByZoneId(any());
    }

    @Test
    @DisplayName("getSeats_withZoneIdFilter_returns200WithFilteredSeats")
    void getSeats_withZoneIdFilter_returns200WithFilteredSeats() throws Exception {
        // Arrange
        Integer zoneId = 10;
        SeatResponse seat1 = createSeatResponse(3, zoneId, "D01", SeatStatus.AVAILABLE, 100, 100, 80, 60, 1, 2);
        SeatResponse seat2 = createSeatResponse(4, zoneId, "D02", SeatStatus.AVAILABLE, 200, 100, 80, 60, 1, 3);
        List<SeatResponse> seats = Arrays.asList(seat1, seat2);

        when(seatService.getSeatsByZoneId(zoneId)).thenReturn(seats);

        // Act & Assert
        mockMvc.perform(get("/slib/seats")
                        .param("zoneId", zoneId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].zoneId").value(zoneId))
                .andExpect(jsonPath("$[1].zoneId").value(zoneId));

        verify(seatService, times(1)).getSeatsByZoneId(zoneId);
        verify(seatService, never()).getAllSeats();
    }

    // =========================================
    // === GET SEAT BY ID ENDPOINT ===
    // =========================================

    @Test
    @DisplayName("getSeatById_validId_returns200WithSeat")
    void getSeatById_validId_returns200WithSeat() throws Exception {
        // Arrange
        Integer seatId = 15;
        SeatResponse seat = createSeatResponse(seatId, 7, "E05", SeatStatus.BOOKED, 300, 250, 80, 60, 3, 2);

        when(seatService.getSeatById(seatId)).thenReturn(seat);

        // Act & Assert
        mockMvc.perform(get("/slib/seats/{id}", seatId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatId").value(seatId))
                .andExpect(jsonPath("$.seatCode").value("E05"))
                .andExpect(jsonPath("$.seatStatus").value("BOOKED"));

        verify(seatService, times(1)).getSeatById(seatId);
    }

    @Test
    @DisplayName("getSeatById_notFound_throwsRuntimeException")
    void getSeatById_notFound_throwsRuntimeException() throws Exception {
        // Arrange
        Integer seatId = 999;
        when(seatService.getSeatById(seatId))
                .thenThrow(new RuntimeException("Seat not found"));

        // Act & Assert
        mockMvc.perform(get("/slib/seats/{id}", seatId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(seatService, times(1)).getSeatById(seatId);
    }

    // ===============================================
    // === UPDATE SEAT POSITION ENDPOINT ===
    // ===============================================

    @Test
    @DisplayName("updateSeatPosition_validData_returns200WithUpdatedPosition")
    void updateSeatPosition_validData_returns200WithUpdatedPosition() throws Exception {
        // Arrange
        Integer seatId = 20;
        SeatResponse request = createSeatResponse(null, null, null, null, 400, 500, null, null, null, null);
        SeatResponse response = createSeatResponse(seatId, 5, "F01", SeatStatus.AVAILABLE, 400, 500, 80, 60, 2, 1);

        when(seatService.updateSeatPosition(eq(seatId), any(SeatResponse.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/slib/seats/{id}/position", seatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatId").value(seatId))
                .andExpect(jsonPath("$.positionX").value(400))
                .andExpect(jsonPath("$.positionY").value(500));

        verify(seatService, times(1)).updateSeatPosition(eq(seatId), any(SeatResponse.class));
    }

    // ================================================
    // === UPDATE SEAT DIMENSIONS ENDPOINT ===
    // ================================================

    @Test
    @DisplayName("updateSeatDimensions_validData_returns200WithUpdatedDimensions")
    void updateSeatDimensions_validData_returns200WithUpdatedDimensions() throws Exception {
        // Arrange
        Integer seatId = 25;
        SeatResponse request = createSeatResponse(null, null, null, null, null, null, 100, 80, null, null);
        SeatResponse response = createSeatResponse(seatId, 8, "G03", SeatStatus.AVAILABLE, 200, 300, 100, 80, 1, 3);

        when(seatService.updateSeatDimensions(eq(seatId), any(SeatResponse.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/slib/seats/{id}/dimensions", seatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatId").value(seatId))
                .andExpect(jsonPath("$.width").value(100))
                .andExpect(jsonPath("$.height").value(80));

        verify(seatService, times(1)).updateSeatDimensions(eq(seatId), any(SeatResponse.class));
    }

    // ===========================================================
    // === UPDATE SEAT POSITION AND DIMENSIONS ENDPOINT ===
    // ===========================================================

    @Test
    @DisplayName("updateSeatPositionAndDimensions_validData_returns200")
    void updateSeatPositionAndDimensions_validData_returns200() throws Exception {
        // Arrange
        Integer seatId = 30;
        SeatResponse request = createSeatResponse(null, null, null, null, 350, 450, 90, 70, null, null);
        SeatResponse response = createSeatResponse(seatId, 12, "H04", SeatStatus.AVAILABLE, 350, 450, 90, 70, 2, 4);

        when(seatService.updateSeatPositionAndDimensions(eq(seatId), any(SeatResponse.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/slib/seats/{id}/position-and-dimensions", seatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatId").value(seatId))
                .andExpect(jsonPath("$.positionX").value(350))
                .andExpect(jsonPath("$.positionY").value(450))
                .andExpect(jsonPath("$.width").value(90))
                .andExpect(jsonPath("$.height").value(70));

        verify(seatService, times(1)).updateSeatPositionAndDimensions(eq(seatId), any(SeatResponse.class));
    }

    // ========================================
    // === UPDATE SEAT (FULL) ENDPOINT ===
    // ========================================

    @Test
    @DisplayName("updateSeat_validData_returns200WithUpdatedSeat")
    void updateSeat_validData_returns200WithUpdatedSeat() throws Exception {
        // Arrange
        Integer seatId = 35;
        SeatResponse request = createSeatResponse(null, 9, "I05", SeatStatus.UNAVAILABLE, 250, 350, 85, 65, 3, 5);
        SeatResponse response = createSeatResponse(seatId, 9, "I05", SeatStatus.UNAVAILABLE, 250, 350, 85, 65, 3, 5);

        when(seatService.updateSeat(eq(seatId), any(SeatResponse.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/slib/seats/{id}", seatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatId").value(seatId))
                .andExpect(jsonPath("$.seatCode").value("I05"))
                .andExpect(jsonPath("$.seatStatus").value("UNAVAILABLE"));

        verify(seatService, times(1)).updateSeat(eq(seatId), any(SeatResponse.class));
    }

    // ======================================
    // === DELETE SEAT ENDPOINT ===
    // ======================================

    @Test
    @DisplayName("deleteSeat_validId_returns200WithSuccessMessage")
    void deleteSeat_validId_returns200WithSuccessMessage() throws Exception {
        // Arrange
        Integer seatId = 40;
        doNothing().when(seatService).deleteSeat(seatId);

        // Act & Assert
        mockMvc.perform(delete("/slib/seats/{id}", seatId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Deleted seat with id = " + seatId));

        verify(seatService, times(1)).deleteSeat(seatId);
    }

    @Test
    @DisplayName("deleteSeat_notFound_throwsRuntimeException")
    void deleteSeat_notFound_throwsRuntimeException() throws Exception {
        // Arrange
        Integer seatId = 999;
        doThrow(new RuntimeException("Seat not found")).when(seatService).deleteSeat(seatId);

        // Act & Assert
        mockMvc.perform(delete("/slib/seats/{id}", seatId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(seatService, times(1)).deleteSeat(seatId);
    }

    // =================================================
    // === GET AVAILABLE SEATS COUNT ENDPOINT ===
    // =================================================

    @Test
    @DisplayName("getAvailableSeats_validZoneId_returns200WithCount")
    void getAvailableSeats_validZoneId_returns200WithCount() throws Exception {
        // Arrange
        Integer zoneId = 15;
        long availableCount = 42L;

        when(bookingService.countAvailableSeats(zoneId)).thenReturn(availableCount);

        // Act & Assert
        mockMvc.perform(get("/slib/seats/getAvailableSeat/{zoneId}", zoneId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(availableCount));

        verify(bookingService, times(1)).countAvailableSeats(zoneId);
    }

    // =============================================
    // === GET ALL SEATS DTO ENDPOINT ===
    // =============================================

    @Test
    @DisplayName("getAllSeats_validZoneId_returns200WithSeatDTOList")
    void getAllSeats_validZoneId_returns200WithSeatDTOList() throws Exception {
        // Arrange
        Integer zoneId = 20;
        SeatDTO dto1 = new SeatDTO();
        dto1.setSeatId(1);
        dto1.setSeatCode("J01");

        SeatDTO dto2 = new SeatDTO();
        dto2.setSeatId(2);
        dto2.setSeatCode("J02");

        List<SeatDTO> seatDTOs = Arrays.asList(dto1, dto2);

        when(bookingService.getAllSeatsDTO(zoneId)).thenReturn(seatDTOs);

        // Act & Assert
        mockMvc.perform(get("/slib/seats/getAllSeat/{zoneId}", zoneId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(bookingService, times(1)).getAllSeatsDTO(zoneId);
    }

    // =====================================================
    // === GET SEATS BY TIME RANGE ENDPOINT ===
    // =====================================================

    @Test
    @DisplayName("getSeatsByTime_validParameters_returns200WithSeatDTOList")
    void getSeatsByTime_validParameters_returns200WithSeatDTOList() throws Exception {
        // Arrange
        Integer zoneId = 25;
        LocalDate date = LocalDate.of(2026, 1, 20);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(11, 0);

        SeatDTO dto1 = new SeatDTO();
        dto1.setSeatId(5);
        dto1.setSeatCode("K01");

        List<SeatDTO> seatDTOs = List.of(dto1);

        when(bookingService.getSeatsByTime(zoneId, date, start, end)).thenReturn(seatDTOs);

        // Act & Assert
        mockMvc.perform(get("/slib/seats/getSeatsByTime/{zoneId}", zoneId)
                        .param("date", date.toString())
                        .param("start", start.toString())
                        .param("end", end.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(bookingService, times(1)).getSeatsByTime(zoneId, date, start, end);
    }

    @Test
    @DisplayName("getSeatsByTime_missingParameters_returns400")
    void getSeatsByTime_missingParameters_returns400() throws Exception {
        // Arrange
        Integer zoneId = 25;

        // Act & Assert
        mockMvc.perform(get("/slib/seats/getSeatsByTime/{zoneId}", zoneId)
                        .param("date", "2026-01-20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).getSeatsByTime(any(), any(), any(), any());
    }

    // ==============================================
    // === GET SEATS BY DATE ENDPOINT ===
    // ==============================================

    @Test
    @DisplayName("getSeatsByDate_validParameters_returns200WithSeatDTOList")
    void getSeatsByDate_validParameters_returns200WithSeatDTOList() throws Exception {
        // Arrange
        Integer zoneId = 30;
        String dateString = "2026-01-25";
        LocalDate date = LocalDate.parse(dateString);

        SeatDTO dto1 = new SeatDTO();
        dto1.setSeatId(10);
        dto1.setSeatCode("L01");

        SeatDTO dto2 = new SeatDTO();
        dto2.setSeatId(11);
        dto2.setSeatCode("L02");

        List<SeatDTO> seatDTOs = Arrays.asList(dto1, dto2);

        when(bookingService.getSeatsByDate(zoneId, date)).thenReturn(seatDTOs);

        // Act & Assert
        mockMvc.perform(get("/slib/seats/getSeatsByDate/{zoneId}", zoneId)
                        .param("date", dateString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(bookingService, times(1)).getSeatsByDate(zoneId, date);
    }

    @Test
    @DisplayName("getSeatsByDate_missingDateParameter_returns400")
    void getSeatsByDate_missingDateParameter_returns400() throws Exception {
        // Arrange
        Integer zoneId = 30;

        // Act & Assert
        mockMvc.perform(get("/slib/seats/getSeatsByDate/{zoneId}", zoneId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).getSeatsByDate(any(), any());
    }

    // ==========================================
    // === HELPER METHOD TO CREATE TEST DATA ===
    // ==========================================

    /**
     * Helper method to create SeatResponse objects for testing
     */
    private SeatResponse createSeatResponse(Integer seatId, Integer zoneId, String seatCode,
                                            SeatStatus seatStatus, Integer positionX, Integer positionY,
                                            Integer width, Integer height, Integer rowNumber, Integer columnNumber) {
        return new SeatResponse(seatId, zoneId, seatCode, seatStatus, positionX, positionY, 
                               width, height, rowNumber, columnNumber);
    }
}
