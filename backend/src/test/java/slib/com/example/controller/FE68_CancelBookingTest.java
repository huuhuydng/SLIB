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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.booking.BookingService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.booking.BookingController;

/**
 * Unit Tests for FE-68: Cancel booking
 * Test Report: doc/Report/FE68_TestReport.md
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-68: Cancel booking - Unit Tests")
class FE68_CancelBookingTest {

        private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
        private static final String TEST_EMAIL = "student@fpt.edu.vn";

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookingService bookingService;

        @MockBean
        private ReservationRepository reservationRepository;

        @MockBean
        private UserRepository userRepository;

        private User buildCurrentUser(UUID userId, String email) {
                User user = new User();
                user.setId(userId);
                user.setEmail(email);
                user.setRole(Role.STUDENT);
                return user;
        }

        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCD01: Cancel booking with valid UUID returns 200 OK")
        void cancelBooking_validRequest_returns200OK() throws Exception {
                UUID reservationId = UUID.randomUUID();
                User bookingUser = buildCurrentUser(TEST_USER_ID, TEST_EMAIL);
                ReservationEntity mockReservation = new ReservationEntity();
                mockReservation.setReservationId(reservationId);
                mockReservation.setStatus("CANCELLED");
                mockReservation.setUser(bookingUser);

                when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(bookingUser));
                when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(bookingUser));
                when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(mockReservation));
                when(bookingService.cancelBooking(reservationId)).thenReturn(mockReservation);

                // The actual cancel endpoint is PUT /slib/bookings/cancel/{reservationId}
                mockMvc.perform(put("/slib/bookings/cancel/" + reservationId))
                        .andExpect(status().isOk());

                verify(bookingService, times(1)).cancelBooking(reservationId);
        }

        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCD02: Cancel booking with non-existing reservation returns 400")
        void cancelBooking_nonExisting_returns400() throws Exception {
                UUID reservationId = UUID.randomUUID();
                when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

                mockMvc.perform(put("/slib/bookings/cancel/" + reservationId))
                        .andExpect(status().isBadRequest());

                verify(bookingService, never()).cancelBooking(reservationId);
        }
}
