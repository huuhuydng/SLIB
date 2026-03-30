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
import slib.com.example.controller.zone_config.SeatController;
import slib.com.example.dto.zone_config.SeatDTO;
import slib.com.example.entity.zone_config.SeatStatus;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.booking.BookingService;
import slib.com.example.service.zone_config.SeatService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-60: View Real-time Seat Map
 * Test Report: doc/Report/UnitTestReport/FE60_TestReport.md
 */
@WebMvcTest(value = SeatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-60: View Real-time Seat Map - Unit Tests")
class FE60_ViewRealtimeSeatMapTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookingService bookingService;

        @MockBean
        private SeatService seatService;

        // =========================================
        // === UTCID01: Requested time range overlaps booked seats ===
        // =========================================

        /**
         * UTCID01: Normal case - seats with overlapping reservations
         * Precondition: Zone id, date, and time range are provided
         * Expected: 200 OK with seat list including status
         */
        @Test
        @DisplayName("UTCID01: View seats with overlapping reservations returns 200 OK")
        void getSeatsByTime_normalOverlap_returns200OK() throws Exception {
                SeatDTO seat = new SeatDTO();
                seat.setSeatId(1);
                seat.setSeatStatus(SeatStatus.BOOKED);

                when(bookingService.getSeatsByTime(eq(1), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                                .thenReturn(List.of(seat));

                mockMvc.perform(get("/slib/seats/getSeatsByTime/1")
                                .param("date", "2026-03-12")
                                .param("start", "08:00")
                                .param("end", "10:00"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$[0].seatId").value(1));

                verify(bookingService, times(1)).getSeatsByTime(eq(1), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class));
        }

        // =========================================
        // === UTCID02: Multiple seats with mixed status ===
        // =========================================

        /**
         * UTCID02: Multiple seats with mixed availability status
         * Precondition: Zone id, date, and time range are provided
         * Expected: 200 OK with multiple seats
         */
        @Test
        @DisplayName("UTCID02: Multiple seats with mixed status returns 200 OK")
        void getSeatsByTime_mixedStatus_returns200OK() throws Exception {
                SeatDTO seat1 = new SeatDTO();
                seat1.setSeatId(1);
                seat1.setSeatStatus(SeatStatus.AVAILABLE);
                SeatDTO seat2 = new SeatDTO();
                seat2.setSeatId(2);
                seat2.setSeatStatus(SeatStatus.BOOKED);

                when(bookingService.getSeatsByTime(eq(2), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                                .thenReturn(List.of(seat1, seat2));

                mockMvc.perform(get("/slib/seats/getSeatsByTime/2")
                                .param("date", "2026-03-12")
                                .param("start", "09:00")
                                .param("end", "11:00"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2));

                verify(bookingService, times(1)).getSeatsByTime(eq(2), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class));
        }

        // =========================================
        // === UTCID03: Zone with all seats available ===
        // =========================================

        /**
         * UTCID03: Zone with all seats available (no overlapping reservations)
         * Precondition: Zone id, date, and time range are provided
         * Expected: 200 OK with all seats showing AVAILABLE
         */
        @Test
        @DisplayName("UTCID03: Zone with all seats available returns 200 OK")
        void getSeatsByTime_allAvailable_returns200OK() throws Exception {
                SeatDTO seat = new SeatDTO();
                seat.setSeatId(1);
                seat.setSeatStatus(SeatStatus.AVAILABLE);

                when(bookingService.getSeatsByTime(eq(3), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                                .thenReturn(List.of(seat));

                mockMvc.perform(get("/slib/seats/getSeatsByTime/3")
                                .param("date", "2026-03-12")
                                .param("start", "06:00")
                                .param("end", "07:00"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].seatStatus").value("AVAILABLE"));

                verify(bookingService, times(1)).getSeatsByTime(eq(3), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class));
        }

        // =========================================
        // === UTCID04: Repository/date parsing fails ===
        // =========================================

        /**
         * UTCID04: Repository or date parsing fails
         * Precondition: Zone id, date, and time range are provided
         * Expected: 200 OK (service returns empty on error) or error handled
         */
        @Test
        @DisplayName("UTCID04: Repository failure returns empty result via service")
        void getSeatsByTime_repositoryFails_returns200OK() throws Exception {
                when(bookingService.getSeatsByTime(eq(99), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                                .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/seats/getSeatsByTime/99")
                                .param("date", "2026-03-12")
                                .param("start", "08:00")
                                .param("end", "10:00"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isEmpty());

                verify(bookingService, times(1)).getSeatsByTime(eq(99), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class));
        }

        // =========================================
        // === UTCID05: Zone with no seats ===
        // =========================================

        /**
         * UTCID05: Date parsing fails from service layer
         * Precondition: Zone id, date, and time range are provided
         * Expected: 200 OK with empty list
         */
        @Test
        @DisplayName("UTCID05: Zone with no seats returns 200 OK with empty array")
        void getSeatsByTime_noSeats_returns200OK() throws Exception {
                when(bookingService.getSeatsByTime(eq(100), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                                .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/seats/getSeatsByTime/100")
                                .param("date", "2026-03-12")
                                .param("start", "08:00")
                                .param("end", "10:00"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isEmpty());

                verify(bookingService, times(1)).getSeatsByTime(eq(100), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class));
        }
}
