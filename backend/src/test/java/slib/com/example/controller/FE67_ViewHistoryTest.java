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
import slib.com.example.dto.booking.BookingHistoryResponse;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.booking.BookingService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-67: View history of booking
 * Test Report: doc/Report/UnitTestReport/FE66_TestReport.md
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-67: View history of booking - Unit Tests")
class FE67_ViewHistoryTest {

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
        // === UTCID01: User with booking history ===
        // =========================================

        /**
         * UTCID01: User has booking history with zone/area enrichment
         * Precondition: User id is provided
         * Expected: 200 OK with booking list
         */
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID01: User with booking history returns 200 OK")
        void getBookingHistory_hasHistory_returns200OK() throws Exception {
                BookingHistoryResponse booking1 = BookingHistoryResponse.builder()
                                .reservationId(UUID.randomUUID())
                                .status("COMPLETED")
                                .seatCode("A-01")
                                .zoneName("Zone A")
                                .areaName("Tang 1")
                                .startTime(LocalDateTime.of(2026, 3, 10, 8, 0))
                                .endTime(LocalDateTime.of(2026, 3, 10, 10, 0))
                                .build();
                BookingHistoryResponse booking2 = BookingHistoryResponse.builder()
                                .reservationId(UUID.randomUUID())
                                .status("CANCELLED")
                                .seatCode("B-03")
                                .zoneName("Zone B")
                                .areaName("Tang 2")
                                .build();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(bookingService.getBookingHistory(TEST_USER_ID)).thenReturn(List.of(booking1, booking2));

                mockMvc.perform(get("/slib/bookings/user/" + TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].status").value("COMPLETED"));

                verify(bookingService, times(1)).getBookingHistory(TEST_USER_ID);
        }

        // =========================================
        // === UTCID02: User with mixed booking statuses ===
        // =========================================

        /**
         * UTCID02: User with mixed booking statuses (COMPLETED, CANCELLED, EXPIRED)
         * Precondition: User id is provided
         * Expected: 200 OK with ordered booking list
         */
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID02: User with mixed statuses returns 200 OK")
        void getBookingHistory_mixedStatuses_returns200OK() throws Exception {
                BookingHistoryResponse booking = BookingHistoryResponse.builder()
                                .reservationId(UUID.randomUUID())
                                .status("EXPIRED")
                                .seatCode("C-10")
                                .build();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(bookingService.getBookingHistory(TEST_USER_ID)).thenReturn(List.of(booking));

                mockMvc.perform(get("/slib/bookings/user/" + TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].status").value("EXPIRED"));

                verify(bookingService, times(1)).getBookingHistory(TEST_USER_ID);
        }

        // =========================================
        // === UTCID03: User with single booking ===
        // =========================================

        /**
         * UTCID03: User with a single booking record
         * Precondition: User id is provided
         * Expected: 200 OK with one record
         */
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID03: User with single booking returns 200 OK")
        void getBookingHistory_singleBooking_returns200OK() throws Exception {
                BookingHistoryResponse booking = BookingHistoryResponse.builder()
                                .reservationId(UUID.randomUUID())
                                .status("BOOKED")
                                .seatCode("D-01")
                                .build();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(bookingService.getBookingHistory(TEST_USER_ID)).thenReturn(List.of(booking));

                mockMvc.perform(get("/slib/bookings/user/" + TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1));

                verify(bookingService, times(1)).getBookingHistory(TEST_USER_ID);
        }

        // =========================================
        // === UTCID04: User with empty history ===
        // =========================================

        /**
         * UTCID04: User has no booking history (empty list)
         * Precondition: User id is provided
         * Expected: 200 OK with empty list
         */
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID04: User with no history returns 200 OK with empty list")
        void getBookingHistory_emptyHistory_returns200OK() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(bookingService.getBookingHistory(TEST_USER_ID)).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/bookings/user/" + TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isEmpty());

                verify(bookingService, times(1)).getBookingHistory(TEST_USER_ID);
        }

        // =========================================
        // === UTCID05: Repository failure ===
        // =========================================

        /**
         * UTCID05: Repository failure is propagated
         * Precondition: User id is provided
         * Expected: 400 Bad Request
         */
        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID05: Repository failure returns 400 Bad Request")
        void getBookingHistory_repositoryFailure_returns400() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID)));
                when(bookingService.getBookingHistory(TEST_USER_ID))
                                .thenThrow(new RuntimeException("Database connection error"));

                mockMvc.perform(get("/slib/bookings/user/" + TEST_USER_ID))
                                .andExpect(status().isBadRequest());

                verify(bookingService, times(1)).getBookingHistory(TEST_USER_ID);
        }
}
