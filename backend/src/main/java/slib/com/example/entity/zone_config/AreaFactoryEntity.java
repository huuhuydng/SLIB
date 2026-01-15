package slib.com.example.entity.zone_config;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "area_factories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AreaFactoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "factory_id")
    private Long factoryId;

    @Column(name = "factory_name", nullable = false)
    private String factoryName;

    @Column(name = "position_x", nullable = false)
    private Integer positionX;

    @Column(name = "position_y", nullable = false)
    private Integer positionY;

    @Column(name = "width", nullable = false)
    private Integer width;

    @Column(name = "height", nullable = false)
    private Integer height;

    @Column(name = "color")
    private String color;

    @ManyToOne
    @JoinColumn(name = "area_id", nullable = false)
    private AreaEntity area;
}
