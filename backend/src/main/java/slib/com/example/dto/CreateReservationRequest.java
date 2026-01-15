package slib.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReservationRequest {
    private String seatCode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
