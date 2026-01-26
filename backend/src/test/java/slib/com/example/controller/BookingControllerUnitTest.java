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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.controller.booking.BookingController;
import slib.com.example.dto.booking.ReservationDTO;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.users.User;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.repository.ReservationRepository;
import slib.com.example.service.BookingService;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for BookingController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 * Test Type: @WebMvcTest (Unit Tests only - no full context)
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("BookingController Unit Tests")
class BookingControllerUnitTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookingService bookingService;

        @MockBean
        private ReservationRepository reservationRepository;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === CREATE BOOKING ENDPOINT ===
        // =========================================

        @Test
        @DisplayName("createBooking_validData_returns200WithReservation")
        void createBooking_validData_returns200WithReservation() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                Integer seatId = 10;
                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime = startTime.plusHours(2);

                Map<String, String> request = new HashMap<>();
                request.put("user_id", userId.toString());
                request.put("seat_id", seatId.toString());
                request.put("start_time", startTime.toString());
                request.put("end_time", endTime.toString());

                ReservationEntity reservation = createReservationEntity(UUID.randomUUID(), userId, seatId,
                                "CONFIRMED", startTime, endTime);

                when(bookingService.createBooking(eq(userId), eq(seatId), any(LocalDateTime.class),
                                any(LocalDateTime.class)))
                                .thenReturn(reservation);

                // Act & Assert
                mockMvc.perform(post("/slib/bookings/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reservationId").exists())
                                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                                .andExpect(jsonPath("$.userId").value(userId.toString()))
                                .andExpect(jsonPath("$.seatId").value(seatId));

                verify(bookingService, times(1)).createBooking(eq(userId), eq(seatId), any(LocalDateTime.class),
                                any(LocalDateTime.class));
        }

        @Test
        @DisplayName("createBooking_invalidUserId_returns400")
        void createBooking_invalidUserId_returns400() throws Exception {
                // Arrange
                Map<String, String> request = new HashMap<>();
                request.put("user_id", "invalid-uuid");
                request.put("seat_id", "10");
                request.put("start_time", LocalDateTime.now().toString());
                request.put("end_time", LocalDateTime.now().plusHours(2).toString());

                // Act & Assert
                mockMvc.perform(post("/slib/bookings/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.containsString("Error:")));

                verify(bookingService, never()).createBooking(any(), any(), any(), any());
        }

        @Test
        @DisplayName("createBooking_serviceThrowsException_returns400")
        void createBooking_serviceThrowsException_returns400() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                Integer seatId = 10;
                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime = startTime.plusHours(2);

                Map<String, String> request = new HashMap<>();
                request.put("user_id", userId.toString());
                request.put("seat_id", seatId.toString());
                request.put("start_time", startTime.toString());
                request.put("end_time", endTime.toString());

                when(bookingService.createBooking(any(), any(), any(), any()))
                                .thenThrow(new RuntimeException("Seat is already booked"));

                // Act & Assert
                mockMvc.perform(post("/slib/bookings/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.containsString("Error:")));

                verify(bookingService, times(1)).createBooking(any(), any(), any(), any());
        }

        @Test
        @DisplayName("createBooking_emptyRequestBody_returns400")
        void createBooking_emptyRequestBody_returns400() throws Exception {
                // Act & Assert
                mockMvc.perform(post("/slib/bookings/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(""))
                                .andExpect(status().isBadRequest());

                verify(bookingService, never()).createBooking(any(), any(), any(), any());
        }

        // ==============================================
        // === UPDATE RESERVATION STATUS ENDPOINT ===
        // ==============================================

        @Test
        @DisplayName("updateStatus_validData_returns200WithUpdatedReservation")
        void updateStatus_validData_returns200WithUpdatedReservation() throws Exception {
                // Arrange
                UUID reservationId = UUID.randomUUID();
                String newStatus = "CHECKED_IN";
                ReservationEntity reservation = createReservationEntity(reservationId, UUID.randomUUID(), 5,
                                "CONFIRMED", LocalDateTime.now(), LocalDateTime.now().plusHours(1));
                reservation.setStatus(newStatus);

                when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
                when(reservationRepository.save(any(ReservationEntity.class))).thenReturn(reservation);

                // Act & Assert
                mockMvc.perform(put("/slib/bookings/updateStatusReserv/{reservationId}", reservationId)
                                .param("status", newStatus)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reservationId").value(reservationId.toString()))
                                .andExpect(jsonPath("$.status").value(newStatus));

                verify(reservationRepository, times(1)).findById(reservationId);
                verify(reservationRepository, times(1)).save(any(ReservationEntity.class));
        }

        @Test
        @DisplayName("updateStatus_notFound_throwsRuntimeException")
        void updateStatus_notFound_throwsRuntimeException() throws Exception {
                // Arrange
                UUID reservationId = UUID.randomUUID();
                when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

                // Act & Assert
                mockMvc.perform(put("/slib/bookings/updateStatusReserv/{reservationId}", reservationId)
                                .param("status", "CANCELLED")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isInternalServerError());

                verify(reservationRepository, times(1)).findById(reservationId);
                verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateStatus_missingStatusParam_returns400")
        void updateStatus_missingStatusParam_returns400() throws Exception {
                // Arrange
                UUID reservationId = UUID.randomUUID();

                // Act & Assert
                mockMvc.perform(put("/slib/bookings/updateStatusReserv/{reservationId}", reservationId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());

                verify(reservationRepository, never()).findById(any());
        }

        // ================================================
        // === GET BOOKINGS BY USER ENDPOINT ===
        // ================================================

        @Test
        @DisplayName("getBookingsByUser_validUserId_returns200WithReservationsList")
        void getBookingsByUser_validUserId_returns200WithReservationsList() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();

                // Mock returns BookingHistoryResponse, not ReservationEntity
                slib.com.example.dto.booking.BookingHistoryResponse res1 = slib.com.example.dto.booking.BookingHistoryResponse
                                .builder()
                                .reservationId(UUID.randomUUID())
                                .status("CONFIRMED")
                                .seatId(1)
                                .seatCode("A01")
                                .zoneName("Zone A")
                                .startTime(LocalDateTime.now())
                                .endTime(LocalDateTime.now().plusHours(2))
                                .build();
                slib.com.example.dto.booking.BookingHistoryResponse res2 = slib.com.example.dto.booking.BookingHistoryResponse
                                .builder()
                                .reservationId(UUID.randomUUID())
                                .status("CHECKED_IN")
                                .seatId(2)
                                .seatCode("A02")
                                .zoneName("Zone A")
                                .startTime(LocalDateTime.now().plusDays(1))
                                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                                .build();
                List<slib.com.example.dto.booking.BookingHistoryResponse> bookings = Arrays.asList(res1, res2);

                when(bookingService.getBookingHistory(userId)).thenReturn(bookings);

                // Act & Assert
                mockMvc.perform(get("/slib/bookings/user/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                                .andExpect(jsonPath("$[1].status").value("CHECKED_IN"));

                verify(bookingService, times(1)).getBookingHistory(userId);
        }

        @Test
        @DisplayName("getBookingsByUser_noBookings_returns200WithEmptyArray")
        void getBookingsByUser_noBookings_returns200WithEmptyArray() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                when(bookingService.getBookingHistory(userId)).thenReturn(List.of());

                // Act & Assert
                mockMvc.perform(get("/slib/bookings/user/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(bookingService, times(1)).getBookingHistory(userId);
        }

        @Test
        @DisplayName("getBookingsByUser_serviceThrowsException_returns400")
        void getBookingsByUser_serviceThrowsException_returns400() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                when(bookingService.getBookingHistory(userId))
                                .thenThrow(new RuntimeException("User not found"));

                // Act & Assert
                mockMvc.perform(get("/slib/bookings/user/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.containsString("Error:")));

                verify(bookingService, times(1)).getBookingHistory(userId);
        }

        // ==========================================
        // === CANCEL BOOKING ENDPOINT ===
        // ==========================================

        @Test
        @DisplayName("cancelBooking_validId_returns200WithCancelledReservation")
        void cancelBooking_validId_returns200WithCancelledReservation() throws Exception {
                // Arrange
                UUID reservationId = UUID.randomUUID();
                ReservationEntity reservation = createReservationEntity(reservationId, UUID.randomUUID(), 3,
                                "CANCELLED", LocalDateTime.now(), LocalDateTime.now().plusHours(1));

                when(bookingService.cancelBooking(reservationId)).thenReturn(reservation);

                // Act & Assert
                mockMvc.perform(put("/slib/bookings/cancel/{reservationId}", reservationId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reservationId").value(reservationId.toString()))
                                .andExpect(jsonPath("$.status").value("CANCELLED"));

                verify(bookingService, times(1)).cancelBooking(reservationId);
        }

        @Test
        @DisplayName("cancelBooking_notFound_returns400")
        void cancelBooking_notFound_returns400() throws Exception {
                // Arrange
                UUID reservationId = UUID.randomUUID();
                when(bookingService.cancelBooking(reservationId))
                                .thenThrow(new RuntimeException("Reservation not found"));

                // Act & Assert
                mockMvc.perform(put("/slib/bookings/cancel/{reservationId}", reservationId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$")
                                                .value(org.hamcrest.Matchers.containsString("Reservation not found")));

                verify(bookingService, times(1)).cancelBooking(reservationId);
        }

        @Test
        @DisplayName("cancelBooking_invalidUUID_returns400")
        void cancelBooking_invalidUUID_returns400() throws Exception {
                // Act & Assert
                mockMvc.perform(put("/slib/bookings/cancel/{reservationId}", "invalid-uuid")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());

                verify(bookingService, never()).cancelBooking(any());
        }

        // ==========================================
        // === GET ALL BOOKINGS ENDPOINT ===
        // ==========================================

        @Test
        @DisplayName("getAllBookings_success_returns200WithAllReservations")
        void getAllBookings_success_returns200WithAllReservations() throws Exception {
                // Arrange
                ReservationEntity res1 = createReservationEntity(UUID.randomUUID(), UUID.randomUUID(), 1, "CONFIRMED",
                                LocalDateTime.now(), LocalDateTime.now().plusHours(2));
                ReservationEntity res2 = createReservationEntity(UUID.randomUUID(), UUID.randomUUID(), 2, "CHECKED_IN",
                                LocalDateTime.now(), LocalDateTime.now().plusHours(1));
                ReservationEntity res3 = createReservationEntity(UUID.randomUUID(), UUID.randomUUID(), 3, "CANCELLED",
                                LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1).plusHours(2));
                List<ReservationEntity> allReservations = Arrays.asList(res1, res2, res3);

                when(bookingService.getAllBookings()).thenReturn(allReservations);

                // Act & Assert
                mockMvc.perform(get("/slib/bookings/getall")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(3))
                                .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                                .andExpect(jsonPath("$[1].status").value("CHECKED_IN"))
                                .andExpect(jsonPath("$[2].status").value("CANCELLED"));

                verify(bookingService, times(1)).getAllBookings();
        }

        @Test
        @DisplayName("getAllBookings_emptyList_returns200WithEmptyArray")
        void getAllBookings_emptyList_returns200WithEmptyArray() throws Exception {
                // Arrange
                when(bookingService.getAllBookings()).thenReturn(List.of());

                // Act & Assert
                mockMvc.perform(get("/slib/bookings/getall")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(bookingService, times(1)).getAllBookings();
        }

        // ==========================================
        // === HELPER METHOD TO CREATE TEST DATA ===
        // ==========================================

        /**
         * Helper method to create ReservationEntity objects for testing
         */
        private ReservationEntity createReservationEntity(UUID reservationId, UUID userId, Integer seatId,
                        String status, LocalDateTime startTime, LocalDateTime endTime) {
                ReservationEntity reservation = new ReservationEntity();
                reservation.setReservationId(reservationId);
                reservation.setStatus(status);
                reservation.setStartTime(startTime);
                reservation.setEndTime(endTime);

                User user = new User();
                user.setId(userId);
                reservation.setUser(user);

                SeatEntity seat = new SeatEntity();
                seat.setSeatId(seatId);
                reservation.setSeat(seat);

                return reservation;
        }
}
