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
import slib.com.example.entity.users.User;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.booking.BookingService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-80: Release occupied seat by Librarian
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-80: Release occupied seat by Librarian - Unit Tests")
class FE80_ReleaseOccupiedSeatTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookingService bookingService;

        @MockBean
        private ReservationRepository reservationRepository;

        @MockBean
        private UserRepository userRepository;

        private static final UUID RESERVATION_ID = UUID.fromString("aaaa1111-1111-1111-1111-111111111111");
        private static final UUID USER_ID = UUID.fromString("cccc3333-3333-3333-3333-333333333333");

        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn", roles = "LIBRARIAN")
        @DisplayName("UTCD01: Librarian releases occupied seat - returns 200")
        void releaseSeat_validLibrarian_returns200() throws Exception {
                User lib = User.builder().id(USER_ID).email("librarian@fpt.edu.vn").build();
                when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(lib));

                ReservationEntity reservation = new ReservationEntity();
                reservation.setReservationId(RESERVATION_ID);
                reservation.setStatus("COMPLETED");
                User student = User.builder().id(UUID.randomUUID()).build();
                reservation.setUser(student);
                SeatEntity seat = new SeatEntity();
                seat.setSeatId(1);
                reservation.setSeat(seat);

                when(bookingService.leaveSeat(RESERVATION_ID)).thenReturn(reservation);

                mockMvc.perform(post("/slib/bookings/leave-seat/{reservationId}", RESERVATION_ID))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn", roles = "LIBRARIAN")
        @DisplayName("UTCD02: Release non-existent reservation - returns 400")
        void releaseSeat_notFound_returns400() throws Exception {
                User lib = User.builder().id(USER_ID).email("librarian@fpt.edu.vn").build();
                when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(lib));
                when(bookingService.leaveSeat(RESERVATION_ID))
                                .thenThrow(new RuntimeException("Reservation not found"));

                mockMvc.perform(post("/slib/bookings/leave-seat/{reservationId}", RESERVATION_ID))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn", roles = "LIBRARIAN")
        @DisplayName("UTCD03: Release already completed reservation - returns 400")
        void releaseSeat_alreadyCompleted_returns400() throws Exception {
                User lib = User.builder().id(USER_ID).email("librarian@fpt.edu.vn").build();
                when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(lib));
                when(bookingService.leaveSeat(RESERVATION_ID))
                                .thenThrow(new RuntimeException("Đặt chỗ đã hoàn thành"));

                mockMvc.perform(post("/slib/bookings/leave-seat/{reservationId}", RESERVATION_ID))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn", roles = "LIBRARIAN")
        @DisplayName("UTCD04: Update reservation status to COMPLETED - returns 200")
        void updateStatus_toCompleted_returns200() throws Exception {
                ReservationEntity reservation = new ReservationEntity();
                reservation.setReservationId(RESERVATION_ID);
                reservation.setStatus("COMPLETED");
                User student = User.builder().id(UUID.randomUUID()).build();
                reservation.setUser(student);
                SeatEntity seat = new SeatEntity();
                seat.setSeatId(1);
                reservation.setSeat(seat);

                when(bookingService.updateStatus(RESERVATION_ID, "COMPLETED")).thenReturn(reservation);

                mockMvc.perform(put("/slib/bookings/updateStatusReserv/{reservationId}?status=COMPLETED",
                                RESERVATION_ID))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn", roles = "LIBRARIAN")
        @DisplayName("UTCD05: Release seat - response contains status COMPLETED")
        void releaseSeat_responseStatus_returns200() throws Exception {
                User lib = User.builder().id(USER_ID).email("librarian@fpt.edu.vn").build();
                when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(lib));

                ReservationEntity reservation = new ReservationEntity();
                reservation.setReservationId(RESERVATION_ID);
                reservation.setStatus("COMPLETED");
                User student = User.builder().id(UUID.randomUUID()).build();
                reservation.setUser(student);
                SeatEntity seat = new SeatEntity();
                seat.setSeatId(1);
                reservation.setSeat(seat);
                when(bookingService.leaveSeat(RESERVATION_ID)).thenReturn(reservation);

                mockMvc.perform(post("/slib/bookings/leave-seat/{reservationId}", RESERVATION_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("COMPLETED"));
        }
}
