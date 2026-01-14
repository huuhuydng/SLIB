    package slib.com.example.entity;

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
        @Column(name = "zone_id")
        private Integer zoneId;

        @Column(name = "zone_name", nullable = false, length = 255)
        private String zoneName;

        @Column(name = "zone_des", columnDefinition = "text")
        private String zoneDes;

        @Column(name = "has_power_outlet", nullable = false)
        private Boolean hasPowerOutlet;

        //  Tọa độ kéo thả
        @Column(name = "position_x", nullable = false)
        private Integer positionX;

        @Column(name = "position_y", nullable = false)
        private Integer positionY;

        // Kích thước zone
        @Column(name = "width", nullable = false)
        private Integer width;

        @Column(name = "height", nullable = false)
        private Integer height;

        @ManyToOne
        @JoinColumn(name = "area_id", nullable = false)
        private AreaEntity area;

        //màu và lock
        @Column(name = "is_locked", nullable = false)
        private Boolean isLocked = false;


        //màu và lock
        @Column(name = "color")
        private String color ;
    }