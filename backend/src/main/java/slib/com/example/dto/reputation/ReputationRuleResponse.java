package slib.com.example.dto.reputation;

import lombok.*;
import slib.com.example.entity.reputation.ReputationRuleEntity;

import java.time.LocalDateTime;

/**
 * Response DTO for reputation rules
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReputationRuleResponse {
    
    private Integer id;
    private String ruleCode;
    private String ruleName;
    private String description;
    private Integer points;
    private String ruleType;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReputationRuleResponse fromEntity(ReputationRuleEntity entity) {
        return ReputationRuleResponse.builder()
            .id(entity.getId())
            .ruleCode(entity.getRuleCode())
            .ruleName(entity.getRuleName())
            .description(entity.getDescription())
            .points(entity.getPoints())
            .ruleType(entity.getRuleType().name())
            .isActive(entity.getIsActive())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
