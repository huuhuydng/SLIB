package slib.com.example.entity.system;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import slib.com.example.entity.users.User;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * System Log Entity
 * Records operational events: system errors, performance warnings,
 * background job logs, integration events, and admin audit trail.
 */
@Entity
@Table(name = "system_logs", indexes = {
        @Index(name = "idx_system_logs_created", columnList = "created_at DESC"),
        @Index(name = "idx_system_logs_user", columnList = "user_id"),
        @Index(name = "idx_system_logs_level", columnList = "level"),
        @Index(name = "idx_system_logs_category", columnList = "category"),
        @Index(name = "idx_system_logs_service", columnList = "service"),
        @Index(name = "idx_system_logs_source", columnList = "source")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 10)
    @Builder.Default
    private LogLevel level = LogLevel.INFO;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    @Builder.Default
    private LogCategory category = LogCategory.AUDIT;

    @Column(name = "service", length = 100)
    private String service;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    // === User context (audit trail) ===

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Snapshot of actor email — preserved even if user is deleted */
    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    // === Event context ===

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20)
    @Builder.Default
    private LogSource source = LogSource.SYSTEM;

    /** Reference to related entity (reservationId, reportId, notificationId, etc.) */
    @Column(name = "reference_id")
    private UUID referenceId;

    // === Legacy audit fields (backward compatible) ===

    @Column(name = "action", length = 100)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // =========================================
    // === ENUMS ===
    // =========================================

    public enum LogLevel {
        ERROR, WARN, INFO, DEBUG
    }

    public enum LogCategory {
        SYSTEM_ERROR,
        PERFORMANCE,
        BACKGROUND_JOB,
        INTEGRATION,
        AUDIT
    }

    public enum LogSource {
        WEB,
        MOBILE,
        HCE,
        SCHEDULER,
        SYSTEM
    }

    /** Derived: true if no user is associated (system-generated event) */
    public boolean isCreatedBySystem() {
        return user == null;
    }
}
