package slib.com.example.entity.zone_config;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id", nullable = false, updatable = false)
    private Integer zoneId;

    @Column(name = "zone_name", nullable = false)
    private String zoneName;

    @Column(name = "zone_des")
    private String zoneDes;

    @Column(name = "position_x", nullable = false)
    private Integer positionX;

    @Column(name = "position_y", nullable = false)
    private Integer positionY;

    @Column(name = "width", nullable = false)
    private Integer width;

    @Column(name = "height", nullable = false)
    private Integer height;

    @ManyToOne
    @JoinColumn(name = "area_id", nullable = false)
    private AreaEntity area;

    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;
}
