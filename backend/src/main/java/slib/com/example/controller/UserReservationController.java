package slib.com.example.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.CreateReservationRequest;
import slib.com.example.dto.ReservationResponse;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.UserReservationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserReservationController {
    
    private final UserReservationService reservationService;
    private final UserRepository userRepository;
    
    /**
     * Tạo reservation mới
     * POST /api/reservations
     * Body: { "seatCode": "A1", "startTime": "2026-01-10T08:00:00", "endTime": "2026-01-10T10:00:00" }
     */
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        ReservationResponse response = reservationService.createReservation(
                user.getId(),
                request.getSeatCode(),
                request.getStartTime(),
                request.getEndTime()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lấy tất cả reservation của user
     * GET /api/reservations/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<ReservationResponse> reservations = reservationService.getMyReservations(user.getId());
        return ResponseEntity.ok(reservations);
    }
    
    /**
     * Lấy chi tiết 1 reservation
     * GET /api/reservations/{reservationId}
     */
    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservationDetails(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        ReservationResponse response = reservationService.getReservationDetails(reservationId, user.getId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Hủy reservation
     * DELETE /api/reservations/{reservationId}
     */
    @DeleteMapping("/{reservationId}")
    public ResponseEntity<String> cancelReservation(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        reservationService.cancelReservation(reservationId, user.getId());
        return ResponseEntity.ok("Reservation cancelled successfully");
    }
}
