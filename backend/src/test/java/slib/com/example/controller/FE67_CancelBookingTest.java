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
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.ReservationRepository;
import slib.com.example.service.BookingService;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.booking.BookingController;

/**
 * Unit Tests for FE-67: Cancel Booking
 * Test Report: doc/Report/FE59_TestReport.md
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-67: Cancel Booking - Unit Tests")
class FE67_CancelBookingTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookingService bookingService;

        @MockBean
        private ReservationRepository reservationRepository;

        @Test
        @DisplayName("UTCD01: Cancel booking with valid UUID returns 200 OK")
        void cancelBooking_validRequest_returns200OK() throws Exception {
                UUID reservationId = UUID.randomUUID();
                ReservationEntity mockReservation = new ReservationEntity();
                mockReservation.setReservationId(reservationId);
                mockReservation.setStatus("CANCELLED");

                when(bookingService.cancelBooking(reservationId)).thenReturn(mockReservation);

                // The actual cancel endpoint is PUT /slib/bookings/cancel/{reservationId}
                mockMvc.perform(put("/slib/bookings/cancel/" + reservationId))
                        .andExpect(status().isOk());

                verify(bookingService, times(1)).cancelBooking(reservationId);
        }

        @Test
        @DisplayName("UTCD02: Cancel booking with non-existing reservation returns 400")
        void cancelBooking_nonExisting_returns400() throws Exception {
                UUID reservationId = UUID.randomUUID();
                when(bookingService.cancelBooking(reservationId))
                        .thenThrow(new RuntimeException("Reservation not found"));

                mockMvc.perform(put("/slib/bookings/cancel/" + reservationId))
                        .andExpect(status().isBadRequest());

                verify(bookingService, times(1)).cancelBooking(reservationId);
        }
}
