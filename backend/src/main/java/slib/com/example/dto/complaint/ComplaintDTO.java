package slib.com.example.dto.complaint;

import lombok.Builder;
import lombok.Data;
import slib.com.example.entity.complaint.ComplaintEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ComplaintDTO {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentCode;
    private String studentAvatar;
    private UUID pointTransactionId;
    private String subject;
    private String content;
    private String evidenceUrl;
    private String status;
    private String resolutionNote;
    private String resolvedByName;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public static ComplaintDTO fromEntity(ComplaintEntity entity) {
        return ComplaintDTO.builder()
                .id(entity.getId())
                .studentId(entity.getUser().getId())
                .studentName(entity.getUser().getFullName())
                .studentCode(entity.getUser().getUserCode())
                .studentAvatar(entity.getUser().getAvtUrl())
                .pointTransactionId(entity.getPointTransactionId())
                .subject(entity.getSubject())
                .content(entity.getContent())
                .evidenceUrl(entity.getEvidenceUrl())
                .status(entity.getStatus().name())
                .resolutionNote(entity.getResolutionNote())
                .resolvedByName(entity.getResolvedBy() != null
                        ? entity.getResolvedBy().getFullName()
                        : null)
                .createdAt(entity.getCreatedAt())
                .resolvedAt(entity.getResolvedAt())
                .build();
    }
}
