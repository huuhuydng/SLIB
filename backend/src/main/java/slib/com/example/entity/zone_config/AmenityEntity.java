package slib.com.example.entity.zone_config;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zone_amenities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmenityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "amenity_id")
    private Integer amenityId;

    @ManyToOne
    @JoinColumn(name = "zone_id", nullable = false)
    private ZoneEntity zone;

    @Column(name = "amenity_name", nullable = false, length = 100)
    private String amenityName;
}