package slib.com.example.service.kiosk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for QR Authentication
 */
public class KioskQrDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QrGenerateRequest {
        private String kioskCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QrGenerateResponse {
        private String qrPayload;
        private LocalDateTime expiresAt;
        private Integer kioskId;
        private String kioskCode;
        private Integer ttlSeconds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QrValidateRequest {
        private String qrPayload;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QrValidateResponse {
        private Boolean success;
        private String message;
        private String sessionToken;
        private String kioskName;
        private String studentCode;
        private String studentName;
        private UUID accessLogId;
        private String action; // CHECK_IN or CHECK_OUT
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KioskSessionResponse {
        private String sessionToken;
        private UUID studentId;
        private String studentCode;
        private String studentName;
        private String studentAvatar;
        private UUID accessLogId;
        private String currentAction; // CHECK_IN or CHECK_OUT or NONE
        private LocalDateTime checkInTime;
        private LocalDateTime expiresAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KioskSessionUpdateMessage {
        private String sessionToken;
        private UUID studentId;
        private String studentCode;
        private String studentName;
        private String studentAvatar;
        private String action; // SESSION_CREATED, CHECK_IN, CHECK_OUT, SESSION_EXPIRED
        private UUID accessLogId;
        private String zoneName;
        private String seatCode;
        private LocalDateTime timestamp;
    }
}
