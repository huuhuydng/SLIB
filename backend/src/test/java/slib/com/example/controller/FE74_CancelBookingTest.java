package slib.com.example.controller;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-74: Cancel booking - Unit Tests")
class FE74_CancelBookingTest {

    private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String TEST_EMAIL = "student@fpt.edu.vn";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private ReservationRepository reservationRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
    @DisplayName("UTCID01: Cancel own booking with valid reservationId")
    void cancelOwnBookingWithValidReservationId() throws Exception {
        UUID reservationId = UUID.randomUUID();
        User currentUser = student(TEST_USER_ID, TEST_EMAIL);
        ReservationEntity reservation = reservation(reservationId, currentUser, "CANCELLED");

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(currentUser));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(bookingService.cancelBooking(reservationId, TEST_USER_ID, false, null)).thenReturn(reservation);

        mockMvc.perform(put("/slib/bookings/cancel/" + reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(reservationId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID.toString()));

        verify(bookingService, times(1)).cancelBooking(reservationId, TEST_USER_ID, false, null);
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
    @DisplayName("UTCID02: Cancel booking with non-existent reservationId")
    void cancelBookingWithNonExistentReservationId() throws Exception {
        UUID reservationId = UUID.randomUUID();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        mockMvc.perform(put("/slib/bookings/cancel/" + reservationId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Reservation not found"));

        verify(bookingService, never()).cancelBooking(reservationId, TEST_USER_ID, false, null);
    }

    @Test
    @DisplayName("UTCID03: Cancel booking with invalid reservationId format")
    void cancelBookingWithInvalidReservationIdFormat() throws Exception {
        mockMvc.perform(put("/slib/bookings/cancel/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
    @DisplayName("UTCID04: Cancel booking for another user's reservation")
    void cancelBookingForAnotherUsersReservation() throws Exception {
        UUID reservationId = UUID.randomUUID();
        User currentUser = student(TEST_USER_ID, TEST_EMAIL);
        User otherUser = student(OTHER_USER_ID, "other@fpt.edu.vn");
        ReservationEntity reservation = reservation(reservationId, otherUser, "BOOKED");

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(currentUser));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        mockMvc.perform(put("/slib/bookings/cancel/" + reservationId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Bạn không có quyền thao tác trên dữ liệu đặt chỗ của người khác."));

        verify(bookingService, never()).cancelBooking(reservationId, TEST_USER_ID, false, null);
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
    @DisplayName("UTCID05: Cancel booking when current user does not exist")
    void cancelBookingWhenCurrentUserDoesNotExist() throws Exception {
        UUID reservationId = UUID.randomUUID();
        User owner = student(TEST_USER_ID, TEST_EMAIL);
        ReservationEntity reservation = reservation(reservationId, owner, "BOOKED");

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        mockMvc.perform(put("/slib/bookings/cancel/" + reservationId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User not found"));

        verify(bookingService, never()).cancelBooking(reservationId, TEST_USER_ID, false, null);
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = "STUDENT")
    @DisplayName("UTCID06: Cancel booking when booking service throws runtime exception")
    void cancelBookingWhenBookingServiceThrowsRuntimeException() throws Exception {
        UUID reservationId = UUID.randomUUID();
        User currentUser = student(TEST_USER_ID, TEST_EMAIL);
        ReservationEntity reservation = reservation(reservationId, currentUser, "BOOKED");

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(currentUser));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(bookingService.cancelBooking(reservationId, TEST_USER_ID, false, null))
                .thenThrow(new RuntimeException("Reservation already cancelled"));

        mockMvc.perform(put("/slib/bookings/cancel/" + reservationId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Reservation already cancelled"));

        verify(bookingService).cancelBooking(reservationId, TEST_USER_ID, false, null);
    }

    private User student(UUID userId, String email) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setRole(Role.STUDENT);
        user.setFullName("Student");
        return user;
    }

    private ReservationEntity reservation(UUID reservationId, User user, String status) {
        ReservationEntity reservation = new ReservationEntity();
        reservation.setReservationId(reservationId);
        reservation.setUser(user);
        reservation.setStatus(status);
        reservation.setStartTime(LocalDateTime.of(2026, 4, 10, 8, 0));
        reservation.setEndTime(LocalDateTime.of(2026, 4, 10, 10, 0));
        return reservation;
    }
}
