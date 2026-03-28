package slib.com.example.entity.kiosk;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import slib.com.example.entity.users.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kiosk QR Session Entity
 * Stores temporary QR session data for kiosk authentication
 */
@Entity
@Table(name = "kiosk_qr_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KioskQrSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kiosk_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private KioskConfigEntity kiosk;

    @Column(name = "session_token", length = 255, nullable = false, unique = true)
    private String sessionToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User student;

    @Column(name = "access_log_id")
    private UUID accessLogId;

    @Column(name = "qr_payload", columnDefinition = "TEXT", nullable = false)
    private String qrPayload;

    @Column(name = "qr_expires_at", nullable = false)
    private LocalDateTime qrExpiresAt;

    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE"; // ACTIVE, USED, EXPIRED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
