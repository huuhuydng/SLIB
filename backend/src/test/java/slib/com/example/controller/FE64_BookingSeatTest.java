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
import slib.com.example.controller.booking.BookingController;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit Tests for FE-64: Booking seat
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-64: Booking seat - Unit Tests")
class FE64_BookingSeatTest {

        private static final UUID CURRENT_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
        private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
        private static final String STUDENT_EMAIL = "student@fpt.edu.vn";
        private static final String LIBRARIAN_EMAIL = "librarian@fpt.edu.vn";

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookingService bookingService;

        @MockBean
        private ReservationRepository reservationRepository;

        @MockBean
        private UserRepository userRepository;

        private User buildUser(UUID userId, String email, Role role) {
                User user = new User();
                user.setId(userId);
                user.setEmail(email);
                user.setRole(role);
                return user;
        }

        private ReservationEntity buildReservation(UUID userId, int seatId, LocalDateTime start, LocalDateTime end, String status) {
                User user = new User();
                user.setId(userId);

                SeatEntity seat = new SeatEntity();
                seat.setSeatId(seatId);

                ReservationEntity reservation = new ReservationEntity();
                reservation.setReservationId(UUID.randomUUID());
                reservation.setUser(user);
                reservation.setSeat(seat);
                reservation.setStartTime(start);
                reservation.setEndTime(end);
                reservation.setConfirmedAt(null);
                reservation.setStatus(status);
                return reservation;
        }

        @Test
        @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID01: Create booking for current student with valid payload")
        void createBooking_forCurrentStudentWithValidPayload() throws Exception {
                LocalDateTime start = LocalDateTime.of(2026, 4, 10, 8, 0);
                LocalDateTime end = start.plusHours(2);
                ReservationEntity reservation = buildReservation(CURRENT_USER_ID, 12, start, end, "BOOKED");

                when(userRepository.findByEmail(STUDENT_EMAIL))
                                .thenReturn(Optional.of(buildUser(CURRENT_USER_ID, STUDENT_EMAIL, Role.STUDENT)));
                when(userRepository.findById(CURRENT_USER_ID))
                                .thenReturn(Optional.of(buildUser(CURRENT_USER_ID, STUDENT_EMAIL, Role.STUDENT)));
                when(bookingService.createBooking(eq(CURRENT_USER_ID), eq(12), eq(start), eq(end)))
                                .thenReturn(reservation);

                String body = """
                                {
                                  "user_id": "11111111-1111-1111-1111-111111111111",
                                  "seat_id": "12",
                                  "start_time": "2026-04-10T08:00:00",
                                  "end_time": "2026-04-10T10:00:00"
                                }
                                """;

                mockMvc.perform(post("/slib/bookings/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(CURRENT_USER_ID.toString()))
                                .andExpect(jsonPath("$.seatId").value(12))
                                .andExpect(jsonPath("$.status").value("BOOKED"));
        }

        @Test
        @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID02: Create booking response includes reservation time range")
        void createBooking_responseIncludesReservationTimeRange() throws Exception {
                LocalDateTime start = LocalDateTime.of(2026, 4, 10, 13, 30);
                LocalDateTime end = start.plusHours(3);
                ReservationEntity reservation = buildReservation(CURRENT_USER_ID, 15, start, end, "PROCESSING");

                when(userRepository.findByEmail(STUDENT_EMAIL))
                                .thenReturn(Optional.of(buildUser(CURRENT_USER_ID, STUDENT_EMAIL, Role.STUDENT)));
                when(userRepository.findById(CURRENT_USER_ID))
                                .thenReturn(Optional.of(buildUser(CURRENT_USER_ID, STUDENT_EMAIL, Role.STUDENT)));
                when(bookingService.createBooking(eq(CURRENT_USER_ID), eq(15), eq(start), eq(end)))
                                .thenReturn(reservation);

                String body = """
                                {
                                  "user_id": "11111111-1111-1111-1111-111111111111",
                                  "seat_id": "15",
                                  "start_time": "2026-04-10T13:30:00",
                                  "end_time": "2026-04-10T16:30:00"
                                }
                                """;

                mockMvc.perform(post("/slib/bookings/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.startTime").value("2026-04-10T13:30:00"))
                                .andExpect(jsonPath("$.endTime").value("2026-04-10T16:30:00"))
                                .andExpect(jsonPath("$.status").value("PROCESSING"));
        }

        @Test
        @WithMockUser(username = LIBRARIAN_EMAIL, roles = "LIBRARIAN")
        @DisplayName("UTCID03: Create booking for another user as librarian")
        void createBooking_forAnotherUserAsLibrarian() throws Exception {
                LocalDateTime start = LocalDateTime.of(2026, 4, 11, 9, 0);
                LocalDateTime end = start.plusHours(2);
                ReservationEntity reservation = buildReservation(OTHER_USER_ID, 22, start, end, "BOOKED");

                when(userRepository.findByEmail(LIBRARIAN_EMAIL))
                                .thenReturn(Optional.of(buildUser(CURRENT_USER_ID, LIBRARIAN_EMAIL, Role.LIBRARIAN)));
                when(userRepository.findById(CURRENT_USER_ID))
                                .thenReturn(Optional.of(buildUser(CURRENT_USER_ID, LIBRARIAN_EMAIL, Role.LIBRARIAN)));
                when(bookingService.createBooking(eq(OTHER_USER_ID), eq(22), eq(start), eq(end)))
                                .thenReturn(reservation);

                String body = """
                                {
                                  "user_id": "22222222-2222-2222-2222-222222222222",
                                  "seat_id": "22",
                                  "start_time": "2026-04-11T09:00:00",
                                  "end_time": "2026-04-11T11:00:00"
                                }
                                """;

                mockMvc.perform(post("/slib/bookings/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(OTHER_USER_ID.toString()))
                                .andExpect(jsonPath("$.seatId").value(22));
        }

        @Test
        @DisplayName("UTCID04: Create booking with missing required fields")
        void createBooking_withMissingRequiredFields() throws Exception {
                mockMvc.perform(post("/slib/bookings/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "seat_id": "12"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                verify(bookingService, never()).createBooking(any(), any(), any(), any());
        }

        @Test
        @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID05: Create booking when seat is already booked")
        void createBooking_whenSeatIsAlreadyBooked() throws Exception {
                LocalDateTime start = LocalDateTime.of(2026, 4, 12, 8, 0);
                LocalDateTime end = start.plusHours(2);

                when(userRepository.findByEmail(STUDENT_EMAIL))
                                .thenReturn(Optional.of(buildUser(CURRENT_USER_ID, STUDENT_EMAIL, Role.STUDENT)));
                when(userRepository.findById(CURRENT_USER_ID))
                                .thenReturn(Optional.of(buildUser(CURRENT_USER_ID, STUDENT_EMAIL, Role.STUDENT)));
                when(bookingService.createBooking(eq(CURRENT_USER_ID), eq(18), eq(start), eq(end)))
                                .thenThrow(new RuntimeException("Seat already booked"));

                String body = """
                                {
                                  "user_id": "11111111-1111-1111-1111-111111111111",
                                  "seat_id": "18",
                                  "start_time": "2026-04-12T08:00:00",
                                  "end_time": "2026-04-12T10:00:00"
                                }
                                """;

                mockMvc.perform(post("/slib/bookings/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID06: Create booking with malformed user_id")
        void createBooking_withMalformedUserId() throws Exception {
                when(userRepository.findByEmail(STUDENT_EMAIL))
                                .thenReturn(Optional.of(buildUser(CURRENT_USER_ID, STUDENT_EMAIL, Role.STUDENT)));

                mockMvc.perform(post("/slib/bookings/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "user_id": "not-a-uuid",
                                                  "seat_id": "12",
                                                  "start_time": "2026-04-10T08:00:00",
                                                  "end_time": "2026-04-10T10:00:00"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                verify(bookingService, never()).createBooking(any(), any(), any(), any());
        }

        @Test
        @WithMockUser(username = STUDENT_EMAIL, roles = "STUDENT")
        @DisplayName("UTCID07: Create booking for another user's user_id as student")
        void createBooking_forAnotherUsersUserIdAsStudent() throws Exception {
                when(userRepository.findByEmail(STUDENT_EMAIL))
                                .thenReturn(Optional.of(buildUser(CURRENT_USER_ID, STUDENT_EMAIL, Role.STUDENT)));
                when(userRepository.findById(CURRENT_USER_ID))
                                .thenReturn(Optional.of(buildUser(CURRENT_USER_ID, STUDENT_EMAIL, Role.STUDENT)));

                mockMvc.perform(post("/slib/bookings/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "user_id": "22222222-2222-2222-2222-222222222222",
                                                  "seat_id": "20",
                                                  "start_time": "2026-04-10T08:00:00",
                                                  "end_time": "2026-04-10T10:00:00"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                verify(bookingService, never()).createBooking(any(), any(), any(), any());
        }
}
