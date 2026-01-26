package slib.com.example.entity.users;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfile {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Column(name = "reputation_score", nullable = false)
    @Builder.Default
    private Integer reputationScore = 100;

    @Column(name = "total_study_hours", nullable = false)
    @Builder.Default
    private Double totalStudyHours = 0.0;

    @Column(name = "violation_count", nullable = false)
    @Builder.Default
    private Integer violationCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
