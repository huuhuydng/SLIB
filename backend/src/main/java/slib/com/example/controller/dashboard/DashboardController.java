package slib.com.example.controller.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import slib.com.example.dto.dashboard.DashboardStatsDTO;
import slib.com.example.service.dashboard.DashboardService;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/slib/dashboard")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    /**
     * Lightweight endpoint for mobile home screen - library occupancy status only
     */
    @GetMapping("/library-status")
    public ResponseEntity<Map<String, Object>> getLibraryStatus() {
        return ResponseEntity.ok(dashboardService.getLibraryStatus());
    }

    /**
     * Test endpoint - gửi broadcast WebSocket trực tiếp để debug
     */
    @PostMapping("/test-broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testBroadcast() {
        try {
            Map<String, String> payload = Map.of(
                    "type", "TEST_BROADCAST",
                    "action", "DEBUG",
                    "timestamp", Instant.now().toString());
            System.out.println("[DASHBOARD] Broadcasting test message to /topic/dashboard: " + payload);
            messagingTemplate.convertAndSend("/topic/dashboard", payload);
            System.out.println("[DASHBOARD] Broadcast sent successfully!");
            return ResponseEntity
                    .ok(Map.of("status", "OK", "message", "Broadcast sent to /topic/dashboard", "payload", payload));
        } catch (Exception e) {
            System.err.println("[DASHBOARD] Broadcast FAILED: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    /**
     * Chart stats with range filter: week, month, year
     */
    @GetMapping("/chart-stats")
    public ResponseEntity<?> getChartStats(@RequestParam(defaultValue = "week") String range) {
        return ResponseEntity.ok(dashboardService.getChartStats(range));
    }

    /**
     * Top students with range filter: week, month, year
     */
    @GetMapping("/top-students")
    public ResponseEntity<?> getTopStudents(@RequestParam(defaultValue = "month") String range) {
        return ResponseEntity.ok(dashboardService.getTopStudents(range));
    }
}
