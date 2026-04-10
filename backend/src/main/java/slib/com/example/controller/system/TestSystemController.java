package slib.com.example.controller.system;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.service.system.TestSystemService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/slib/system/test-tools")
@RequiredArgsConstructor
public class TestSystemController {

    private final TestSystemService testSystemService;

    @PostMapping("/bookings/{reservationId}/prepare-reminder")
    public ResponseEntity<Map<String, Object>> prepareReminder(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(testSystemService.prepareCheckinReminder(reservationId));
    }

    @PostMapping("/bookings/{reservationId}/prepare-expiry-warning")
    public ResponseEntity<Map<String, Object>> prepareExpiryWarning(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(testSystemService.prepareExpiryWarning(reservationId));
    }
}
