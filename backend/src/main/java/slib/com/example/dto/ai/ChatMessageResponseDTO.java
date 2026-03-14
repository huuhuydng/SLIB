package slib.com.example.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponseDTO {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private Boolean needsReview;
    private Double confidenceScore;
    private LocalDateTime createdAt;
}
