package slib.com.example.controller.zone_config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.service.zone_config.SeatHoldService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/slib/seats")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    /**
     * Hold a seat temporarily for a specific time slot
     * POST /slib/seats/{seatId}/hold
     * Body: { userId, startTime, endTime }
     */
    @PostMapping("/{seatId}/hold")
    public ResponseEntity<?> holdSeat(
            @PathVariable Integer seatId,
            @RequestBody Map<String, String> body) {
        try {
            UUID userId = UUID.fromString(body.get("userId"));
            LocalDateTime startTime = LocalDateTime.parse(body.get("startTime"));
            LocalDateTime endTime = LocalDateTime.parse(body.get("endTime"));

            Map<String, Object> result = seatHoldService.holdSeat(seatId, userId, startTime, endTime);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Release a held seat
     * DELETE /slib/seats/{seatId}/hold
     * Body: { userId, startTime, endTime }
     */
    @DeleteMapping("/{seatId}/hold")
    public ResponseEntity<?> releaseSeat(
            @PathVariable Integer seatId,
            @RequestBody Map<String, String> body) {
        try {
            UUID userId = UUID.fromString(body.get("userId"));
            LocalDateTime startTime = LocalDateTime.parse(body.get("startTime"));
            LocalDateTime endTime = LocalDateTime.parse(body.get("endTime"));

            Map<String, Object> result = seatHoldService.releaseSeat(seatId, userId, startTime, endTime);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if a seat is available for a time slot
     * GET /slib/seats/{seatId}/available
     */
    @GetMapping("/{seatId}/available")
    public ResponseEntity<?> checkAvailability(
            @PathVariable Integer seatId,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTime);
            LocalDateTime end = LocalDateTime.parse(endTime);

            boolean available = seatHoldService.isSeatAvailable(seatId, start, end);
            return ResponseEntity.ok(Map.of("available", available, "seatId", seatId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
