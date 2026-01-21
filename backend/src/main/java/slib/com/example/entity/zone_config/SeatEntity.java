package slib.com.example.entity.zone_config;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import slib.com.example.entity.booking.ReservationEntity;

@Entity
@Table(name = "seats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id", nullable = false, updatable = false)
    private Integer seatId;

    @ManyToOne
    @JoinColumn(name = "zone_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private ZoneEntity zone;

    @Column(name = "seat_code", nullable = false)
    private String seatCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_status", nullable = false, columnDefinition = "seat_status")
    private SeatStatus seatStatus;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "column_number", nullable = false)
    private Integer columnNumber;

    // Temporary Hold fields
    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "held_by_user")
    private UUID heldByUser;

    @OneToMany(mappedBy = "seat")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<ReservationEntity> reservation;
}
