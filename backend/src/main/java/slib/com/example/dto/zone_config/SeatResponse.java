package slib.com.example.dto.zone_config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import slib.com.example.entity.zone_config.SeatStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {

    private Integer seatId;
    private Integer zoneId;
    private String seatCode;
    private SeatStatus seatStatus; // Computed dynamically from reservations
    private Integer rowNumber;
    private Integer columnNumber;
    private Boolean isActive; // Admin restriction flag
    private String nfcTagUid; // NFC tag UID for seat verification
    private String reservationEndTime; // ISO datetime string, null if AVAILABLE

    // Booker info - populated when seat is BOOKED
    private String bookedByUserName;
    private String bookedByUserCode;
    private String bookedByAvatarUrl;
    private String reservationStartTime;
}