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
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.booking.BookingService;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-81: View actual seat end time
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-81: View actual seat end time - Unit Tests")
class FE81_ViewActualSeatEndTimeTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookingService bookingService;

        @MockBean
        private ReservationRepository reservationRepository;

        @MockBean
        private UserRepository userRepository;

        private static final UUID USER_ID = UUID.fromString("bbbb2222-2222-2222-2222-222222222222");

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD01: View booking history with actual end time - returns 200")
        void viewBookingHistory_withActualEndTime_returns200() throws Exception {
                User user = User.builder().id(USER_ID).email("student@fpt.edu.vn").build();
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(user));
                when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

                BookingHistoryResponse response = BookingHistoryResponse.builder()
                                .reservationId(UUID.randomUUID())
                                .status("COMPLETED")
                                .startTime(LocalDateTime.now().minusHours(2))
                                .endTime(LocalDateTime.now().minusHours(1))
                                .actualEndTime(LocalDateTime.now().minusMinutes(45))
                                .build();
                when(bookingService.getBookingHistory(USER_ID)).thenReturn(List.of(response));

                mockMvc.perform(get("/slib/bookings/user/{userId}", USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].actualEndTime").exists());
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD02: View booking history - no completed bookings returns empty")
        void viewBookingHistory_empty_returns200() throws Exception {
                User user = User.builder().id(USER_ID).email("student@fpt.edu.vn").build();
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(user));
                when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
                when(bookingService.getBookingHistory(USER_ID)).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/bookings/user/{userId}", USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD03: View booking with null actual end time (still active) - returns 200")
        void viewBookingHistory_nullActualEndTime_returns200() throws Exception {
                User user = User.builder().id(USER_ID).email("student@fpt.edu.vn").build();
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(user));
                when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

                BookingHistoryResponse response = BookingHistoryResponse.builder()
                                .reservationId(UUID.randomUUID())
                                .status("CONFIRMED")
                                .startTime(LocalDateTime.now().minusHours(1))
                                .endTime(LocalDateTime.now().plusHours(1))
                                .actualEndTime(null)
                                .build();
                when(bookingService.getBookingHistory(USER_ID)).thenReturn(List.of(response));

                mockMvc.perform(get("/slib/bookings/user/{userId}", USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].actualEndTime").doesNotExist());
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD04: Service error - returns 400")
        void viewBookingHistory_serviceError_returns400() throws Exception {
                User user = User.builder().id(USER_ID).email("student@fpt.edu.vn").build();
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(user));
                when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
                when(bookingService.getBookingHistory(USER_ID)).thenThrow(new RuntimeException("DB error"));

                mockMvc.perform(get("/slib/bookings/user/{userId}", USER_ID))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD05: View multiple bookings with different actual end times")
        void viewBookingHistory_multipleBookings_returns200() throws Exception {
                User user = User.builder().id(USER_ID).email("student@fpt.edu.vn").build();
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(user));
                when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

                BookingHistoryResponse r1 = BookingHistoryResponse.builder()
                                .reservationId(UUID.randomUUID()).status("COMPLETED")
                                .actualEndTime(LocalDateTime.now().minusHours(2)).build();
                BookingHistoryResponse r2 = BookingHistoryResponse.builder()
                                .reservationId(UUID.randomUUID()).status("COMPLETED")
                                .actualEndTime(LocalDateTime.now().minusHours(1)).build();
                when(bookingService.getBookingHistory(USER_ID)).thenReturn(List.of(r1, r2));

                mockMvc.perform(get("/slib/bookings/user/{userId}", USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2));
        }
}
