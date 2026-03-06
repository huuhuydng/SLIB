package slib.com.example.controller.kiosk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.service.kiosk.KioskQrAuthService;
import slib.com.example.service.kiosk.KioskQrDTO;

import java.util.Map;

/**
 * Kiosk Authentication Controller
 * Handles QR code generation and validation for kiosk devices
 */
@RestController
@RequestMapping("/slib/kiosk")
@RequiredArgsConstructor
@Slf4j
public class KioskAuthController {

    private final KioskQrAuthService kioskQrAuthService;

    /**
     * Generate QR code for kiosk
     * GET /slib/kiosk/qr/generate/{kioskCode}
     */
    @GetMapping("/qr/generate/{kioskCode}")
    public ResponseEntity<KioskQrDTO.QrGenerateResponse> generateQr(@PathVariable String kioskCode) {
        log.info("Generating QR for kiosk: {}", kioskCode);
        KioskQrDTO.QrGenerateResponse response = kioskQrAuthService.generateQr(kioskCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate QR code from mobile app
     * POST /slib/kiosk/qr/validate
     *
     * Body: { "qrPayload": "...", "kioskCode": "..." }
     */
    @PostMapping("/qr/validate")
    public ResponseEntity<KioskQrDTO.QrValidateResponse> validateQr(@RequestBody Map<String, String> request) {
        String qrPayload = request.get("qrPayload");
        String kioskCode = request.get("kioskCode");

        log.info("Validating QR for kiosk: {}", kioskCode);
        KioskQrDTO.QrValidateResponse response = kioskQrAuthService.validateQr(qrPayload, kioskCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Complete session after mobile authenticates
     * POST /slib/kiosk/session/complete
     *
     * Body: { "sessionToken": "...", "userId": "..." }
     */
    @PostMapping("/session/complete")
    public ResponseEntity<KioskQrDTO.KioskSessionResponse> completeSession(@RequestBody Map<String, String> request) {
        String sessionToken = request.get("sessionToken");
        String userIdStr = request.get("userId");

        log.info("Completing session with token: {}", sessionToken);

        java.util.UUID userId = java.util.UUID.fromString(userIdStr);
        KioskQrDTO.KioskSessionResponse response = kioskQrAuthService.completeSession(sessionToken, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get current active session for kiosk
     * GET /slib/kiosk/session/{kioskCode}
     */
    @GetMapping("/session/{kioskCode}")
    public ResponseEntity<KioskQrDTO.KioskSessionResponse> getActiveSession(@PathVariable String kioskCode) {
        return kioskQrAuthService.getActiveSession(kioskCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Check out and close session
     * POST /slib/kiosk/session/checkout
     *
     * Body: { "sessionToken": "..." }
     */
    @PostMapping("/session/checkout")
    public ResponseEntity<Map<String, String>> checkOut(@RequestBody Map<String, String> request) {
        String sessionToken = request.get("sessionToken");

        log.info("Checking out session: {}", sessionToken);
        kioskQrAuthService.checkOut(sessionToken);

        return ResponseEntity.ok(Map.of("success", "true", "message", "Checked out successfully"));
    }

    /**
     * Check-in — sinh viên ấn nút Check-in trên dashboard
     * POST /slib/kiosk/session/checkin
     *
     * Body: { "sessionToken": "..." }
     */
    @PostMapping("/session/checkin")
    public ResponseEntity<Map<String, String>> checkIn(@RequestBody Map<String, String> request) {
        String sessionToken = request.get("sessionToken");

        log.info("Check-in session: {}", sessionToken);
        kioskQrAuthService.checkIn(sessionToken);

        return ResponseEntity.ok(Map.of("success", "true", "message", "Check-in thành công"));
    }

    /**
     * Check-out từ mobile app (không cần sessionToken)
     * POST /slib/kiosk/session/checkout-mobile
     *
     * Body: { "userId": "..." }
     */
    @PostMapping("/session/checkout-mobile")
    public ResponseEntity<Map<String, String>> checkOutMobile(@RequestBody Map<String, String> request) {
        String userIdStr = request.get("userId");

        log.info("Mobile check-out for user: {}", userIdStr);
        kioskQrAuthService.checkOutByUserId(java.util.UUID.fromString(userIdStr));

        return ResponseEntity.ok(Map.of("success", "true", "message", "Check-out từ mobile thành công"));
    }

    /**
     * Kiểm tra trạng thái check-in hiện tại của user
     * GET /slib/kiosk/session/check-status/{userId}
     */
    @GetMapping("/session/check-status/{userId}")
    public ResponseEntity<Map<String, Object>> checkStatus(@PathVariable String userId) {
        try {
            java.util.UUID uid = java.util.UUID.fromString(userId);
            boolean isCheckedIn = kioskQrAuthService.isUserCheckedIn(uid);
            return ResponseEntity.ok(Map.of("isCheckedIn", isCheckedIn));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("isCheckedIn", false));
        }
    }

    /**
     * Chỉ expire session kiosk, KHÔNG check-out (đóng AccessLog)
     * Dùng khi kiosk timeout hoặc user bấm back
     */
    @PostMapping("/session/expire")
    public ResponseEntity<Map<String, String>> expireSession(@RequestBody Map<String, String> request) {
        String sessionToken = request.get("sessionToken");
        log.info("Expiring kiosk session: {}", sessionToken);
        kioskQrAuthService.expireSession(sessionToken);
        return ResponseEntity.ok(Map.of("success", "true", "message", "Session expired"));
    }
}
