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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.booking.BookingController;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.booking.BookingService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-72: Confirm booking via NFC after library check-in
 * Test Report: doc/Report/UnitTestReport/FE65_TestReport.md
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-72: Confirm booking via NFC after library check-in - Unit Tests")
class FE72_ConfirmViaNFCTest {

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

        // =========================================
        // === UTCID01: Valid reservationId + nfc_uid ===
        // =========================================

        /**
         * UTCID01: Valid reservationId + nfc_uid mapped to the reserved seat
         * Precondition: reservationId and nfc_uid are submitted from the mobile app
         * Expected: 200 OK with confirmed reservation
         */
        @Test
        @DisplayName("UTCID01: Valid NFC UID confirmation returns 200 OK")
        void confirmNfcUid_validData_returns200OK() throws Exception {
                UUID reservationId = UUID.randomUUID();
                ReservationEntity reservation = new ReservationEntity();
                reservation.setReservationId(reservationId);
                reservation.setStatus("CONFIRMED");

                when(bookingService.confirmSeatWithNfcUid(eq(reservationId), eq("04A23C91")))
                                .thenReturn(reservation);

                Map<String, String> request = new HashMap<>();
                request.put("nfc_uid", "04A23C91");

                mockMvc.perform(post("/slib/bookings/confirm-nfc-uid/" + reservationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(bookingService, times(1)).confirmSeatWithNfcUid(eq(reservationId), eq("04A23C91"));
        }

        // =========================================
        // === UTCID02: Missing nfc_uid ===
        // =========================================

        /**
         * UTCID02: Missing nfc_uid in request body
         * Precondition: reservationId and nfc_uid are submitted from the mobile app
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCID02: Missing nfc_uid returns 400 Bad Request")
        void confirmNfcUid_missingUid_returns400() throws Exception {
                UUID reservationId = UUID.randomUUID();

                Map<String, String> request = new HashMap<>();
                // nfc_uid is missing

                mockMvc.perform(post("/slib/bookings/confirm-nfc-uid/" + reservationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(bookingService, never()).confirmSeatWithNfcUid(any(), anyString());
        }

        // =========================================
        // === UTCID03: UID not mapped to any seat ===
        // =========================================

        /**
         * UTCID03: UID is not mapped to any seat
         * Precondition: reservationId and nfc_uid are submitted from the mobile app
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCID03: Unmapped NFC UID returns 400 Bad Request")
        void confirmNfcUid_unmappedUid_returns400() throws Exception {
                UUID reservationId = UUID.randomUUID();

                when(bookingService.confirmSeatWithNfcUid(eq(reservationId), eq("UNKNOWN_UID")))
                                .thenThrow(new RuntimeException("Khong tim thay ghe voi NFC UID nay"));

                Map<String, String> request = new HashMap<>();
                request.put("nfc_uid", "UNKNOWN_UID");

                mockMvc.perform(post("/slib/bookings/confirm-nfc-uid/" + reservationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(bookingService, times(1)).confirmSeatWithNfcUid(eq(reservationId), eq("UNKNOWN_UID"));
        }

        // =========================================
        // === UTCID04: UID maps to a different seat ===
        // =========================================

        /**
         * UTCID04: UID maps to a different seat than the reservation
         * Precondition: reservationId and nfc_uid are submitted from the mobile app
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCID04: NFC UID maps to different seat returns 400 Bad Request")
        void confirmNfcUid_wrongSeat_returns400() throws Exception {
                UUID reservationId = UUID.randomUUID();

                when(bookingService.confirmSeatWithNfcUid(eq(reservationId), eq("04B99X11")))
                                .thenThrow(new RuntimeException("NFC UID khong khop voi ghe da dat"));

                Map<String, String> request = new HashMap<>();
                request.put("nfc_uid", "04B99X11");

                mockMvc.perform(post("/slib/bookings/confirm-nfc-uid/" + reservationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(bookingService, times(1)).confirmSeatWithNfcUid(eq(reservationId), eq("04B99X11"));
        }

        // =========================================
        // === UTCID05: Seat confirmation outside time window ===
        // =========================================

        /**
         * UTCID05: Seat confirmation is attempted outside the valid time window
         * Precondition: reservationId and nfc_uid are submitted after the student has checked in to the library
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCID05: Seat confirmation outside time window returns 400 Bad Request")
        void confirmNfcUid_outsideTimeWindow_returns400() throws Exception {
                UUID reservationId = UUID.randomUUID();

                when(bookingService.confirmSeatWithNfcUid(eq(reservationId), eq("04A23C91")))
                                .thenThrow(new RuntimeException("Ngoài khung giờ cho phép xác nhận ghế"));

                Map<String, String> request = new HashMap<>();
                request.put("nfc_uid", "04A23C91");

                mockMvc.perform(post("/slib/bookings/confirm-nfc-uid/" + reservationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(bookingService, times(1)).confirmSeatWithNfcUid(eq(reservationId), eq("04A23C91"));
        }

        // =========================================
        // === UTCID06: Reservation does not exist ===
        // =========================================

        /**
         * UTCID06: Reservation does not exist
         * Precondition: reservationId and nfc_uid are submitted from the mobile app
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCID06: Non-existent reservation returns 400 Bad Request")
        void confirmNfcUid_reservationNotFound_returns400() throws Exception {
                UUID unknownReservationId = UUID.randomUUID();

                when(bookingService.confirmSeatWithNfcUid(eq(unknownReservationId), eq("04A23C91")))
                                .thenThrow(new RuntimeException("Khong tim thay dat cho"));

                Map<String, String> request = new HashMap<>();
                request.put("nfc_uid", "04A23C91");

                mockMvc.perform(post("/slib/bookings/confirm-nfc-uid/" + unknownReservationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(bookingService, times(1)).confirmSeatWithNfcUid(eq(unknownReservationId), eq("04A23C91"));
        }
}
