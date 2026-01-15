package slib.com.example.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.*;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.SeatManagementService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/seat-management")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SeatManagementController {
    
    private final SeatManagementService seatManagementService;
    private final UserRepository userRepository;
    
    /**
     * Lấy tất cả ghế với trạng thái trong khoảng thời gian
     * GET /api/seat-management/seats (real-time, không có params)
     * GET /api/seat-management/seats?startTime=2024-12-29T07:00:00&endTime=2024-12-29T09:00:00&zone=Khu yên tĩnh
     */
    @GetMapping("/seats")
    public ResponseEntity<SeatManagementResponse> getAllSeats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false, defaultValue = "Tất cả khu vực") String zone
    ) {
        // ✅ KHÔNG set default! Để null để Service tự xử lý real-time vs time-range
        // startTime/endTime = null → Service dùng findActiveReservationsAtTime(now)
        // startTime/endTime có giá trị → Service dùng findReservationsInTimeRange()
        
        SeatManagementResponse response = seatManagementService.getAllSeatsWithStatus(startTime, endTime, zone);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lấy chi tiết 1 ghế
     * GET /api/seat-management/seats/A1?checkTime=2024-12-29T07:00:00
     */
    @GetMapping("/seats/{seatCode}")
    public ResponseEntity<SeatResponse> getSeatDetails(
            @PathVariable String seatCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkTime
    ) {
        if (checkTime == null) {
            checkTime = LocalDateTime.now();
        }
        
        SeatResponse response = seatManagementService.getSeatDetails(seatCode, checkTime);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Thêm hạn chế cho ghế (chỉ librarian)
     * POST /api/seat-management/restrictions
     * Body: { "seatCode": "A1", "reason": "Ghế hỏng", "startTime": "...", "endTime": "..." }
     */
    @PostMapping("/restrictions")
    public ResponseEntity<SeatRestrictionResponse> addRestriction(
            @RequestBody SeatRestrictionRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID librarianId;
        
        // If no authentication (permitAll), use a default librarian or first LIBRARIAN role user
        if (userDetails == null) {
            User librarian = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.LIBRARIAN)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No librarian found in system"));
            librarianId = librarian.getId();
        } else {
            User librarian = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            librarianId = librarian.getId();
        }
        
        SeatRestrictionResponse response = seatManagementService.addSeatRestriction(request, librarianId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Bỏ hạn chế ghế (chỉ librarian)
     * DELETE /api/seat-management/restrictions/{seatCode}
     */
    @DeleteMapping("/restrictions/{seatCode}")
    public ResponseEntity<java.util.Map<String, String>> removeRestriction(@PathVariable String seatCode) {
        seatManagementService.removeSeatRestriction(seatCode);
        return ResponseEntity.ok(java.util.Map.of("message", "Restriction removed successfully"));
    }
    
    /**
     * Lấy tất cả hạn chế đang active
     * GET /api/seat-management/restrictions
     */
    @GetMapping("/restrictions")
    public ResponseEntity<List<SeatRestrictionResponse>> getAllActiveRestrictions() {
        List<SeatRestrictionResponse> restrictions = seatManagementService.getAllActiveRestrictions();
        return ResponseEntity.ok(restrictions);
    }
    
    /**
     * Lấy reservations trong khoảng thời gian
     * GET /api/seat-management/reservations?startTime=...&endTime=...
     */
    @GetMapping("/reservations")
    public ResponseEntity<List<ReservationResponse>> getReservations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        if (endTime == null) {
            endTime = startTime.plusHours(2);
        }
        
        List<ReservationResponse> reservations = seatManagementService.getReservationsInTimeRange(startTime, endTime);
        return ResponseEntity.ok(reservations);
    }
    
    /**
     * Lấy tất cả zones
     * GET /api/seat-management/zones
     */
    @GetMapping("/zones")
    public ResponseEntity<List<ZoneResponse>> getAllZones() {
        List<ZoneResponse> zones = seatManagementService.getAllZones();
        return ResponseEntity.ok(zones);
    }
    
    /**
     * Health check endpoint
     * GET /api/seat-management/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Seat Management API is running");
    }
}
