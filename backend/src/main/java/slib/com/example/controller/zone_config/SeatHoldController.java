package slib.com.example.controller.zone_config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.service.SeatHoldService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/slib/seats")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    /**
     * Hold a seat temporarily (5 minutes)
     * POST /slib/seats/{seatId}/hold
     */
    @PostMapping("/{seatId}/hold")
    public ResponseEntity<?> holdSeat(
            @PathVariable Integer seatId,
            @RequestBody Map<String, String> body) {
        try {
            UUID userId = UUID.fromString(body.get("userId"));
            Map<String, Object> result = seatHoldService.holdSeat(seatId, userId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid userId format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Release a held seat
     * DELETE /slib/seats/{seatId}/hold
     */
    @DeleteMapping("/{seatId}/hold")
    public ResponseEntity<?> releaseSeat(
            @PathVariable Integer seatId,
            @RequestBody Map<String, String> body) {
        try {
            UUID userId = UUID.fromString(body.get("userId"));
            Map<String, Object> result = seatHoldService.releaseSeat(seatId, userId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid userId format"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
