package slib.com.example.controller.booking;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import slib.com.example.dto.booking.ReservationDTO;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.repository.ReservationRepository;
import slib.com.example.service.BookingService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/slib/bookings")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class BookingController {

    private final BookingService bookingService;
    private final ReservationRepository reservationRepository;

    public BookingController(BookingService bookingService, ReservationRepository reservationRepository) {
        this.bookingService = bookingService;
        this.reservationRepository = reservationRepository;
    }

    // --- CREATE BOOKING ---
    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@RequestBody Map<String, String> request) {
        try {
            UUID userId = UUID.fromString(request.get("user_id"));
            Integer seatId = Integer.parseInt(request.get("seat_id"));
            LocalDateTime startTime = LocalDateTime.parse(request.get("start_time"));
            LocalDateTime endTime = LocalDateTime.parse(request.get("end_time"));

            ReservationEntity reservation = bookingService.createBooking(userId, seatId, startTime, endTime);

            ReservationDTO dto = new ReservationDTO();
            dto.setReservationId(reservation.getReservationId());
            dto.setStatus(reservation.getStatus());
            dto.setUserId(reservation.getUser().getId());
            dto.setSeatId(reservation.getSeat().getSeatId());
            dto.setStartTime(reservation.getStartTime());
            dto.setEndTime(reservation.getEndTime());

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/updateStatusReserv/{reservationId}")
    public ResponseEntity<ReservationEntity> updateStatus(
            @PathVariable UUID reservationId,
            @RequestParam String status) {
        ReservationEntity reserv = bookingService.updateStatus(reservationId, status);
        return ResponseEntity.ok(reserv);
    }

    // --- GET BOOKINGS BY USER (with zone/area info) ---
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBookingsByUser(@PathVariable UUID userId) {
        try {
            var bookings = bookingService.getBookingHistory(userId);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // --- GET UPCOMING BOOKING FOR USER ---
    @GetMapping("/upcoming/{userId}")
    public ResponseEntity<?> getUpcomingBooking(@PathVariable UUID userId) {
        try {
            return bookingService.getUpcomingBooking(userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.noContent().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // --- CANCEL BOOKING ---
    @PutMapping("/cancel/{reservationId}")
    public ResponseEntity<?> cancelBooking(@PathVariable UUID reservationId) {
        try {
            ReservationEntity reservation = bookingService.cancelBooking(reservationId);
            return ResponseEntity.ok(reservation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- CONFIRM SEAT WITH NFC ---
    @PostMapping("/confirm-nfc/{reservationId}")
    public ResponseEntity<?> confirmSeatWithNfc(
            @PathVariable UUID reservationId,
            @RequestBody Map<String, String> request) {
        try {
            String nfcData = request.get("nfc_data");
            if (nfcData == null || nfcData.isEmpty()) {
                return ResponseEntity.badRequest().body("Thiếu dữ liệu NFC");
            }
            ReservationEntity reservation = bookingService.confirmSeatWithNfc(reservationId, nfcData);
            return ResponseEntity.ok(reservation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- GET ALL BOOKINGS ---
    @GetMapping("/getall")
    public List<ReservationEntity> getAllBookings() {
        return bookingService.getAllBookings();
    }
}
