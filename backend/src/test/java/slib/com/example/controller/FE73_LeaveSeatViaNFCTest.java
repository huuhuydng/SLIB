package slib.com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-73: Leave seat via NFC
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-73: Leave seat via NFC - Unit Tests")
class FE73_LeaveSeatViaNFCTest {

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

        private static final UUID RESERVATION_ID = UUID.fromString("aaaa1111-1111-1111-1111-111111111111");
        private static final UUID USER_ID = UUID.fromString("bbbb2222-2222-2222-2222-222222222222");
        private User studentUser;

        @BeforeEach
        void setUp() {
                studentUser = User.builder()
                                .id(USER_ID)
                                .email("student@fpt.edu.vn")
                                .role(Role.STUDENT)
                                .build();
                // Must mock both findByEmail and findById for getCurrentUserId + resolveAuthorizedUserId
                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(studentUser));
                when(userRepository.findById(USER_ID)).thenReturn(Optional.of(studentUser));
        }

        private ReservationEntity buildReservation(String status) {
                SeatEntity seat = new SeatEntity();
                seat.setSeatId(1);
                ReservationEntity r = new ReservationEntity();
                r.setReservationId(RESERVATION_ID);
                r.setUser(studentUser);
                r.setSeat(seat);
                r.setStatus(status);
                r.setStartTime(LocalDateTime.now().minusHours(1));
                r.setEndTime(LocalDateTime.now().plusHours(1));
                return r;
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD01: Leave seat with valid NFC UID - returns 200")
        void leaveSeatNfc_validNfc_returns200() throws Exception {
                ReservationEntity reservation = buildReservation("CONFIRMED");
                when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

                ReservationEntity completed = buildReservation("COMPLETED");
                when(bookingService.leaveSeatWithNfcUid(eq(RESERVATION_ID), eq(USER_ID), eq("NFC-123")))
                                .thenReturn(completed);

                String body = objectMapper.writeValueAsString(Map.of("nfc_uid", "NFC-123"));
                mockMvc.perform(post("/slib/bookings/leave-seat-nfc/{reservationId}", RESERVATION_ID)
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD02: Leave seat with missing NFC UID key - returns 400")
        void leaveSeatNfc_missingNfc_returns400() throws Exception {
                ReservationEntity reservation = buildReservation("CONFIRMED");
                when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

                String body = objectMapper.writeValueAsString(Map.of("other", "value"));
                mockMvc.perform(post("/slib/bookings/leave-seat-nfc/{reservationId}", RESERVATION_ID)
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD03: Leave seat for non-existent reservation - returns 400")
        void leaveSeatNfc_reservationNotFound_returns400() throws Exception {
                UUID unknownId = UUID.randomUUID();
                when(reservationRepository.findById(unknownId)).thenReturn(Optional.empty());

                String body = objectMapper.writeValueAsString(Map.of("nfc_uid", "NFC-123"));
                mockMvc.perform(post("/slib/bookings/leave-seat-nfc/{reservationId}", unknownId)
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD04: Leave seat with empty NFC UID string - returns 400")
        void leaveSeatNfc_emptyNfc_returns400() throws Exception {
                ReservationEntity reservation = buildReservation("CONFIRMED");
                when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

                String body = objectMapper.writeValueAsString(Map.of("nfc_uid", "   "));
                mockMvc.perform(post("/slib/bookings/leave-seat-nfc/{reservationId}", RESERVATION_ID)
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD05: Service throws exception during leave - returns 400")
        void leaveSeatNfc_serviceError_returns400() throws Exception {
                ReservationEntity reservation = buildReservation("CONFIRMED");
                when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
                when(bookingService.leaveSeatWithNfcUid(any(), any(), any()))
                                .thenThrow(new RuntimeException("NFC UID không khớp"));

                String body = objectMapper.writeValueAsString(Map.of("nfc_uid", "WRONG-NFC"));
                mockMvc.perform(post("/slib/bookings/leave-seat-nfc/{reservationId}", RESERVATION_ID)
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD06: Leave seat - response contains reservation data with COMPLETED status")
        void leaveSeatNfc_responseContainsData_returns200() throws Exception {
                ReservationEntity reservation = buildReservation("CONFIRMED");
                when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

                ReservationEntity completed = buildReservation("COMPLETED");
                when(bookingService.leaveSeatWithNfcUid(eq(RESERVATION_ID), eq(USER_ID), eq("NFC-ABC")))
                                .thenReturn(completed);

                String body = objectMapper.writeValueAsString(Map.of("nfc_uid", "NFC-ABC"));
                mockMvc.perform(post("/slib/bookings/leave-seat-nfc/{reservationId}", RESERVATION_ID)
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("COMPLETED"));
        }
}
