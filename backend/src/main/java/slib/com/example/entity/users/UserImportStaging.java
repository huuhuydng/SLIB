package slib.com.example.entity.users;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Staging table entity for bulk import.
 * Data is validated here before being moved to users table.
 */
@Entity
@Table(name = "user_import_staging")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserImportStaging {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Column(name = "user_code")
    private String userCode;

    @Column(name = "email")
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "role")
    private String role;

    @Column(name = "avt_url", columnDefinition = "TEXT")
    private String avtUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StagingStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null)
            status = StagingStatus.PENDING;
    }

    public enum StagingStatus {
        PENDING, // Not yet validated
        VALID, // Passed validation
        INVALID, // Failed validation
        IMPORTED // Successfully moved to users table
    }
}
