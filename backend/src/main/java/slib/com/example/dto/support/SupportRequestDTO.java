package slib.com.example.dto.support;

import lombok.Builder;
import lombok.Data;
import slib.com.example.entity.support.SupportRequest;
import slib.com.example.entity.support.SupportRequestStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SupportRequestDTO {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentCode;
    private String studentAvatar;
    private String description;
    private List<String> imageUrls;
    private SupportRequestStatus status;
    private String adminResponse;
    private String resolvedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    public static SupportRequestDTO fromEntity(SupportRequest entity) {
        return SupportRequestDTO.builder()
                .id(entity.getId())
                .studentId(entity.getStudent().getId())
                .studentName(entity.getStudent().getFullName())
                .studentCode(entity.getStudent().getUserCode())
                .studentAvatar(entity.getStudent().getAvtUrl())
                .description(entity.getDescription())
                .imageUrls(entity.getImageUrls() != null
                        ? Arrays.asList(entity.getImageUrls())
                        : List.of())
                .status(entity.getStatus())
                .adminResponse(entity.getAdminResponse())
                .resolvedByName(entity.getResolvedBy() != null
                        ? entity.getResolvedBy().getFullName()
                        : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .resolvedAt(entity.getResolvedAt())
                .build();
    }
}
