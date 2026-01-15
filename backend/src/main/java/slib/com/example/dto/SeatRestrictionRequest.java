package slib.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatRestrictionRequest {
    private String seatCode;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
