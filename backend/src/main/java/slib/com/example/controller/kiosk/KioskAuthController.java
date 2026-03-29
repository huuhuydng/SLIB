package slib.com.example.controller.kiosk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import slib.com.example.entity.kiosk.KioskActivationCodeEntity;
import slib.com.example.entity.kiosk.KioskConfigEntity;
import slib.com.example.entity.users.User;
import slib.com.example.repository.kiosk.KioskActivationCodeRepository;
import slib.com.example.security.KioskDevicePrincipal;
import slib.com.example.service.kiosk.KioskQrAuthService;
import slib.com.example.service.kiosk.KioskQrDTO;
import slib.com.example.service.kiosk.KioskTokenService;
import slib.com.example.service.users.UserService;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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
    private final KioskTokenService kioskTokenService;
    private final KioskActivationCodeRepository kioskActivationCodeRepository;
    private final UserService userService;

    /**
     * Kich hoat kiosk bang device token.
     * POST /slib/kiosk/session/activate
     * Body: { "token": "eyJ..." }
     *
     * Endpoint nay la permitAll - kiosk goi khi khoi dong de xac thuc.
     */
    @PostMapping("/session/activate")
    public ResponseEntity<?> activateDevice(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error", "Token khong duoc de trong"));
        }

        KioskConfigEntity kiosk = kioskTokenService.validateDeviceToken(token);
        if (kiosk == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Token khong hop le, da het han hoac da bi thu hoi"));
        }

        // Cap nhat thoi gian hoat dong cuoi
        kioskTokenService.updateLastActive(kiosk.getId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kioskId", kiosk.getId());
        response.put("kioskCode", kiosk.getKioskCode());
        response.put("kioskName", kiosk.getKioskName());
        response.put("kioskType", kiosk.getKioskType());
        response.put("location", kiosk.getLocation());
        response.put("isActive", kiosk.getIsActive());
        response.put("message", "Kich hoat kiosk thanh cong");

        log.info("Kiosk {} da kich hoat thanh cong", kiosk.getKioskCode());
        return ResponseEntity.ok(response);
    }

    /**
     * Kich hoat kiosk bang ma kich hoat ngan (6 ky tu).
     * POST /slib/kiosk/session/activate-code
     * Body: { "code": "A3F9K2" }
     *
     * Endpoint nay la permitAll - kiosk goi khi nhap ma kich hoat tren man hinh khoa.
     */
    @PostMapping("/session/activate-code")
    @Transactional
    public ResponseEntity<?> activateByCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ma kich hoat khong duoc de trong"));
        }

        // Tim ma kich hoat chua su dung
        KioskActivationCodeEntity activationCode = kioskActivationCodeRepository
                .findByCodeAndUsedFalse(code.toUpperCase().trim())
                .orElse(null);

        if (activationCode == null || activationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Ma kich hoat khong hop le hoac da het han"
            ));
        }

        // Danh dau da su dung
        activationCode.setUsed(true);
        kioskActivationCodeRepository.save(activationCode);

        // Xac thuc bang device token (cung logic nhu /session/activate)
        String deviceToken = activationCode.getDeviceToken();
        KioskConfigEntity kiosk = kioskTokenService.validateDeviceToken(deviceToken);
        if (kiosk == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Token khong hop le, da het han hoac da bi thu hoi"
            ));
        }

        // Cap nhat thoi gian hoat dong cuoi
        kioskTokenService.updateLastActive(kiosk.getId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("deviceToken", deviceToken);
        response.put("kioskId", kiosk.getId());
        response.put("kioskCode", kiosk.getKioskCode());
        response.put("kioskName", kiosk.getKioskName());
        response.put("kioskType", kiosk.getKioskType());
        response.put("location", kiosk.getLocation());
        response.put("isActive", kiosk.getIsActive());
        response.put("message", "Kich hoat kiosk thanh cong");

        log.info("Kiosk {} da kich hoat thanh cong bang ma kich hoat {}", kiosk.getKioskCode(), code);
        return ResponseEntity.ok(response);
    }

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
    public ResponseEntity<?> completeSession(@RequestBody Map<String, String> request) {
        String sessionToken = request.get("sessionToken");
        String userIdStr = request.get("userId");

        log.info("Completing session with token: {}", sessionToken);

        java.util.UUID userId = java.util.UUID.fromString(userIdStr);
        ResponseEntity<Map<String, Object>> authError = validateUserOrKioskAccessGeneric(userId);
        if (authError != null) {
            return ResponseEntity.status(authError.getStatusCode()).body(authError.getBody());
        }
        KioskQrDTO.KioskSessionResponse response = kioskQrAuthService.completeSession(sessionToken, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Verify student code entered on kiosk login screen.
     * GET /slib/kiosk/session/verify-student/{studentCode}
     */
    @GetMapping("/session/verify-student/{studentCode}")
    @PreAuthorize("hasRole('KIOSK')")
    public ResponseEntity<Map<String, Object>> verifyStudent(@PathVariable String studentCode) {
        User student = userService.getActiveStudentByUserCode(studentCode);
        return ResponseEntity.ok(Map.of(
                "userId", student.getId(),
                "userCode", student.getUserCode(),
                "fullName", student.getFullName(),
                "avatarUrl", student.getAvtUrl() != null ? student.getAvtUrl() : ""
        ));
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
        UUID requestedUserId = UUID.fromString(userIdStr);

        // Kiem tra xac thuc: user JWT phai khop userId, hoac la kiosk device token
        ResponseEntity<Map<String, String>> authError = validateUserOrKioskAccess(requestedUserId);
        if (authError != null) {
            return authError;
        }

        log.info("Mobile check-out for user: {}", userIdStr);
        kioskQrAuthService.checkOutByUserId(requestedUserId);

        return ResponseEntity.ok(Map.of("success", "true", "message", "Check-out từ mobile thành công"));
    }

    /**
     * Kiểm tra trạng thái check-in hiện tại của user
     * GET /slib/kiosk/session/check-status/{userId}
     */
    @GetMapping("/session/check-status/{userId}")
    public ResponseEntity<Map<String, Object>> checkStatus(@PathVariable String userId) {
        try {
            UUID uid = UUID.fromString(userId);

            // Kiem tra xac thuc: user JWT phai khop userId, hoac la kiosk device token
            ResponseEntity<Map<String, Object>> authError = validateUserOrKioskAccessGeneric(uid);
            if (authError != null) {
                return authError;
            }

            boolean isCheckedIn = kioskQrAuthService.isUserCheckedIn(uid);
            return ResponseEntity.ok(Map.of("isCheckedIn", isCheckedIn));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("isCheckedIn", false));
        }
    }

    /**
     * Kiem tra quyen truy cap: neu la user JWT thi userId phai khop,
     * neu la kiosk device token thi cho phep.
     * Tra ve ResponseEntity loi neu khong hop le, null neu hop le.
     */
    private ResponseEntity<Map<String, String>> validateUserOrKioskAccess(UUID requestedUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Yeu cau xac thuc"));
        }

        // Kiosk device token -> cho phep
        if (auth instanceof KioskDevicePrincipal) {
            return null;
        }

        // User JWT -> userId phai khop
        if (auth.getPrincipal() instanceof User user) {
            if (!user.getId().equals(requestedUserId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Khong co quyen truy cap tai nguyen cua nguoi dung khac"));
            }
            return null;
        }

        return ResponseEntity.status(401).body(Map.of("error", "Yeu cau xac thuc"));
    }

    /**
     * Phien ban generic cua validateUserOrKioskAccess cho cac endpoint tra ve Map<String, Object>.
     */
    private <T> ResponseEntity<Map<String, T>> validateUserOrKioskAccessGeneric(UUID requestedUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            @SuppressWarnings("unchecked")
            Map<String, T> error = (Map<String, T>) Map.of("error", "Yeu cau xac thuc");
            return ResponseEntity.status(401).body(error);
        }

        if (auth instanceof KioskDevicePrincipal) {
            return null;
        }

        if (auth.getPrincipal() instanceof User user) {
            if (!user.getId().equals(requestedUserId)) {
                @SuppressWarnings("unchecked")
                Map<String, T> error = (Map<String, T>) Map.of("error", "Khong co quyen truy cap tai nguyen cua nguoi dung khac");
                return ResponseEntity.status(403).body(error);
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, T> error = (Map<String, T>) Map.of("error", "Yeu cau xac thuc");
        return ResponseEntity.status(401).body(error);
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
