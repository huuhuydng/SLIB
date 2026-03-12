package slib.com.example.entity.kiosk;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity luu tru ma kich hoat ngan cho kiosk.
 * Ma 6 ky tu thay the cho viec paste JWT token dai.
 */
@Entity
@Table(name = "kiosk_activation_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KioskActivationCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "kiosk_id", nullable = false)
    private Integer kioskId;

    @Column(name = "code", length = 6, nullable = false, unique = true)
    private String code;

    @Column(name = "device_token", columnDefinition = "TEXT", nullable = false)
    private String deviceToken;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "used", nullable = false)
    private Boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
