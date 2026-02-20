package slib.com.example.entity.users;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Tracks import job progress and status.
 * Used for async import with Rich Progress pattern.
 */
@Entity
@Table(name = "import_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJob {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "batch_id", nullable = false, unique = true)
    private UUID batchId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "valid_count")
    private Integer validCount;

    @Column(name = "invalid_count")
    private Integer invalidCount;

    @Column(name = "imported_count")
    private Integer importedCount;

    @Column(name = "avatar_count")
    private Integer avatarCount;

    @Column(name = "avatar_uploaded")
    private Integer avatarUploaded;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ImportJobStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(VIETNAM_ZONE);
        updatedAt = LocalDateTime.now(VIETNAM_ZONE);
        if (status == null)
            status = ImportJobStatus.PENDING;
        if (totalRows == null)
            totalRows = 0;
        if (validCount == null)
            validCount = 0;
        if (invalidCount == null)
            invalidCount = 0;
        if (importedCount == null)
            importedCount = 0;
        if (avatarCount == null)
            avatarCount = 0;
        if (avatarUploaded == null)
            avatarUploaded = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(VIETNAM_ZONE);
    }

    public enum ImportJobStatus {
        PENDING, // Job created, waiting to start
        PARSING, // Parsing Excel file
        VALIDATING, // Validating data in staging
        IMPORTING, // Moving to users table
        ENRICHING, // Uploading avatars (background)
        COMPLETED, // All done
        FAILED // Error occurred
    }
}
