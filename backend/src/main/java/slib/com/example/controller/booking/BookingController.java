package slib.com.example.controller.booking;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import slib.com.example.dto.booking.BookingResponse;
import slib.com.example.dto.booking.ReservationDTO;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.booking.BookingService;
import slib.com.example.entity.users.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/slib/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    public BookingController(BookingService bookingService, ReservationRepository reservationRepository,
            UserRepository userRepository) {
        this.bookingService = bookingService;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
    }

    private UUID getCurrentUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    private UUID resolveAuthorizedUserId(UUID requestedUserId, UserDetails userDetails) {
        UUID currentUserId = getCurrentUserId(userDetails);
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (currentUser.getRole() != null && currentUser.getRole().isStaff()) {
            return requestedUserId;
        }
        if (!currentUser.getId().equals(requestedUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền thao tác trên dữ liệu đặt chỗ của người khác.");
        }
        return currentUser.getId();
    }

    // --- CREATE BOOKING ---
    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            UUID userId = resolveAuthorizedUserId(UUID.fromString(request.get("user_id")), userDetails);
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

    @RequestMapping(value = "/manual-confirm/{reservationId}", method = { RequestMethod.POST, RequestMethod.PUT })
    public ResponseEntity<ReservationDTO> manualConfirm(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = getCurrentUserId(userDetails);
        ReservationEntity reserv = bookingService.confirmSeatByStaff(reservationId, librarianId);
        return ResponseEntity.ok(toDTO(reserv));
    }

    // --- GET BOOKINGS BY USER (with zone/area info) ---
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBookingsByUser(@PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);
            var bookings = bookingService.getBookingHistory(resolvedUserId);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // --- GET UPCOMING BOOKING FOR USER ---
    @GetMapping("/upcoming/{userId}")
    public ResponseEntity<?> getUpcomingBooking(@PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);
            return bookingService.getUpcomingBooking(resolvedUserId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.noContent().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // --- CANCEL BOOKING ---
    @PutMapping("/cancel/{reservationId}")
    public ResponseEntity<?> cancelBooking(@PathVariable UUID reservationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ReservationEntity existingReservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new RuntimeException("Reservation not found"));
            resolveAuthorizedUserId(existingReservation.getUser().getId(), userDetails);
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
