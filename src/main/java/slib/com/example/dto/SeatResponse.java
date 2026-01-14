package slib.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {

    private Integer seatId;
    private Integer zoneId;
    private String seatCode;
    private Boolean isActive;
    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;

    private Integer rowNumber;

    private Integer columnNumber;
}