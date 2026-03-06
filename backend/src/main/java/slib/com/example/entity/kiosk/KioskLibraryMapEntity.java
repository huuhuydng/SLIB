package slib.com.example.entity.kiosk;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Library Map Entity
 * Stores the library layout image and zone configurations
 */
@Entity
@Table(name = "library_maps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KioskLibraryMapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "map_name", length = 200, nullable = false)
    private String mapName;

    @Column(name = "map_image_url", columnDefinition = "TEXT", nullable = false)
    private String mapImageUrl;

    @Column(name = "public_id", length = 255)
    private String publicId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "libraryMap", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KioskZoneMapEntity> zones;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
