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

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-71: View Booking Details
 * Test Report: doc/Report/UnitTestReport/FE71_TestReport.md
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-71: View Booking Details - Unit Tests")
class FE71_ViewBookingDetailsTest {

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
        // === UTCID01: Valid upcoming booking exists - Normal ===
        // =========================================

        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID01: Get upcoming booking with valid userId returns 200 OK")
        void getUpcomingBooking_exists_returns200OK() throws Exception {
                UpcomingBookingResponse mockBooking = UpcomingBookingResponse.builder()
                                .reservationId(UUID.randomUUID())
                                .status("BOOKED")
                                .seatCode("A-01")
                                .build();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(bookingService.getUpcomingBooking(eq(TEST_USER_ID))).thenReturn(Optional.of(mockBooking));

                mockMvc.perform(get("/slib/bookings/upcoming/{userId}", TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("BOOKED"));

                verify(bookingService, times(1)).getUpcomingBooking(eq(TEST_USER_ID));
        }

        // =========================================
        // === UTCID02: Booking with full seat/zone info - Abnormal ===
        // =========================================

        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID02: Get upcoming booking with full details returns 200 OK")
        void getUpcomingBooking_fullDetails_returns200OK() throws Exception {
                UpcomingBookingResponse mockBooking = UpcomingBookingResponse.builder()
                                .reservationId(UUID.randomUUID())
                                .status("CONFIRMED")
                                .seatCode("B-05")
                                .build();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(bookingService.getUpcomingBooking(eq(TEST_USER_ID))).thenReturn(Optional.of(mockBooking));

                mockMvc.perform(get("/slib/bookings/upcoming/{userId}", TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CONFIRMED"));

                verify(bookingService, times(1)).getUpcomingBooking(eq(TEST_USER_ID));
        }

        // =========================================
        // === UTCID03: Booking in PROCESSING status - Abnormal ===
        // =========================================

        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID03: Get upcoming booking with PROCESSING status returns 200 OK")
        void getUpcomingBooking_processingStatus_returns200OK() throws Exception {
                UpcomingBookingResponse mockBooking = UpcomingBookingResponse.builder()
                                .reservationId(UUID.randomUUID())
                                .status("PROCESSING")
                                .build();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(bookingService.getUpcomingBooking(eq(TEST_USER_ID))).thenReturn(Optional.of(mockBooking));

                mockMvc.perform(get("/slib/bookings/upcoming/{userId}", TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("PROCESSING"));

                verify(bookingService, times(1)).getUpcomingBooking(eq(TEST_USER_ID));
        }

        // =========================================
        // === UTCID04: No upcoming booking - Abnormal ===
        // =========================================

        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID04: Get upcoming booking when none exists returns 204 No Content")
        void getUpcomingBooking_notExists_returns204NoContent() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(bookingService.getUpcomingBooking(eq(TEST_USER_ID))).thenReturn(Optional.empty());

                mockMvc.perform(get("/slib/bookings/upcoming/{userId}", TEST_USER_ID))
                                .andExpect(status().isNoContent());

                verify(bookingService, times(1)).getUpcomingBooking(eq(TEST_USER_ID));
        }

        // =========================================
        // === UTCID05: Invalid userId format - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get upcoming booking with invalid userId returns 400 Bad Request")
        void getUpcomingBooking_invalidUserId_returns400BadRequest() throws Exception {
                mockMvc.perform(get("/slib/bookings/upcoming/{userId}", "invalid-uuid"))
                                .andExpect(status().isBadRequest());
        }
}
