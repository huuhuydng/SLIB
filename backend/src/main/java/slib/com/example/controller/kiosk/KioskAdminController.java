package slib.com.example.controller.kiosk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.kiosk.KioskActivationCodeEntity;
import slib.com.example.entity.kiosk.KioskConfigEntity;
import slib.com.example.entity.users.User;
import slib.com.example.exception.BadRequestException;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.repository.kiosk.KioskActivationCodeRepository;
import slib.com.example.repository.kiosk.KioskConfigRepository;
import slib.com.example.service.kiosk.KioskTokenService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller quan ly kiosk device token.
 * Chi danh cho ADMIN va LIBRARIAN.
 */
@RestController
@RequestMapping("/slib/kiosk/admin")
@RequiredArgsConstructor
@Slf4j
public class KioskAdminController {

    private final KioskTokenService kioskTokenService;
    private final KioskConfigRepository kioskConfigRepository;
    private final KioskActivationCodeRepository kioskActivationCodeRepository;

    @Value("${app.frontend-url:https://slibsystem.site}")
    private String frontendUrl;

    /**
     * Tao device token cho kiosk.
     * POST /slib/kiosk/admin/token/{kioskId}
     */
    @PostMapping("/token/{kioskId}")
    public ResponseEntity<Map<String, Object>> generateToken(
            @PathVariable Integer kioskId,
            @RequestParam(defaultValue = "false") boolean force,
            Authentication authentication) {

        KioskConfigEntity kiosk = kioskConfigRepository.findById(kioskId)
                .orElseThrow(() -> new BadRequestException("Khong tim thay kiosk"));

        if (kioskTokenService.hasValidToken(kiosk) && !force) {
            throw new BadRequestException("Kiosk dang co token con hieu luc. Chi cap lai khi thuc su can thiet.");
        }

        UUID issuedByUserId = extractUserId(authentication);
        String token = kioskTokenService.generateDeviceToken(kioskId, issuedByUserId);
        kioskActivationCodeRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        kioskActivationCodeRepository.deleteByKioskIdAndUsedFalse(kioskId);

        // Tao ma kich hoat ngan 6 ky tu
        String activationCode = generateActivationCode();
        KioskActivationCodeEntity codeEntity = KioskActivationCodeEntity.builder()
                .kioskId(kioskId)
                .code(activationCode)
                .deviceToken(token)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();
        kioskActivationCodeRepository.save(codeEntity);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kioskCode", kiosk.getKioskCode());
        response.put("token", token);
        response.put("expiresAt", kiosk.getDeviceTokenExpiresAt());
        response.put("activationUrl", frontendUrl + "/kiosk/?token=" + token);
        response.put("activationCode", activationCode);

        log.info("Da tao device token va ma kich hoat {} cho kiosk {} boi {}", activationCode, kiosk.getKioskCode(), issuedByUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Thu hoi device token cua kiosk.
     * DELETE /slib/kiosk/admin/token/{kioskId}
     */
    @DeleteMapping("/token/{kioskId}")
    public ResponseEntity<Map<String, String>> revokeToken(@PathVariable Integer kioskId) {
        kioskTokenService.revokeDeviceToken(kioskId);
        return ResponseEntity.ok(Map.of("message", "Da thu hoi device token thanh cong"));
    }

    /**
     * Danh sach tat ca kiosk voi trang thai token.
     * GET /slib/kiosk/admin/sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> listKioskSessions() {
        List<KioskConfigEntity> kiosks = kioskConfigRepository.findAll();

        List<Map<String, Object>> result = kiosks.stream().map(kiosk -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", kiosk.getId());
            item.put("kioskCode", kiosk.getKioskCode());
            item.put("kioskName", kiosk.getKioskName());
            item.put("kioskType", kiosk.getKioskType());
            item.put("location", kiosk.getLocation());
            item.put("isActive", kiosk.getIsActive());
            item.put("hasDeviceToken", kiosk.getDeviceToken() != null);
            item.put("deviceTokenIssuedAt", kiosk.getDeviceTokenIssuedAt());
            item.put("deviceTokenExpiresAt", kiosk.getDeviceTokenExpiresAt());
            item.put("lastActiveAt", kiosk.getLastActiveAt());
            boolean tokenValid = kioskTokenService.hasValidToken(kiosk);
            item.put("tokenValid", tokenValid);
            item.put("online", kioskTokenService.isOnline(kiosk));
            item.put("runtimeStatus", kioskTokenService.getRuntimeStatus(kiosk));

            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ==================== CRUD Kiosk Device ====================

    /**
     * Tao kiosk moi.
     * POST /slib/kiosk/admin/kiosks
     */
    @PostMapping("/kiosks")
    @Transactional
    public ResponseEntity<Map<String, Object>> createKiosk(@RequestBody Map<String, String> request) {
        String kioskCode = request.get("kioskCode");
        String kioskName = request.get("kioskName");
        String kioskType = request.get("kioskType");
        String location = request.get("location");

        // Validate truong bat buoc
        if (kioskCode == null || kioskCode.isBlank()) {
            throw new BadRequestException("Ma kiosk khong duoc de trong");
        }
        if (kioskName == null || kioskName.isBlank()) {
            throw new BadRequestException("Ten kiosk khong duoc de trong");
        }
        if (kioskType == null || kioskType.isBlank()) {
            throw new BadRequestException("Loai kiosk khong duoc de trong");
        }
        if (!kioskType.equals("INTERACTIVE") && !kioskType.equals("MONITORING")) {
            throw new BadRequestException("Loai kiosk phai la INTERACTIVE hoac MONITORING");
        }

        // Kiem tra ma kiosk trung
        if (kioskConfigRepository.existsByKioskCode(kioskCode)) {
            throw new BadRequestException("Ma kiosk da ton tai: " + kioskCode);
        }

        KioskConfigEntity kiosk = KioskConfigEntity.builder()
                .kioskCode(kioskCode)
                .kioskName(kioskName)
                .kioskType(kioskType)
                .location(location)
                .isActive(true)
                .qrSecretKey(UUID.randomUUID().toString())
                .build();

        kiosk = kioskConfigRepository.save(kiosk);
        log.info("Da tao kiosk moi: {} ({})", kiosk.getKioskCode(), kiosk.getId());

        return ResponseEntity.ok(buildKioskDetailMap(kiosk));
    }

    /**
     * Cap nhat thong tin kiosk.
     * PUT /slib/kiosk/admin/kiosks/{kioskId}
     */
    @PutMapping("/kiosks/{kioskId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateKiosk(
            @PathVariable Integer kioskId,
            @RequestBody Map<String, Object> request) {

        KioskConfigEntity kiosk = kioskConfigRepository.findById(kioskId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay kiosk voi id: " + kioskId));

        // Cap nhat cac truong duoc phep (kioskCode khong duoc sua)
        if (request.containsKey("kioskName")) {
            String kioskName = (String) request.get("kioskName");
            if (kioskName == null || kioskName.isBlank()) {
                throw new BadRequestException("Ten kiosk khong duoc de trong");
            }
            kiosk.setKioskName(kioskName);
        }

        if (request.containsKey("kioskType")) {
            String kioskType = (String) request.get("kioskType");
            if (kioskType == null || kioskType.isBlank()) {
                throw new BadRequestException("Loai kiosk khong duoc de trong");
            }
            if (!kioskType.equals("INTERACTIVE") && !kioskType.equals("MONITORING")) {
                throw new BadRequestException("Loai kiosk phai la INTERACTIVE hoac MONITORING");
            }
            kiosk.setKioskType(kioskType);
        }

        if (request.containsKey("location")) {
            kiosk.setLocation((String) request.get("location"));
        }

        if (request.containsKey("isActive")) {
            kiosk.setIsActive((Boolean) request.get("isActive"));
        }

        kiosk = kioskConfigRepository.save(kiosk);
        log.info("Da cap nhat kiosk: {} ({})", kiosk.getKioskCode(), kiosk.getId());

        return ResponseEntity.ok(buildKioskDetailMap(kiosk));
    }

    /**
     * Xoa kiosk. Dong thoi thu hoi token neu co.
     * DELETE /slib/kiosk/admin/kiosks/{kioskId}
     */
    @DeleteMapping("/kiosks/{kioskId}")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteKiosk(@PathVariable Integer kioskId) {
        KioskConfigEntity kiosk = kioskConfigRepository.findById(kioskId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay kiosk voi id: " + kioskId));

        // Thu hoi token truoc khi xoa
        if (kiosk.getDeviceToken() != null) {
            kioskTokenService.revokeDeviceToken(kioskId);
        }

        String kioskCode = kiosk.getKioskCode();
        kioskConfigRepository.delete(kiosk);
        log.info("Da xoa kiosk: {} ({})", kioskCode, kioskId);

        return ResponseEntity.ok(Map.of("message", "Da xoa kiosk " + kioskCode + " thanh cong"));
    }

    /**
     * Lay thong tin chi tiet kiosk.
     * GET /slib/kiosk/admin/kiosks/{kioskId}
     */
    @GetMapping("/kiosks/{kioskId}")
    public ResponseEntity<Map<String, Object>> getKioskDetail(@PathVariable Integer kioskId) {
        KioskConfigEntity kiosk = kioskConfigRepository.findById(kioskId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay kiosk voi id: " + kioskId));

        return ResponseEntity.ok(buildKioskDetailMap(kiosk));
    }

    /**
     * Build map chi tiet kiosk bao gom trang thai token.
     */
    private Map<String, Object> buildKioskDetailMap(KioskConfigEntity kiosk) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", kiosk.getId());
        item.put("kioskCode", kiosk.getKioskCode());
        item.put("kioskName", kiosk.getKioskName());
        item.put("kioskType", kiosk.getKioskType());
        item.put("location", kiosk.getLocation());
        item.put("isActive", kiosk.getIsActive());
        item.put("qrSecretKey", kiosk.getQrSecretKey());
        item.put("hasDeviceToken", kiosk.getDeviceToken() != null);
        item.put("deviceTokenIssuedAt", kiosk.getDeviceTokenIssuedAt());
        item.put("deviceTokenExpiresAt", kiosk.getDeviceTokenExpiresAt());
        item.put("lastActiveAt", kiosk.getLastActiveAt());
        boolean tokenValid = kioskTokenService.hasValidToken(kiosk);
        item.put("tokenValid", tokenValid);
        item.put("online", kioskTokenService.isOnline(kiosk));
        item.put("runtimeStatus", kioskTokenService.getRuntimeStatus(kiosk));

        item.put("createdAt", kiosk.getCreatedAt());
        item.put("updatedAt", kiosk.getUpdatedAt());

        return item;
    }

    /**
     * Tao ma kich hoat ngau nhien 6 ky tu (chu hoa + so, loai bo ky tu de nham lan).
     */
    private String generateActivationCode() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();
        } while (kioskActivationCodeRepository.existsByCode(code));
        return code;
    }

    /**
     * Lay UUID cua user hien tai tu Authentication.
     */
    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BadRequestException("Khong the xac dinh nguoi dung hien tai");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getId();
        }
        if (principal instanceof UserDetails) {
            // Fallback: khong lay duoc UUID, dung UUID ngau nhien (truong hop hiem)
            log.warn("Khong the lay UUID tu UserDetails, su dung UUID mac dinh");
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }

        throw new BadRequestException("Khong the xac dinh nguoi dung hien tai");
    }
}
