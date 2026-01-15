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
    private Integer positionX;
    private Integer positionY;
    private Integer zoneId;
}
