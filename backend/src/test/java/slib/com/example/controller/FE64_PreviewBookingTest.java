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
import slib.com.example.controller.booking.BookingController;
import slib.com.example.dto.booking.UpcomingBookingResponse;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.booking.BookingService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-64: Preview Booking
 * Test Report: doc/Report/UnitTestReport/FE64_TestReport.md
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-64: Preview Booking - Unit Tests")
class FE64_PreviewBookingTest {

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

        // =========================================
        // === UTCID01: User has active upcoming booking ===
        // =========================================

        /**
         * UTCID01: User has active or upcoming booking
         * Precondition: User id is provided
         * Expected: 200 OK with booking data
         */
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID01: User with upcoming booking returns 200 OK")
        void getUpcomingBooking_hasBooking_returns200OK() throws Exception {
                UpcomingBookingResponse booking = UpcomingBookingResponse.builder()
                                .reservationId(UUID.randomUUID())
                                .status("BOOKED")
                                .seatCode("A-01")
                                .zoneName("Zone A")
                                .areaName("Tang 1")
                                .startTime(LocalDateTime.of(2026, 3, 12, 8, 0))
                                .endTime(LocalDateTime.of(2026, 3, 12, 10, 0))
                                .build();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(bookingService.getUpcomingBooking(TEST_USER_ID)).thenReturn(Optional.of(booking));

                mockMvc.perform(get("/slib/bookings/upcoming/" + TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("BOOKED"))
                                .andExpect(jsonPath("$.seatCode").value("A-01"));

                verify(bookingService, times(1)).getUpcomingBooking(TEST_USER_ID);
        }

        // =========================================
        // === UTCID02: User has CONFIRMED booking ===
        // =========================================

        /**
         * UTCID02: User has CONFIRMED booking (active session)
         * Precondition: User id is provided
         * Expected: 200 OK with confirmed booking
         */
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID02: User with confirmed booking returns 200 OK")
        void getUpcomingBooking_confirmedBooking_returns200OK() throws Exception {
                UpcomingBookingResponse booking = UpcomingBookingResponse.builder()
                                .reservationId(UUID.randomUUID())
                                .status("CONFIRMED")
                                .seatCode("B-05")
                                .zoneName("Zone B")
                                .build();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(bookingService.getUpcomingBooking(TEST_USER_ID)).thenReturn(Optional.of(booking));

                mockMvc.perform(get("/slib/bookings/upcoming/" + TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CONFIRMED"));

                verify(bookingService, times(1)).getUpcomingBooking(TEST_USER_ID);
        }

        // =========================================
        // === UTCID03: User has no upcoming booking ===
        // =========================================

        /**
         * UTCID03: User has no upcoming booking
         * Precondition: User id is provided
         * Expected: 204 No Content
         */
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID03: User with no upcoming booking returns 204 No Content")
        void getUpcomingBooking_noBooking_returns204() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(bookingService.getUpcomingBooking(TEST_USER_ID)).thenReturn(Optional.empty());

                mockMvc.perform(get("/slib/bookings/upcoming/" + TEST_USER_ID))
                                .andExpect(status().isNoContent());

                verify(bookingService, times(1)).getUpcomingBooking(TEST_USER_ID);
        }

        // =========================================
        // === UTCID04: Query fails - exception from service ===
        // =========================================

        /**
         * UTCID04: Query failure is propagated/handled by controller path
         * Precondition: User id is provided
         * Expected: 400 Bad Request
         */
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID04: Query failure returns 400 Bad Request")
        void getUpcomingBooking_queryFails_returns400() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(bookingService.getUpcomingBooking(TEST_USER_ID))
                                .thenThrow(new RuntimeException("Database query timeout"));

                mockMvc.perform(get("/slib/bookings/upcoming/" + TEST_USER_ID))
                                .andExpect(status().isBadRequest());

                verify(bookingService, times(1)).getUpcomingBooking(TEST_USER_ID);
        }

        // =========================================
        // === UTCID05: Service throws unexpected error ===
        // =========================================

        /**
         * UTCID05: Service throws unexpected error
         * Precondition: User id is provided
         * Expected: 400 Bad Request (controller catches all exceptions)
         */
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID05: Unexpected service error returns 400 Bad Request")
        void getUpcomingBooking_unexpectedError_returns400() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(bookingService.getUpcomingBooking(TEST_USER_ID))
                                .thenThrow(new RuntimeException("Unexpected error in booking service"));

                mockMvc.perform(get("/slib/bookings/upcoming/" + TEST_USER_ID))
                                .andExpect(status().isBadRequest());

                verify(bookingService, times(1)).getUpcomingBooking(TEST_USER_ID);
        }
}
