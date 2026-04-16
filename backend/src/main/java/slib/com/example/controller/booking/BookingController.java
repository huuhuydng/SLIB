package slib.com.example.controller.booking;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import slib.com.example.dto.booking.BookingResponse;
import slib.com.example.dto.booking.CancelBookingRequest;
import slib.com.example.dto.booking.ChangeSeatRequest;
import slib.com.example.dto.booking.ReservationDTO;
import slib.com.example.dto.booking.SeatNfcActionStatusResponse;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.booking.BookingService;
import slib.com.example.entity.users.User;
import slib.com.example.security.KioskDevicePrincipal;

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

    private Authentication requireAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }
        return authentication;
    }

    private UUID getCurrentUserId() {
        Authentication authentication = requireAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return user.getId();
        }

        String username = null;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else if (principal instanceof String principalName && principalName != null
                && !"anonymousUser".equalsIgnoreCase(principalName)) {
            username = principalName;
        }

        if (username == null || username.isBlank()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    private UUID resolveAuthorizedUserId(UUID requestedUserId) {
        Authentication authentication = requireAuthentication();
        if (authentication instanceof KioskDevicePrincipal) {
            return requestedUserId;
        }

        UUID currentUserId = getCurrentUserId();
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
            UUID userId = resolveAuthorizedUserId(UUID.fromString(request.get("user_id")));
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
        UUID librarianId = getCurrentUserId();
        ReservationEntity reserv = bookingService.confirmSeatByStaff(reservationId, librarianId);
        return ResponseEntity.ok(toDTO(reserv));
    }

    @PostMapping("/leave-seat/{reservationId}")
    public ResponseEntity<?> leaveSeat(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            getCurrentUserId();
            ReservationEntity reserv = bookingService.leaveSeat(reservationId);
            return ResponseEntity.ok(toDTO(reserv));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/leave-seat-nfc/{reservationId}")
    public ResponseEntity<?> leaveSeatWithNfcUid(
            @PathVariable UUID reservationId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ReservationEntity reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new RuntimeException("Đặt chỗ không tồn tại"));
            UUID userId = resolveAuthorizedUserId(reservation.getUser().getId());

            String nfcUid = request.get("nfc_uid");
            if (nfcUid == null || nfcUid.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Thiếu NFC UID");
            }

            ReservationEntity reserv = bookingService.leaveSeatWithNfcUid(reservationId, userId, nfcUid);
            return ResponseEntity.ok(toDTO(reserv));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- GET BOOKINGS BY USER (with zone/area info) ---
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBookingsByUser(@PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            UUID resolvedUserId = resolveAuthorizedUserId(userId);
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
            UUID resolvedUserId = resolveAuthorizedUserId(userId);
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
            @RequestBody(required = false) CancelBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ReservationEntity existingReservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new RuntimeException("Reservation not found"));
            resolveAuthorizedUserId(existingReservation.getUser().getId());
            Authentication authentication = requireAuthentication();
            UUID currentUserId = null;
            boolean cancelledByStaff = false;

            if (!(authentication instanceof KioskDevicePrincipal)) {
                currentUserId = getCurrentUserId();
                User currentUser = userRepository.findById(currentUserId)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                cancelledByStaff = currentUser.getRole() != null && currentUser.getRole().isStaff();
            }

            ReservationEntity reservation = bookingService.cancelBooking(
                    reservationId,
                    currentUserId,
                    cancelledByStaff,
                    request != null ? request.getReason() : null);
            return ResponseEntity.ok(toDTO(reservation));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/change-seat/{reservationId}")
    public ResponseEntity<?> changeSeat(
            @PathVariable UUID reservationId,
            @RequestBody ChangeSeatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (request == null || request.getSeatId() == null) {
                return ResponseEntity.badRequest().body("Vui lòng chọn ghế mới");
            }

            ReservationEntity existingReservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new RuntimeException("Reservation not found"));
            resolveAuthorizedUserId(existingReservation.getUser().getId());
            UUID currentUserId = getCurrentUserId();

            ReservationEntity reservation = bookingService.changeSeatForLayoutAffectedReservation(
                    reservationId,
                    currentUserId,
                    request.getSeatId());
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

    @GetMapping("/seat-nfc-status/{reservationId}")
    public ResponseEntity<?> getSeatNfcActionStatus(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ReservationEntity reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new RuntimeException("Đặt chỗ không tồn tại"));
            resolveAuthorizedUserId(reservation.getUser().getId());

            SeatNfcActionStatusResponse response = bookingService.getSeatNfcActionStatus(reservationId);
            return ResponseEntity.ok(response);
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
        dto.setActualEndTime(entity.getActualEndTime());
        return dto;
    }
}
