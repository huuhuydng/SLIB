package slib.com.example.controller.kiosk;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.kiosk.CreateKioskRequest;
import slib.com.example.dto.kiosk.UpdateKioskRequest;
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
                .orElseThrow(() -> new BadRequestException("Không tìm thấy kiosk"));

        if (kioskTokenService.hasValidToken(kiosk) && !force) {
            throw new BadRequestException("Kiosk đang có mã kích hoạt còn hiệu lực. Chỉ cấp lại khi thực sự cần thiết.");
        }

        UUID issuedByUserId = extractUserId(authentication);
        String token = kioskTokenService.generateDeviceToken(kioskId, issuedByUserId);
        kioskActivationCodeRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        kioskActivationCodeRepository.deleteByKioskIdAndUsedFalse(kioskId);

        // Tạo mã kích hoạt ngắn 6 ký tự
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

        log.info("Đã tạo token thiết bị và mã kích hoạt {} cho kiosk {} bởi {}", activationCode, kiosk.getKioskCode(), issuedByUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Thu hoi device token cua kiosk.
     * DELETE /slib/kiosk/admin/token/{kioskId}
     */
    @DeleteMapping("/token/{kioskId}")
    public ResponseEntity<Map<String, String>> revokeToken(@PathVariable Integer kioskId) {
        kioskTokenService.revokeDeviceToken(kioskId);
        return ResponseEntity.ok(Map.of("message", "Đã thu hồi mã kích hoạt của kiosk thành công"));
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
    public ResponseEntity<Map<String, Object>> createKiosk(@Valid @RequestBody CreateKioskRequest request) {
        String kioskCode = request.getKioskCode().trim().toUpperCase(Locale.ROOT);
        String kioskName = request.getKioskName().trim();
        String kioskType = request.getKioskType().trim().toUpperCase(Locale.ROOT);
        String location = request.getLocation() != null ? request.getLocation().trim() : null;

        // Kiểm tra mã kiosk trùng
        if (kioskConfigRepository.existsByKioskCode(kioskCode)) {
            throw new BadRequestException("Mã kiosk đã tồn tại: " + kioskCode);
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
        log.info("Đã tạo kiosk mới: {} ({})", kiosk.getKioskCode(), kiosk.getId());

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
            @Valid @RequestBody UpdateKioskRequest request) {

        KioskConfigEntity kiosk = kioskConfigRepository.findById(kioskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kiosk với id: " + kioskId));

        // Cập nhật các trường được phép (kioskCode không được sửa)
        if (request.getKioskName() != null) {
            String kioskName = request.getKioskName().trim();
            if (kioskName.isBlank()) {
                throw new BadRequestException("Tên kiosk không được để trống");
            }
            kiosk.setKioskName(kioskName);
        }

        if (request.getKioskType() != null) {
            String kioskType = request.getKioskType().trim().toUpperCase(Locale.ROOT);
            if (kioskType.isBlank()) {
                throw new BadRequestException("Loại kiosk không được để trống");
            }
            kiosk.setKioskType(kioskType);
        }

        if (request.getLocation() != null) {
            kiosk.setLocation(request.getLocation().trim());
        }

        if (request.getIsActive() != null) {
            kiosk.setIsActive(request.getIsActive());
        }

        kiosk = kioskConfigRepository.save(kiosk);
        log.info("Đã cập nhật kiosk: {} ({})", kiosk.getKioskCode(), kiosk.getId());

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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kiosk với id: " + kioskId));

        // Thu hồi token trước khi xóa
        if (kiosk.getDeviceToken() != null) {
            kioskTokenService.revokeDeviceToken(kioskId);
        }

        String kioskCode = kiosk.getKioskCode();
        kioskConfigRepository.delete(kiosk);
        log.info("Đã xóa kiosk: {} ({})", kioskCode, kioskId);

        return ResponseEntity.ok(Map.of("message", "Đã xóa kiosk " + kioskCode + " thành công"));
    }

    /**
     * Lay thong tin chi tiet kiosk.
     * GET /slib/kiosk/admin/kiosks/{kioskId}
     */
    @GetMapping("/kiosks/{kioskId}")
    public ResponseEntity<Map<String, Object>> getKioskDetail(@PathVariable Integer kioskId) {
        KioskConfigEntity kiosk = kioskConfigRepository.findById(kioskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kiosk với id: " + kioskId));

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
            throw new BadRequestException("Không thể xác định người dùng hiện tại");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getId();
        }
        if (principal instanceof UserDetails) {
            // Fallback: khong lay duoc UUID, dung UUID ngau nhien (truong hop hiem)
            log.warn("Không thể lấy UUID từ UserDetails, sử dụng UUID mặc định");
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }

        throw new BadRequestException("Không thể xác định người dùng hiện tại");
    }
}
