package slib.com.example.entity.hce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import slib.com.example.entity.zone_config.AreaEntity;

import java.time.LocalDateTime;

/**
 * HCE Device Entity
 * Manages HCE hardware devices for library entry/exit gates and seat readers
 */
@Entity
@Table(name = "hce_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HceDeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "device_id", length = 50, unique = true, nullable = false)
    private String deviceId;

    @Column(name = "device_name", length = 100, nullable = false)
    private String deviceName;

    @Column(name = "location", length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 20, nullable = false)
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    private DeviceStatus status = DeviceStatus.ACTIVE;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private AreaEntity area;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DeviceType {
        ENTRY_GATE,
        EXIT_GATE,
        SEAT_READER
    }

    public enum DeviceStatus {
        ACTIVE,
        INACTIVE,
        MAINTENANCE
    }
}
