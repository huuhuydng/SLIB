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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.BookingService;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-62: Booking Seat
 * Test Report: doc/Report/FE59_TestReport.md
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-62: Booking Seat - Unit Tests")
class FE62_BookingSeatTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookingService bookingService;

        // UTCD01: Valid seat - Success
        @Test
        @DisplayName("UTCD01: Booking seat with valid data returns 200 OK")
        void bookingSeat_validSeat_returns200OK() throws Exception {
                mockMvc.perform(post("/slib/bookings")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"seatId\":\"" + UUID.randomUUID() + "\",\"zoneId\":\"" + UUID.randomUUID() + "\"}"))
                        .andExpect(status().isOk());
        }

        // UTCD02: No token - 401
        @Test
        @DisplayName("UTCD02: Booking seat without token returns 401 Unauthorized")
        void bookingSeat_noToken_returns401() throws Exception {
                mockMvc.perform(post("/slib/bookings"))
                        .andExpect(status().isUnauthorized());
        }

        // UTCD03: Seat already booked - 409
        @Test
        @DisplayName("UTCD03: Booking already booked seat returns 409 Conflict")
        void bookingSeat_alreadyBooked_returns409() throws Exception {
                when(bookingService.createBooking(any(), any(), any()))
                        .thenThrow(new RuntimeException("Seat already booked"));

                mockMvc.perform(post("/slib/bookings")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"seatId\":\"" + UUID.randomUUID() + "\"}"))
                        .andExpect(status().isConflict());
        }
}
