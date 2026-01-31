package slib.com.example.entity.feedback;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import slib.com.example.entity.users.User;
import slib.com.example.entity.zone_config.SeatEntity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Seat Violation Report Entity
 * Reports for seat rule violations by students
 */
@Entity
@Table(name = "seat_violation_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatViolationReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "violator_id")
    private User violator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private SeatEntity seat;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", length = 50, nullable = false)
    private ViolationType violationType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "evidence_url", columnDefinition = "TEXT")
    private String evidenceUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "point_deducted")
    private Integer pointDeducted;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    public enum ViolationType {
        UNAUTHORIZED_USE,
        LEFT_BELONGINGS,
        NOISE,
        OTHER
    }

    public enum ReportStatus {
        PENDING,
        VERIFIED,
        RESOLVED,
        REJECTED
    }
}
