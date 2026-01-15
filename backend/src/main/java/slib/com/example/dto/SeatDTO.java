package slib.com.example.dto;

import slib.com.example.entity.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
