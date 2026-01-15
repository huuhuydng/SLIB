package slib.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatRestrictionResponse {
    private UUID restrictionId;
    private Integer seatId;
    private String seatCode;
    private String restrictedByName;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
