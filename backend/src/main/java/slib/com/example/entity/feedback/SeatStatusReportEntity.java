package slib.com.example.entity.feedback;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import slib.com.example.entity.users.User;
import slib.com.example.entity.zone_config.SeatEntity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Seat Status Report Entity
 * Reports for broken/dirty seats and equipment issues
 */
@Entity
@Table(name = "seat_status_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatStatusReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private SeatEntity seat;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", length = 50, nullable = false)
    private IssueType issueType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public enum IssueType {
        BROKEN,
        DIRTY,
        MISSING_EQUIPMENT,
        OTHER
    }

    public enum ReportStatus {
        PENDING,
        VERIFIED,
        RESOLVED,
        REJECTED
    }
}
