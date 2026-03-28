package slib.com.example.dto.zone_config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import slib.com.example.entity.zone_config.SeatStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatDTO {
    private Integer seatId;
    private String seatCode;
    private SeatStatus seatStatus;
    private Integer rowNumber;
    private Integer columnNumber;
    private Integer zoneId;
    private String nfcTagUid;
    private String reservationEndTime; // ISO datetime string, null if AVAILABLE
}
