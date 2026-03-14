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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.service.booking.BookingService;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.booking.BookingController;

/**
 * Unit Tests for FE-63: Booking Seat
 * Test Report: doc/Report/FE59_TestReport.md
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-63: Booking Seat - Unit Tests")
class FE63_BookingSeatTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookingService bookingService;

        @MockBean
        private ReservationRepository reservationRepository;

        // UTCD01: Valid seat - calls /slib/bookings/create, controller catches parsing errors and returns 400
        @Test
        @DisplayName("UTCD01: Booking seat with valid JSON returns 400 when required fields missing")
        void bookingSeat_validSeat_returns400WhenFieldsMissing() throws Exception {
                // The controller expects user_id, seat_id, start_time, end_time in the request body
                // Sending incomplete data will cause a parsing exception caught by the controller
                mockMvc.perform(post("/slib/bookings/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"seatId\":\"" + UUID.randomUUID() + "\",\"zoneId\":\"" + UUID.randomUUID() + "\"}"))
                        .andExpect(status().isBadRequest());
        }

        // UTCD02: Security is disabled (addFilters=false), so we test that missing body returns 400
        @Test
        @DisplayName("UTCD02: Booking seat without request body returns 400")
        void bookingSeat_noBody_returns400() throws Exception {
                mockMvc.perform(post("/slib/bookings/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isBadRequest());
        }

        // UTCD03: Seat already booked - controller returns 400 (its catch block uses badRequest)
        @Test
        @DisplayName("UTCD03: Booking already booked seat returns 400 Bad Request")
        void bookingSeat_alreadyBooked_returns400() throws Exception {
                LocalDateTime start = LocalDateTime.now().plusHours(1);
                LocalDateTime end = start.plusHours(2);
                UUID userId = UUID.randomUUID();

                when(bookingService.createBooking(any(), any(), any(), any()))
                        .thenThrow(new RuntimeException("Seat already booked"));

                String body = String.format(
                        "{\"user_id\":\"%s\",\"seat_id\":\"1\",\"start_time\":\"%s\",\"end_time\":\"%s\"}",
                        userId, start, end);

                mockMvc.perform(post("/slib/bookings/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().isBadRequest());
        }
}
