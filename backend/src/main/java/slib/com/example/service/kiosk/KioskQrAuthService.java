package slib.com.example.service.kiosk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.activity.ActivityLogEntity;
import slib.com.example.entity.hce.AccessLog;
import slib.com.example.entity.kiosk.KioskConfigEntity;
import slib.com.example.entity.kiosk.KioskQrSessionEntity;
import slib.com.example.entity.users.User;
import slib.com.example.exception.BadRequestException;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.repository.hce.AccessLogRepository;
import slib.com.example.repository.kiosk.KioskConfigRepository;
import slib.com.example.repository.kiosk.KioskQrSessionRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.activity.ActivityService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Kiosk QR Authentication Service
 * Handles QR code generation and validation for kiosk authentication
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KioskQrAuthService {

    private final KioskConfigRepository kioskConfigRepository;
    private final KioskQrSessionRepository qrSessionRepository;
    private final UserRepository userRepository;
    private final AccessLogRepository accessLogRepository;
    private final ActivityService activityService;
    private final KioskWebSocketService kioskWebSocketService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${kiosk.qr.ttl-seconds:600}") // 10 minutes default
    private int qrTtlSeconds;

    @Value("${kiosk.qr.algorithm:HmacSHA256}")
    private String hmacAlgorithm;

    private static final String ALGORITHM = "HmacSHA256";
    /**
     * Generate a new QR code for kiosk
     */
    @Transactional
    public KioskQrDTO.QrGenerateResponse generateQr(String kioskCode) {
        // Find kiosk config
        KioskConfigEntity kiosk = kioskConfigRepository.findByKioskCode(kioskCode)
                .orElseThrow(() -> new ResourceNotFoundException("Kiosk not found: " + kioskCode));

        if (!kiosk.getIsActive()) {
            throw new BadRequestException("Kiosk is not active: " + kioskCode);
        }

        // Expire old active sessions for this kiosk
        qrSessionRepository.expireOldSessions(LocalDateTime.now());

        // Generate QR payload
        String nonce = generateNonce();
        long timestamp = System.currentTimeMillis();
        String payload = buildQrPayload(kiosk.getKioskCode(), timestamp, nonce);

        // Sign the payload
        String signature = signPayload(payload, kiosk.getQrSecretKey());
        String fullPayload = payload + "." + signature;

        // Create session
        KioskQrSessionEntity session = KioskQrSessionEntity.builder()
                .kiosk(kiosk)
                .sessionToken(generateSessionToken())
                .qrPayload(fullPayload)
                .qrExpiresAt(LocalDateTime.now().plusSeconds(qrTtlSeconds))
                .status("ACTIVE")
                .build();

        qrSessionRepository.save(session);

        log.info("Generated QR for kiosk {} with token {}", kioskCode, session.getSessionToken());

        return KioskQrDTO.QrGenerateResponse.builder()
                .qrPayload(fullPayload)
                .expiresAt(session.getQrExpiresAt())
                .kioskId(kiosk.getId())
                .kioskCode(kiosk.getKioskCode())
                .ttlSeconds(qrTtlSeconds)
                .build();
    }

    /**
     * Validate QR code from mobile app
     */
    @Transactional
    public KioskQrDTO.QrValidateResponse validateQr(String qrPayload, String kioskCode) {
        // Parse QR payload: format = "kioskCode:timestamp:ttl:nonce.signature"
        int lastDot = qrPayload.lastIndexOf('.');
        if (lastDot == -1 || lastDot == 0 || lastDot == qrPayload.length() - 1) {
            return KioskQrDTO.QrValidateResponse.builder()
                    .success(false)
                    .message("Invalid QR format")
                    .build();
        }

        String payload = qrPayload.substring(0, lastDot);
        String signature = qrPayload.substring(lastDot + 1);

        // Parse payload parts: kioskCode:timestamp:ttl:nonce
        String[] payloadParts = payload.split(":");
        if (payloadParts.length < 3) {
            return KioskQrDTO.QrValidateResponse.builder()
                    .success(false)
                    .message("Invalid payload format")
                    .build();
        }

        String kid = payloadParts[0]; // kiosk code
        long timestamp = Long.parseLong(payloadParts[1]);
        String nonce = payloadParts.length > 3 ? payloadParts[3] : payloadParts[2];

        // Verify kiosk code matches
        if (!kid.equals(kioskCode)) {
            return KioskQrDTO.QrValidateResponse.builder()
                    .success(false)
                    .message("QR code is for different kiosk")
                    .build();
        }

        // Find kiosk config
        KioskConfigEntity kiosk = kioskConfigRepository.findByKioskCode(kioskCode)
                .orElse(null);

        if (kiosk == null) {
            return KioskQrDTO.QrValidateResponse.builder()
                    .success(false)
                    .message("Kiosk not found")
                    .build();
        }

        // Verify signature
        String expectedSignature = signPayload(payload, kiosk.getQrSecretKey());
        if (!expectedSignature.equals(signature)) {
            log.warn("Invalid QR signature for kiosk {}", kioskCode);
            return KioskQrDTO.QrValidateResponse.builder()
                    .success(false)
                    .message("Invalid QR signature")
                    .build();
        }

        // Check timestamp (should not be older than TTL)
        long currentTime = System.currentTimeMillis();
        if (currentTime - timestamp > qrTtlSeconds * 1000) {
            return KioskQrDTO.QrValidateResponse.builder()
                    .success(false)
                    .message("QR code has expired")
                    .build();
        }

        // Find the session associated with this QR payload
        Optional<KioskQrSessionEntity> sessionOpt = qrSessionRepository.findByQrPayloadAndStatus(qrPayload, "ACTIVE");
        if (sessionOpt.isEmpty()) {
            return KioskQrDTO.QrValidateResponse.builder()
                    .success(false)
                    .message("QR session not found or expired")
                    .build();
        }

        return KioskQrDTO.QrValidateResponse.builder()
                .success(true)
                .message("QR validated successfully")
                .sessionToken(sessionOpt.get().getSessionToken())
                .kioskName(kiosk.getKioskName())
                .build();
    }

    /**
     * Complete the session after mobile authenticates
     * Auto check-in: tự động tạo AccessLog khi quét QR
     */
    @Transactional
    public KioskQrDTO.KioskSessionResponse completeSession(String sessionToken, UUID userId) {
        // Find the session
        KioskQrSessionEntity session = qrSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (!"ACTIVE".equals(session.getStatus())) {
            throw new BadRequestException("Session is not active");
        }

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Auto check-in: kiểm tra xem đã check-in chưa
        Optional<AccessLog> existingCheckIn = accessLogRepository.checkInUser(userId);
        UUID accessLogId;
        LocalDateTime checkInTime;

        if (existingCheckIn.isPresent()) {
            // Đã check-in rồi — giữ nguyên
            accessLogId = existingCheckIn.get().getLogId();
            checkInTime = existingCheckIn.get().getCheckInTime();
            log.info("User {} already checked in, keeping existing log", user.getUserCode());
        } else {
            // Chưa check-in — tạo AccessLog mới (auto check-in)
            AccessLog newLog = AccessLog.builder()
                    .user(user)
                    .deviceId(session.getKiosk().getKioskCode())
                    .checkInTime(LocalDateTime.now())
                    .build();
            newLog = accessLogRepository.save(newLog);
            accessLogId = newLog.getLogId();
            checkInTime = newLog.getCheckInTime();
            logCheckInActivity(user, session.getKiosk().getKioskCode());
            log.info("Auto check-in for user {} via kiosk", user.getUserCode());
            broadcastAccessLog(newLog, "CHECK_IN");
        }

        // Update session
        session.setStudent(user);
        session.setAccessLogId(accessLogId);
        session.setStatus("USED");
        qrSessionRepository.save(session);

        // Gửi WebSocket notification cho kiosk frontend
        KioskQrDTO.KioskSessionUpdateMessage wsMessage = KioskQrDTO.KioskSessionUpdateMessage.builder()
                .sessionToken(sessionToken)
                .studentId(user.getId())
                .studentCode(user.getUserCode())
                .studentName(user.getFullName())
                .studentAvatar(user.getAvtUrl())
                .action("CHECK_IN")
                .accessLogId(accessLogId)
                .timestamp(LocalDateTime.now())
                .build();
        kioskWebSocketService.sendSessionUpdate(session.getKiosk().getKioskCode(), wsMessage);

        return KioskQrDTO.KioskSessionResponse.builder()
                .sessionToken(sessionToken)
                .studentId(user.getId())
                .studentCode(user.getUserCode())
                .studentName(user.getFullName())
                .studentAvatar(user.getAvtUrl())
                .accessLogId(accessLogId)
                .currentAction("CHECK_IN")
                .checkInTime(checkInTime)
                .expiresAt(session.getQrExpiresAt())
                .build();
    }

    /**
     * Check-in: Tạo AccessLog khi sinh viên ấn nút Check-in trên dashboard
     * Tự động đóng các bản ghi check-in cũ (nếu có) trước khi tạo mới
     */
    @Transactional
    public void checkIn(String sessionToken) {
        KioskQrSessionEntity session = qrSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (session.getStudent() == null) {
            throw new BadRequestException("Session has no student linked");
        }

        // Tự động đóng các bản ghi check-in cũ chưa checkout
        Optional<AccessLog> existing = accessLogRepository.checkInUser(session.getStudent().getId());
        if (existing.isPresent()) {
            AccessLog oldLog = existing.get();
            oldLog.setCheckOutTime(LocalDateTime.now());
            accessLogRepository.save(oldLog);
            logCheckOutActivity(session.getStudent(), oldLog, session.getKiosk().getKioskCode());
            log.info("Auto-closed old check-in for user {}", session.getStudent().getUserCode());
        }

        // Tạo access log mới
        AccessLog accessLog = AccessLog.builder()
                .user(session.getStudent())
                .deviceId(session.getKiosk().getKioskCode())
                .checkInTime(LocalDateTime.now())
                .build();
        accessLog = accessLogRepository.save(accessLog);
        logCheckInActivity(session.getStudent(), session.getKiosk().getKioskCode());

        // Cập nhật session
        session.setAccessLogId(accessLog.getLogId());
        qrSessionRepository.save(session);

        log.info("Check-in for user {} via kiosk", session.getStudent().getUserCode());
        broadcastAccessLog(accessLog, "CHECK_IN");
    }

    /**
     * Check out and close the session
     */
    @Transactional
    public void checkOut(String sessionToken) {
        KioskQrSessionEntity session = qrSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (session.getAccessLogId() != null) {
            AccessLog accessLog = accessLogRepository.findById(session.getAccessLogId())
                    .orElse(null);

            if (accessLog != null && accessLog.getCheckOutTime() == null) {
                accessLog.setCheckOutTime(LocalDateTime.now());
                accessLogRepository.save(accessLog);
                logCheckOutActivity(session.getStudent(), accessLog, session.getKiosk().getKioskCode());
                log.info("Checked out user {}", session.getStudent().getUserCode());
                broadcastAccessLog(accessLog, "CHECK_OUT");
            }
        }

        // Expire the session
        session.setStatus("EXPIRED");
        qrSessionRepository.save(session);
    }

    /**
     * Check-out theo userId (cho mobile app)
     * Tìm AccessLog active và đóng
     */
    @Transactional
    public void checkOutByUserId(UUID userId) {
        Optional<AccessLog> activeLog = accessLogRepository.checkInUser(userId);
        if (activeLog.isEmpty()) {
            throw new BadRequestException("Không tìm thấy phiên check-in đang hoạt động");
        }

        AccessLog accessLog = activeLog.get();
        accessLog.setCheckOutTime(LocalDateTime.now());
        accessLogRepository.save(accessLog);

        User user = userRepository.findById(userId).orElse(null);
        String userCode = user != null ? user.getUserCode() : userId.toString();
        KioskQrAuthService.log.info("Mobile check-out for user {}", userCode);
        broadcastAccessLog(accessLog, "CHECK_OUT");
    }

    /**
     * Kiểm tra user có đang check-in không
     */
    public boolean isUserCheckedIn(UUID userId) {
        return accessLogRepository.checkInUser(userId).isPresent();
    }

    /**
     * Chỉ expire session kiosk, KHÔNG đóng AccessLog
     */
    @Transactional
    public void expireSession(String sessionToken) {
        qrSessionRepository.findBySessionToken(sessionToken).ifPresent(session -> {
            session.setStatus("EXPIRED");
            qrSessionRepository.save(session);
            log.info("Expired kiosk session {}", sessionToken);
        });
    }

    /**
     * Get current active session for kiosk
     */
    public Optional<KioskQrDTO.KioskSessionResponse> getActiveSession(String kioskCode) {
        KioskConfigEntity kiosk = kioskConfigRepository.findByKioskCode(kioskCode)
                .orElse(null);

        if (kiosk == null)
            return Optional.empty();

        return qrSessionRepository.findByKioskIdAndStatusIn(kiosk.getId(), java.util.List.of("ACTIVE", "USED"))
                .stream()
                .filter(s -> s.getStudent() != null)
                .map(session -> {
                    String[] actionHolder = { "CHECK_IN" };
                    LocalDateTime[] checkInTimeHolder = { null };

                    if (session.getAccessLogId() != null) {
                        accessLogRepository.findById(session.getAccessLogId()).ifPresent(log -> {
                            actionHolder[0] = log.getCheckOutTime() == null ? "CHECK_IN" : "CHECK_OUT";
                            checkInTimeHolder[0] = log.getCheckInTime();
                        });
                    }

                    return KioskQrDTO.KioskSessionResponse.builder()
                            .sessionToken(session.getSessionToken())
                            .studentId(session.getStudent().getId())
                            .studentCode(session.getStudent().getUserCode())
                            .studentName(session.getStudent().getFullName())
                            .studentAvatar(session.getStudent().getAvtUrl())
                            .accessLogId(session.getAccessLogId())
                            .currentAction(actionHolder[0])
                            .checkInTime(checkInTimeHolder[0])
                            .expiresAt(session.getQrExpiresAt())
                            .build();
                })
                .findFirst();
    }

    // Helper methods

    private void logCheckInActivity(User user, String kioskCode) {
        try {
            activityService.logActivity(ActivityLogEntity.builder()
                    .userId(user.getId())
                    .activityType(ActivityLogEntity.TYPE_CHECK_IN)
                    .title("Check-in thành công")
                    .description("Bạn đã vào thư viện qua kiosk " + kioskCode)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to log kiosk check-in activity: {}", e.getMessage());
        }
    }

    private void logCheckOutActivity(User user, AccessLog accessLog, String kioskCode) {
        try {
            int durationMinutes = accessLog.getCheckInTime() != null && accessLog.getCheckOutTime() != null
                    ? (int) ChronoUnit.MINUTES.between(accessLog.getCheckInTime(), accessLog.getCheckOutTime())
                    : 0;

            activityService.logActivity(ActivityLogEntity.builder()
                    .userId(user.getId())
                    .activityType(ActivityLogEntity.TYPE_CHECK_OUT)
                    .title("Check-out thành công")
                    .description("Bạn đã rời thư viện qua kiosk " + kioskCode
                            + (durationMinutes > 0 ? " sau " + formatDuration(durationMinutes) : ""))
                    .durationMinutes(durationMinutes > 0 ? durationMinutes : null)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to log kiosk check-out activity: {}", e.getMessage());
        }
    }

    private String formatDuration(int minutes) {
        int hours = minutes ~/ 60;
        int remainingMinutes = minutes % 60;

        if (hours > 0 && remainingMinutes > 0) {
            return hours + " giờ " + remainingMinutes + " phút";
        }
        if (hours > 0) {
            return hours + " giờ";
        }
        return remainingMinutes + " phút";
    }

    private String generateNonce() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateSessionToken() {
        return UUID.randomUUID().toString();
    }

    private String buildQrPayload(String kioskCode, long timestamp, String nonce) {
        return String.format("%s:%d:%d:%s", kioskCode, timestamp, qrTtlSeconds, nonce);
    }

    private void broadcastAccessLog(AccessLog accessLog, String type) {
        try {
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", type);
            wsMessage.put("studentCode", accessLog.getUser().getUserCode());
            wsMessage.put("studentName", accessLog.getUser().getFullName());
            wsMessage.put("userCode", accessLog.getUser().getUserCode());
            wsMessage.put("fullName", accessLog.getUser().getFullName());
            wsMessage.put("userName", accessLog.getUser().getFullName());
            wsMessage.put("deviceId", accessLog.getDeviceId());
            wsMessage.put("time", LocalDateTime.now().toString());

            if ("CHECK_IN".equals(type)) {
                wsMessage.put("checkInTime", accessLog.getCheckInTime().toString());
            } else {
                wsMessage.put("checkOutTime",
                        accessLog.getCheckOutTime() != null ? accessLog.getCheckOutTime().toString()
                                : LocalDateTime.now().toString());
            }

            messagingTemplate.convertAndSend("/topic/access-logs", wsMessage);
            messagingTemplate.convertAndSend("/topic/dashboard",
                    Map.of("type", "CHECKIN_UPDATE", "action", type, "timestamp",
                            java.time.Instant.now().toString()));
        } catch (Exception e) {
            log.warn("Failed to broadcast access log: {}", e.getMessage());
        }
    }

    private String signPayload(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (Exception e) {
            log.error("Error signing payload", e);
            throw new RuntimeException("Error signing payload", e);
        }
    }
}
