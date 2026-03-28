package slib.com.example.dto.zone_config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for single seat NFC detail (FE-49: Chi tiết cấu hình NFC của
 * ghế).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NfcInfoResponse {
    private Integer seatId;
    private String seatCode;
    private Integer zoneId;
    private String zoneName;
    private Long areaId;
    private String areaName;
    private boolean nfcMapped;
    private String nfcUidMasked;
    private LocalDateTime lastUpdated;
}
