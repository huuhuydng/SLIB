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
import slib.com.example.controller.booking.BookingController;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.ReservationRepository;
import slib.com.example.service.BookingService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-72: Cancel Invalid Booking
 * Test Report: doc/Report/UnitTestReport/FE72_TestReport.md
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-72: Cancel Invalid Booking - Unit Tests")
class FE72_CancelInvalidBookingTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookingService bookingService;

        @MockBean
        private ReservationRepository reservationRepository;

        // =========================================
        // === UTCID01: Valid cancellation - Normal ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Cancel booking with valid reservationId returns 200 OK")
        void cancelBooking_validReservation_returns200OK() throws Exception {
                UUID reservationId = UUID.randomUUID();
                ReservationEntity mockReservation = new ReservationEntity();
                mockReservation.setReservationId(reservationId);
                mockReservation.setStatus("CANCELLED");

                when(bookingService.cancelBooking(eq(reservationId))).thenReturn(mockReservation);

                mockMvc.perform(put("/slib/bookings/cancel/{reservationId}", reservationId))
                                .andExpect(status().isOk());

                verify(bookingService, times(1)).cancelBooking(eq(reservationId));
        }

        // =========================================
        // === UTCID02: Already cancelled booking - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Cancel already cancelled booking returns 400 Bad Request")
        void cancelBooking_alreadyCancelled_returns400BadRequest() throws Exception {
                UUID reservationId = UUID.randomUUID();

                when(bookingService.cancelBooking(eq(reservationId)))
                                .thenThrow(new RuntimeException("Booking da bi huy truoc do"));

                mockMvc.perform(put("/slib/bookings/cancel/{reservationId}", reservationId))
                                .andExpect(status().isBadRequest());

                verify(bookingService, times(1)).cancelBooking(eq(reservationId));
        }

        // =========================================
        // === UTCID03: Booking in non-cancellable status - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Cancel booking in COMPLETED status returns 400 Bad Request")
        void cancelBooking_completedStatus_returns400BadRequest() throws Exception {
                UUID reservationId = UUID.randomUUID();

                when(bookingService.cancelBooking(eq(reservationId)))
                                .thenThrow(new RuntimeException("Khong the huy booking da hoan thanh"));

                mockMvc.perform(put("/slib/bookings/cancel/{reservationId}", reservationId))
                                .andExpect(status().isBadRequest());

                verify(bookingService, times(1)).cancelBooking(eq(reservationId));
        }

        // =========================================
        // === UTCID04: Non-existent reservation - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Cancel non-existent booking returns 400 Bad Request")
        void cancelBooking_notFound_returns400BadRequest() throws Exception {
                UUID reservationId = UUID.randomUUID();

                when(bookingService.cancelBooking(eq(reservationId)))
                                .thenThrow(new RuntimeException("Reservation not found"));

                mockMvc.perform(put("/slib/bookings/cancel/{reservationId}", reservationId))
                                .andExpect(status().isBadRequest());

                verify(bookingService, times(1)).cancelBooking(eq(reservationId));
        }

        // =========================================
        // === UTCID05: Invalid reservationId format - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Cancel booking with invalid UUID format returns 400 Bad Request")
        void cancelBooking_invalidUUID_returns400BadRequest() throws Exception {
                mockMvc.perform(put("/slib/bookings/cancel/{reservationId}", "invalid-uuid"))
                                .andExpect(status().isBadRequest());
        }
}
