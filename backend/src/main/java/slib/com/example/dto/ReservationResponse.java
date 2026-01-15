package slib.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private UUID reservationId;
    private UUID userId;
    private String userFullName;
    private String studentCode;
    private Integer seatId;
    private String seatCode;
    private String zoneName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime createdAt;
}
