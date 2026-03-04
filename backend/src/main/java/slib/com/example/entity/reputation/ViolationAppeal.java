package slib.com.example.entity.reputation;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import slib.com.example.entity.users.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Violation Appeal Entity
 * Stores appeals from students against violation records
 */
@Entity
@Table(name = "violation_appeals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViolationAppeal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "violation_id", nullable = false)
    private ViolationRecord violation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "appeal_reason", columnDefinition = "TEXT", nullable = false)
    private String appealReason; // Lí do khiếu nại

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private AppealStatus status = AppealStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy; // Thủ thư xử lý khiếu nại

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes; // Ghi chú của thủ thư khi xử lý

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum AppealStatus {
        PENDING,   // Đang chờ xử lý
        APPROVED,  // Chấp nhận khiếu nại
        REJECTED   // Từ chối khiếu nại
    }
}
