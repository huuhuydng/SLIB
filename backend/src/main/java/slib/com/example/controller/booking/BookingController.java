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
        ReservationEntity reserv = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        reserv.setStatus(status);
        return ResponseEntity.ok(reservationRepository.save(reserv));
    }

    // --- GET BOOKINGS BY USER ---
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBookingsByUser(@PathVariable UUID userId) {
        try {
            List<ReservationEntity> reservations = bookingService.getBookingsByUser(userId);
            return ResponseEntity.ok(reservations);
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
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // --- GET ALL BOOKINGS ---
    @GetMapping("/getall")
    public List<ReservationEntity> getAllBookings() {
        return bookingService.getAllBookings();
    }
}
