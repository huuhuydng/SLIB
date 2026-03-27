package slib.com.example.dto.feedback;

import lombok.Builder;
import lombok.Data;
import slib.com.example.entity.feedback.SeatStatusReportEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SeatStatusReportResponse {
    private UUID id;
    private UUID reporterId;
    private String reporterName;
    private String reporterCode;
    private String reporterAvatar;
    private Integer seatId;
    private String seatCode;
    private String zoneName;
    private String areaName;
    private String issueType;
    private String issueTypeLabel;
    private String description;
    private String imageUrl;
    private String status;
    private String verifiedByName;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime resolvedAt;

    public static SeatStatusReportResponse fromEntity(SeatStatusReportEntity entity) {
        SeatStatusReportResponseBuilder builder = SeatStatusReportResponse.builder()
                .id(entity.getId())
                .reporterId(entity.getUser().getId())
                .reporterName(entity.getUser().getFullName())
                .reporterCode(entity.getUser().getUserCode())
                .reporterAvatar(entity.getUser().getAvtUrl())
                .seatId(entity.getSeat().getSeatId())
                .seatCode(entity.getSeat().getSeatCode())
                .issueType(entity.getIssueType().name())
                .issueTypeLabel(getIssueTypeLabel(entity.getIssueType()))
                .description(entity.getDescription())
                .imageUrl(entity.getImageUrl())
                .status(entity.getStatus().name())
                .createdAt(entity.getCreatedAt())
                .verifiedAt(entity.getVerifiedAt())
                .resolvedAt(entity.getResolvedAt());

        if (entity.getSeat().getZone() != null) {
            builder.zoneName(entity.getSeat().getZone().getZoneName());
            if (entity.getSeat().getZone().getArea() != null) {
                builder.areaName(entity.getSeat().getZone().getArea().getAreaName());
            }
        }

        if (entity.getVerifiedBy() != null) {
            builder.verifiedByName(entity.getVerifiedBy().getFullName());
        }

        return builder.build();
    }

    private static String getIssueTypeLabel(SeatStatusReportEntity.IssueType type) {
        return switch (type) {
            case BROKEN -> "Ghế hỏng";
            case DIRTY -> "Ghế bẩn";
            case MISSING_EQUIPMENT -> "Thiếu thiết bị";
            case OTHER -> "Vấn đề khác";
        };
    }
}
