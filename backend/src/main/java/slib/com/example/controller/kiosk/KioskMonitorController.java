package slib.com.example.controller.kiosk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.service.kiosk.KioskMonitorService;

import java.util.List;
import java.util.Map;

/**
 * Kiosk Monitor Controller
 * Handles monitoring endpoints for library entry/exit
 */
@RestController
@RequestMapping("/slib/kiosk")
@RequiredArgsConstructor
@Slf4j
public class KioskMonitorController {

    private final KioskMonitorService kioskMonitorService;

    /**
     * Get today's statistics
     * GET /slib/kiosk/monitor/stats
     */
    @GetMapping("/monitor/stats")
    public ResponseEntity<Map<String, Object>> getTodayStats() {
        Map<String, Object> stats = kioskMonitorService.getTodayStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get recent entry logs
     * GET /slib/kiosk/monitor/logs?limit=10
     */
    @GetMapping("/monitor/logs")
    public ResponseEntity<List<Map<String, Object>>> getRecentLogs(
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> logs = kioskMonitorService.getRecentLogs(limit);
        return ResponseEntity.ok(logs);
    }
}
