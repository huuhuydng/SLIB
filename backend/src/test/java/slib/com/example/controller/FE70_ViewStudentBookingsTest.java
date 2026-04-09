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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-70: View list of user bookings
 * Test Report: doc/Report/UnitTestReport/FE69_TestReport.md
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-70: View list of user bookings - Unit Tests")
class FE70_ViewStudentBookingsTest {

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

        // =========================================
        // === UTCID01: Valid user with bookings - Normal ===
        // =========================================

        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID01: Get bookings for user with existing bookings returns 200 OK")
        void getBookingsByUser_withBookings_returns200OK() throws Exception {
                BookingHistoryResponse r1 = BookingHistoryResponse.builder()
                                .reservationId(UUID.randomUUID()).status("BOOKED").build();
                BookingHistoryResponse r2 = BookingHistoryResponse.builder()
                                .reservationId(UUID.randomUUID()).status("COMPLETED").build();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                when(bookingService.getBookingHistory(eq(TEST_USER_ID))).thenReturn(List.of(r1, r2));

                mockMvc.perform(get("/slib/bookings/user/{userId}", TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2));

                verify(bookingService, times(1)).getBookingHistory(eq(TEST_USER_ID));
        }

        // =========================================
        // === UTCID02: User with single booking - Abnormal ===
        // =========================================

        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID02: Get bookings for user with single booking returns 200 OK")
        void getBookingsByUser_singleBooking_returns200OK() throws Exception {
                BookingHistoryResponse r1 = BookingHistoryResponse.builder()
                                .reservationId(UUID.randomUUID()).status("BOOKED").build();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                when(bookingService.getBookingHistory(eq(TEST_USER_ID))).thenReturn(List.of(r1));

                mockMvc.perform(get("/slib/bookings/user/{userId}", TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(1));

                verify(bookingService, times(1)).getBookingHistory(eq(TEST_USER_ID));
        }

        // =========================================
        // === UTCID03: User with various status bookings - Abnormal ===
        // =========================================

        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID03: Get bookings with mixed statuses returns 200 OK")
        void getBookingsByUser_mixedStatuses_returns200OK() throws Exception {
                BookingHistoryResponse r1 = BookingHistoryResponse.builder()
                                .reservationId(UUID.randomUUID()).status("CANCELLED").build();
                BookingHistoryResponse r2 = BookingHistoryResponse.builder()
                                .reservationId(UUID.randomUUID()).status("EXPIRED").build();

                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                when(bookingService.getBookingHistory(eq(TEST_USER_ID))).thenReturn(List.of(r1, r2));

                mockMvc.perform(get("/slib/bookings/user/{userId}", TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());

                verify(bookingService, times(1)).getBookingHistory(eq(TEST_USER_ID));
        }

        // =========================================
        // === UTCID04: User with no bookings - Abnormal ===
        // =========================================

        @Test
        @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID04: Get bookings for user with no bookings returns 200 OK with empty list")
        void getBookingsByUser_noBookings_returns200OKEmptyList() throws Exception {
                when(userRepository.findByEmail(TEST_EMAIL))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                when(userRepository.findById(TEST_USER_ID))
                                .thenReturn(java.util.Optional.of(buildCurrentUser(TEST_USER_ID, TEST_EMAIL)));
                when(bookingService.getBookingHistory(eq(TEST_USER_ID))).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/bookings/user/{userId}", TEST_USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(bookingService, times(1)).getBookingHistory(eq(TEST_USER_ID));
        }

        // =========================================
        // === UTCID05: Invalid userId format - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get bookings with invalid userId format returns 400 Bad Request")
        void getBookingsByUser_invalidUserId_returns400BadRequest() throws Exception {
                mockMvc.perform(get("/slib/bookings/user/{userId}", "invalid-uuid"))
                                .andExpect(status().isBadRequest());
        }
}
