package slib.com.example.dto.zone_config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import slib.com.example.entity.zone_config.SeatStatus;

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