package slib.com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import slib.com.example.controller.booking.BookingController;
import slib.com.example.dto.booking.ChangeSeatRequest;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.booking.BookingService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
@DisplayName("FE-83: Change seat for affected booking - Unit Tests")
class FE83_ChangeSeatForAffectedBookingTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RESERVATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String EMAIL = "student@fpt.edu.vn";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private ReservationRepository reservationRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = EMAIL, roles = "STUDENT")
    @DisplayName("UTCID01: Change seat for layout-affected booking returns 200 OK")
    void changeSeat_layoutAffectedBooking_returns200() throws Exception {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setRole(Role.STUDENT);

        ReservationEntity existing = buildReservation(4, user, "BOOKED");
        ReservationEntity updated = buildReservation(9, user, "BOOKED");

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(existing));
        when(bookingService.changeSeatForLayoutAffectedReservation(eq(RESERVATION_ID), eq(USER_ID), eq(9)))
                .thenReturn(updated);

        mockMvc.perform(put("/slib/bookings/change-seat/{reservationId}", RESERVATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeSeatRequest(9))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatId").value(9))
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "STUDENT")
    @DisplayName("UTCID02: Change seat without selecting new seat returns 400 Bad Request")
    void changeSeat_missingSeatId_returns400() throws Exception {
        mockMvc.perform(put("/slib/bookings/change-seat/{reservationId}", RESERVATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeSeatRequest(null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Vui lòng chọn ghế mới"));

        verify(bookingService, never()).changeSeatForLayoutAffectedReservation(eq(RESERVATION_ID), eq(USER_ID), anyInt());
    }

    private ReservationEntity buildReservation(int seatId, User user, String status) {
        SeatEntity seat = new SeatEntity();
        seat.setSeatId(seatId);

        ReservationEntity reservation = new ReservationEntity();
        reservation.setReservationId(RESERVATION_ID);
        reservation.setUser(user);
        reservation.setSeat(seat);
        reservation.setStatus(status);
        reservation.setStartTime(LocalDateTime.of(2026, 4, 21, 8, 0));
        reservation.setEndTime(LocalDateTime.of(2026, 4, 21, 10, 0));
        return reservation;
    }
}
