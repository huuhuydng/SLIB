package slib.com.example.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    
    @Column(name = "zone_name", nullable = false, length = 100)
    private String zoneName;

    @Column(name = "zone_des", columnDefinition = "TEXT")
    private String zoneDes;

    @Column(name = "has_power_outlet")
    private Boolean hasPowerOutlet;

    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SeatEntity> seats;
}
