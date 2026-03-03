package slib.com.example.entity.kiosk;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Zone Map Entity
 * Represents a zone (area) on the library map
 */
@Entity
@Table(name = "zone_maps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KioskZoneMapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_map_id", nullable = false)
    private KioskLibraryMapEntity libraryMap;

    @Column(name = "zone_name", length = 150, nullable = false)
    private String zoneName;

    @Column(name = "zone_type", length = 100, nullable = false)
    // quiet, discuss, self-study, entrance, shelf, etc.
    private String zoneType;

    @Column(name = "x_position", nullable = false)
    private Integer xPosition; // pixel X

    @Column(name = "y_position", nullable = false)
    private Integer yPosition; // pixel Y

    @Column(name = "width", nullable = false)
    private Integer width;

    @Column(name = "height", nullable = false)
    private Integer height;

    @Column(name = "color_code", length = 20)
    private String colorCode; // e.g. #FF7518 for orange

    @Builder.Default
    @Column(name = "is_interactive", nullable = false)
    private Boolean isInteractive = true; // Can be clicked to view details

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
