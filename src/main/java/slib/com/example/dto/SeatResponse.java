package slib.com.example.dto;

import slib.com.example.entity.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {

    private Integer seatId;
    private Integer zoneId;
    private String seatCode;
    private SeatStatus seatStatus;
    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;

    private Integer rowNumber;

    private Integer columnNumber;
}