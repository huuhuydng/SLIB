package slib.com.example.dto.zone_config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for NFC mapping list (FE-48: Danh sách ghế đã gán thẻ NFC).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NfcMappingResponse {
    private Integer seatId;
    private String seatCode;
    private Integer zoneId;
    private String zoneName;
    private Long areaId;
    private String areaName;
    private boolean hasNfcTag;
    private String maskedNfcUid;
    private LocalDateTime updatedAt;
}
