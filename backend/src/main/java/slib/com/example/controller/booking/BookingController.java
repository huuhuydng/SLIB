package slib.com.example.controller.booking;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import slib.com.example.dto.booking.BookingResponse;
import slib.com.example.dto.booking.ReservationDTO;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.service.booking.BookingService;

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
            dto.setConfirmedAt(reservation.getConfirmedAt());

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/updateStatusReserv/{reservationId}")
    public ResponseEntity<ReservationDTO> updateStatus(
            @PathVariable UUID reservationId,
            @RequestParam String status) {
        ReservationEntity reserv = bookingService.updateStatus(reservationId, status);
        return ResponseEntity.ok(toDTO(reserv));
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
            return ResponseEntity.ok(toDTO(reservation));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- CONFIRM SEAT WITH NFC (legacy — deprecated) ---
    @Deprecated
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
            return ResponseEntity.ok(toDTO(reservation));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- CONFIRM SEAT WITH NFC UID (new — UID Mapping Strategy) ---
    @PostMapping("/confirm-nfc-uid/{reservationId}")
    public ResponseEntity<?> confirmSeatWithNfcUid(
            @PathVariable UUID reservationId,
            @RequestBody Map<String, String> request) {
        try {
            String nfcUid = request.get("nfc_uid");
            if (nfcUid == null || nfcUid.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Thiếu NFC UID");
            }
            ReservationEntity reservation = bookingService.confirmSeatWithNfcUid(reservationId, nfcUid);
            return ResponseEntity.ok(toDTO(reservation));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- GET ALL BOOKINGS ---
    @GetMapping("/getall")
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    // --- BATCH DELETE ---
    @DeleteMapping("/batch")
    public ResponseEntity<?> deleteBatch(@RequestBody Map<String, List<String>> body) {
        try {
            List<String> ids = body.get("ids");
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Danh sách ID không được trống"));
            }
            List<UUID> uuids = ids.stream().map(UUID::fromString).collect(java.util.stream.Collectors.toList());
            reservationRepository.deleteAllById(uuids);
            return ResponseEntity.ok(Map.of("deleted", uuids.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private ReservationDTO toDTO(ReservationEntity entity) {
        ReservationDTO dto = new ReservationDTO();
        dto.setReservationId(entity.getReservationId());
        dto.setStatus(entity.getStatus());
        dto.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        dto.setSeatId(entity.getSeat() != null ? entity.getSeat().getSeatId() : null);
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setConfirmedAt(entity.getConfirmedAt());
        return dto;
    }
}
