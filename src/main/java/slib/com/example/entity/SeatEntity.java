package slib.com.example.entity;

<<<<<<< HEAD
import jakarta.persistence.*;
import lombok.*;
=======
import java.util.List;

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
import lombok.NoArgsConstructor;
>>>>>>> 9e7981680528c51139544e478f7f9919199c239c

@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
<<<<<<< HEAD
    @Column(name = "seat_id")
=======
    @Column(name = "seat_id", nullable = false, updatable = false)
>>>>>>> 9e7981680528c51139544e478f7f9919199c239c
    private Integer seatId;

    @ManyToOne
    @JoinColumn(name = "zone_id", nullable = false)
    private ZoneEntity zone;

<<<<<<< HEAD
    @Column(name = "seat_code", nullable = false, length = 255)
    private String seatCode;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
=======
    @Column(name = "seat_code", nullable = false, unique = true)
    private String seatCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_status", nullable = false, columnDefinition = "seat_status")
    private SeatStatus seatStatus;
>>>>>>> 9e7981680528c51139544e478f7f9919199c239c

    @Column(name = "position_x", nullable = false)
    private Integer positionX;

    @Column(name = "position_y", nullable = false)
    private Integer positionY;

<<<<<<< HEAD
     // Không cần lưu row_number / column_number là null
    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "column_number", nullable = false)
    private Integer columnNumber; 
    
    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

}
=======
    @OneToMany(mappedBy = "seat")
    private List<ReservationEntity> reservation;
}
>>>>>>> 9e7981680528c51139544e478f7f9919199c239c
