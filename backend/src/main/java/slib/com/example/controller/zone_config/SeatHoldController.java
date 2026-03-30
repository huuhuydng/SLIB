package slib.com.example.controller.zone_config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.service.zone_config.SeatHoldService;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/slib/seats")
@RequiredArgsConstructor
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    /**
     * Hold a seat temporarily for a specific time slot
     * POST /slib/seats/{seatId}/hold
     * Deprecated: seat holding is now managed via reservation PROCESSING flow.
     */
    @PostMapping("/{seatId}/hold")
    public ResponseEntity<?> holdSeat(
            @PathVariable Integer seatId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.status(410).body(Map.of(
                "error", "Chức năng giữ ghế trực tiếp không còn được hỗ trợ. Vui lòng dùng luồng tạo booking."
        ));
    }

    /**
     * Release a held seat
     * DELETE /slib/seats/{seatId}/hold
     * Deprecated: seat release now follows reservation cancellation/expiry flow.
     */
    @DeleteMapping("/{seatId}/hold")
    public ResponseEntity<?> releaseSeat(
            @PathVariable Integer seatId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.status(410).body(Map.of(
                "error", "Chức năng nhả ghế trực tiếp không còn được hỗ trợ. Vui lòng dùng luồng booking hiện tại."
        ));
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
