package slib.com.example.entity.kiosk;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Kiosk Config Entity
 * Stores configuration for kiosk devices
 */
@Entity
@Table(name = "kiosk_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KioskConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "kiosk_code", length = 50, nullable = false, unique = true)
    private String kioskCode;

    @Column(name = "kiosk_name", length = 200, nullable = false)
    private String kioskName;

    @Column(name = "kiosk_type", length = 50, nullable = false)
    private String kioskType; // 'INTERACTIVE' or 'MONITORING'

    @Column(name = "location")
    private String location;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "qr_secret_key")
    private String qrSecretKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
