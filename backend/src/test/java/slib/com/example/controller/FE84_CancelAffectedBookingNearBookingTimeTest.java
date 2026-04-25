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
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.booking.BookingService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-84: Cancel affected booking near booking time - Unit Tests")
class FE84_CancelAffectedBookingNearBookingTimeTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RESERVATION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String EMAIL = "student@fpt.edu.vn";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private ReservationRepository reservationRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = EMAIL, roles = "STUDENT")
    @DisplayName("UTCID01: Cancel layout-affected booking near booking time returns 200 OK")
    void cancelAffectedBookingNearStartTime_returns200() throws Exception {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setRole(Role.STUDENT);

        ReservationEntity reservation = buildReservation(user, "BOOKED");
        reservation.setLayoutChanged(true);

        ReservationEntity cancelled = buildReservation(user, "CANCELLED");
        cancelled.setLayoutChanged(true);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(bookingService.cancelBooking(RESERVATION_ID, USER_ID, false, null)).thenReturn(cancelled);

        mockMvc.perform(put("/slib/bookings/cancel/{reservationId}", RESERVATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(RESERVATION_ID.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "STUDENT")
    @DisplayName("UTCID02: Cancel affected booking of another user returns 400 Bad Request")
    void cancelAffectedBookingForAnotherUser_returns400() throws Exception {
        User owner = new User();
        owner.setId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        owner.setEmail("other@fpt.edu.vn");
        owner.setRole(Role.STUDENT);

        User currentUser = new User();
        currentUser.setId(USER_ID);
        currentUser.setEmail(EMAIL);
        currentUser.setRole(Role.STUDENT);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(currentUser));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(buildReservation(owner, "BOOKED")));

        mockMvc.perform(put("/slib/bookings/cancel/{reservationId}", RESERVATION_ID))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).cancelBooking(eq(RESERVATION_ID), eq(USER_ID), eq(false), eq(null));
    }

    private ReservationEntity buildReservation(User user, String status) {
        ReservationEntity reservation = new ReservationEntity();
        reservation.setReservationId(RESERVATION_ID);
        reservation.setUser(user);
        reservation.setStatus(status);
        reservation.setStartTime(LocalDateTime.of(2026, 4, 21, 8, 0));
        reservation.setEndTime(LocalDateTime.of(2026, 4, 21, 10, 0));
        return reservation;
    }
}
