package slib.com.example.dto.reputation;

import lombok.*;
import slib.com.example.entity.reputation.ReputationRuleEntity;

/**
 * Request DTO for creating/updating reputation rules
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReputationRuleRequest {
    
    private String ruleCode;
    private String ruleName;
    private String description;
    private Integer points;
    private ReputationRuleEntity.RuleType ruleType;
    private Boolean isActive;
}
