package slib.com.example.entity.zone_config;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
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

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "column_number", nullable = false)
    private Integer columnNumber;

    // Admin restriction flag (replaces UNAVAILABLE status)
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Dynamic seat status (AVAILABLE, BOOKED, HOLDING)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "seat_status")
    @Builder.Default
    private SeatStatus seatStatus = SeatStatus.AVAILABLE;

    // NFC tag UID (hashed) for seat verification
    @Column(name = "nfc_tag_uid")
    private String nfcTagUid;

    @OneToMany(mappedBy = "seat", fetch = jakarta.persistence.FetchType.EAGER)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<ReservationEntity> reservation;
}
