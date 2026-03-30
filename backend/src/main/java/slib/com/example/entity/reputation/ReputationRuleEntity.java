package slib.com.example.entity.reputation;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Reputation Rule Entity
 * Stores penalty/reward rules for the reputation system
 */
@Entity
@Table(name = "reputation_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReputationRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "rule_code", length = 50, unique = true, nullable = false)
    private String ruleCode;

    @Column(name = "rule_name", length = 200, nullable = false)
    private String ruleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "points", nullable = false)
    private Integer points; // negative = penalty, positive = reward

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", length = 20, nullable = false)
    private RuleType ruleType;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum RuleType {
        PENALTY,
        REWARD
    }
}
