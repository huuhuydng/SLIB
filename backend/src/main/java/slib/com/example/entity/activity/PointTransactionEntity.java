package slib.com.example.entity.activity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "point_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "transaction_type", nullable = false, length = 50)
    private String transactionType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "balance_after")
    private Integer balanceAfter;

    @Column(name = "activity_log_id")
    private UUID activityLogId;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
    }

    // Transaction types constants
    public static final String TYPE_REWARD = "REWARD";
    public static final String TYPE_PENALTY = "PENALTY";
    public static final String TYPE_WEEKLY_BONUS = "WEEKLY_BONUS";
    public static final String TYPE_NO_SHOW_PENALTY = "NO_SHOW_PENALTY";
    public static final String TYPE_CHECK_OUT_LATE_PENALTY = "CHECK_OUT_LATE_PENALTY";
}
