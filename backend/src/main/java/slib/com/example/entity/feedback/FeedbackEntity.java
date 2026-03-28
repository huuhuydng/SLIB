package slib.com.example.entity.feedback;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import slib.com.example.entity.users.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Feedback Entity
 * User feedback after check-out from library
 */
@Entity
@Table(name = "feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "rating")
    private Integer rating; // 1-5 stars

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "category", length = 50)
    private String category; // AI-categorized: FACILITY, SERVICE, GENERAL

    @Column(name = "ai_category_confidence", precision = 3, scale = 2)
    private BigDecimal aiCategoryConfidence;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    private FeedbackStatus status = FeedbackStatus.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public enum FeedbackStatus {
        NEW,
        REVIEWED,
        ACTED
    }
}
