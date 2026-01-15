package slib.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {
    private Integer seatId;
    private String seatCode;
    private String zoneName;
    private Integer zoneId;
    private String status;
    private Boolean isOccupied;
    private Integer positionX;
    private Integer positionY;
}
