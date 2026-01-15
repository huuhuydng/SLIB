package slib.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatOccupancyStats {
    private Integer totalSeats;
    private Integer occupiedSeats;
    private Integer restrictedSeats;
    private Integer availableSeats;
    private Double occupancyRate;
}
