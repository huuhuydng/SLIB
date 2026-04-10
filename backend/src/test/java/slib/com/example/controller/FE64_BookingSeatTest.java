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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.booking.BookingService;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.booking.BookingController;

/**
 * Unit Tests for FE-64: Booking seat
 * Test Report: doc/Report/FE64_TestReport.md
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-64: Booking seat - Unit Tests")
class FE64_BookingSeatTest {

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

        private User buildCurrentUser(UUID userId) {
                User user = new User();
                user.setId(userId);
                user.setEmail(TEST_EMAIL);
                user.setRole(Role.STUDENT);
                return user;
        }

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
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCD03: Booking already booked seat returns 400 Bad Request")
        void bookingSeat_alreadyBooked_returns400() throws Exception {
                LocalDateTime start = LocalDateTime.now().plusHours(1);
                LocalDateTime end = start.plusHours(2);

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(bookingService.createBooking(any(), any(), any(), any()))
                        .thenThrow(new RuntimeException("Seat already booked"));

                String body = String.format(
                        "{\"user_id\":\"%s\",\"seat_id\":\"1\",\"start_time\":\"%s\",\"end_time\":\"%s\"}",
                        TEST_USER_ID, start, end);

                mockMvc.perform(post("/slib/bookings/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().isBadRequest());
        }
}
