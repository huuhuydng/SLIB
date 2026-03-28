package slib.com.example.entity.system;

import jakarta.persistence.*;
import lombok.*;
import slib.com.example.entity.users.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Backup History Entity
 * Records of completed backups
 */
@Entity
@Table(name = "backup_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private BackupScheduleEntity schedule;

    @Column(name = "file_path", columnDefinition = "TEXT", nullable = false)
    private String filePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private BackupStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public enum BackupStatus {
        SUCCESS,
        FAILED
    }
}
