package slib.com.example.entity.system;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Backup Schedule Entity
 * Automatic backup configuration
 */
@Entity
@Table(name = "backup_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "schedule_name", length = 100, nullable = false)
    private String scheduleName;

    @Column(name = "cron_expression", length = 50, nullable = false)
    private String cronExpression;

    @Enumerated(EnumType.STRING)
    @Column(name = "backup_type", length = 20, nullable = false)
    private BackupType backupType;

    @Builder.Default
    @Column(name = "retain_days", nullable = false)
    private Integer retainDays = 30;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_backup_at")
    private LocalDateTime lastBackupAt;

    @Column(name = "next_backup_at")
    private LocalDateTime nextBackupAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum BackupType {
        FULL,
        INCREMENTAL
    }
}
