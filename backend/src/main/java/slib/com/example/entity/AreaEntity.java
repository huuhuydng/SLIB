package slib.com.example.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "areas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AreaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "area_id")
    private Long areaId;

    @Column(name = "area_name", nullable = false, unique = true)
    private String areaName;

    @Column(name = "width", nullable = false)
    private Integer width;

    @Column(name = "height", nullable = false)
    private Integer height;

    @Column(name = "position_x")
    private Integer positionX;

    @Column(name = "position_y")
    private Integer positionY;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "locked", nullable = false)
    private Boolean locked;
}
