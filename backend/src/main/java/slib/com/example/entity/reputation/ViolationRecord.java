package slib.com.example.entity.reputation;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import slib.com.example.entity.users.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Violation Record Entity
 * Stores individual violation records for students
 */
@Entity
@Table(name = "violation_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViolationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // Thủ thư tạo vi phạm

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private ReputationRuleEntity rule;

    @Column(name = "violation_reason", columnDefinition = "TEXT", nullable = false)
    private String violationReason;

    @Column(name = "penalty_points", nullable = false)
    private Integer penaltyPoints; // Số điểm bị trừ (số dương)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private ViolationStatus status = ViolationStatus.ACTIVE;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // Ghi chú thêm

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ViolationStatus {
        ACTIVE,      // Vi phạm đang hiệu lực
        APPEALED,    // Đang được khiếu nại
        DISMISSED,   // Đã bị bác bỏ khiếu nại
        CANCELLED    // Đã hủy do khiếu nại thành công
    }
}
