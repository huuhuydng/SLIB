package slib.com.example.dto.feedback;

import lombok.Builder;
import lombok.Data;
import slib.com.example.entity.feedback.SeatViolationReportEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ViolationReportResponse {

    private UUID id;

    // Reporter info
    private UUID reporterId;
    private String reporterName;
    private String reporterCode;
    private String reporterAvatar;

    // Violator info (co the null)
    private UUID violatorId;
    private String violatorName;
    private String violatorCode;
    private String violatorAvatar;

    // Seat info
    private Integer seatId;
    private String seatCode;
    private String zoneName;
    private String areaName;

    // Report details
    private String violationType;
    private String violationTypeLabel;
    private String description;
    private String evidenceUrl;
    private String status;

    // Verification info
    private String verifiedByName;
    private Integer pointDeducted;

    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;

    // Appeal/complaint info (populated separately)
    private String appealStatus;
    private String appealResolutionNote;

    public static ViolationReportResponse fromEntity(SeatViolationReportEntity entity) {
        ViolationReportResponseBuilder builder = ViolationReportResponse.builder()
                .id(entity.getId())
                .reporterId(entity.getReporter().getId())
                .reporterName(entity.getReporter().getFullName())
                .reporterCode(entity.getReporter().getUserCode())
                .reporterAvatar(entity.getReporter().getAvtUrl())
                .seatId(entity.getSeat().getSeatId())
                .seatCode(entity.getSeat().getSeatCode())
                .violationType(entity.getViolationType().name())
                .violationTypeLabel(getViolationTypeLabel(entity.getViolationType()))
                .description(entity.getDescription())
                .evidenceUrl(entity.getEvidenceUrl())
                .status(entity.getStatus().name())
                .pointDeducted(entity.getPointDeducted())
                .createdAt(entity.getCreatedAt())
                .verifiedAt(entity.getVerifiedAt());

        // Zone & Area info
        if (entity.getSeat().getZone() != null) {
            builder.zoneName(entity.getSeat().getZone().getZoneName());
            if (entity.getSeat().getZone().getArea() != null) {
                builder.areaName(entity.getSeat().getZone().getArea().getAreaName());
            }
        }

        // Violator info
        if (entity.getViolator() != null) {
            builder.violatorId(entity.getViolator().getId())
                    .violatorName(entity.getViolator().getFullName())
                    .violatorCode(entity.getViolator().getUserCode())
                    .violatorAvatar(entity.getViolator().getAvtUrl());
        }

        // Verified by
        if (entity.getVerifiedBy() != null) {
            builder.verifiedByName(entity.getVerifiedBy().getFullName());
        }

        return builder.build();
    }

    private static String getViolationTypeLabel(SeatViolationReportEntity.ViolationType type) {
        return switch (type) {
            case UNAUTHORIZED_USE -> "Sử dụng ghế không đúng";
            case LEFT_BELONGINGS -> "Để đồ giữ chỗ";
            case NOISE -> "Gây ồn ào";
            case FEET_ON_SEAT -> "Gác chân lên ghế/bàn";
            case FOOD_DRINK -> "Ăn uống trong thư viện";
            case SLEEPING -> "Ngủ tại chỗ ngồi";
            case OTHER -> "Khác";
        };
    }
}
