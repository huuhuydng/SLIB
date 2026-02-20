package slib.com.example.dto.feedback;

import lombok.Builder;
import lombok.Data;
import slib.com.example.entity.feedback.FeedbackEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FeedbackDTO {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentCode;
    private String studentAvatar;
    private UUID reservationId;
    private Integer rating;
    private String content;
    private String category;
    private String status;
    private String reviewedByName;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public static FeedbackDTO fromEntity(FeedbackEntity entity) {
        return FeedbackDTO.builder()
                .id(entity.getId())
                .studentId(entity.getUser().getId())
                .studentName(entity.getUser().getFullName())
                .studentCode(entity.getUser().getUserCode())
                .studentAvatar(entity.getUser().getAvtUrl())
                .reservationId(entity.getReservationId())
                .rating(entity.getRating())
                .content(entity.getContent())
                .category(entity.getCategory())
                .status(entity.getStatus().name())
                .reviewedByName(entity.getReviewedBy() != null
                        ? entity.getReviewedBy().getFullName()
                        : null)
                .createdAt(entity.getCreatedAt())
                .reviewedAt(entity.getReviewedAt())
                .build();
    }
}
