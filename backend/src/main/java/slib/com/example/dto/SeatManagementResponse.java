package slib.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatManagementResponse {
    private List<SeatResponse> seats;
    private SeatOccupancyStats stats;
    private List<String> restrictedSeatCodes;
    private String queryMode; // "REAL_TIME" or "TIME_RANGE"
    private LocalDateTime queryStartTime;
    private LocalDateTime queryEndTime;
    
    // Constructor cũ để backward compatible
    public SeatManagementResponse(List<SeatResponse> seats, SeatOccupancyStats stats, List<String> restrictedSeatCodes) {
        this.seats = seats;
        this.stats = stats;
        this.restrictedSeatCodes = restrictedSeatCodes;
    }
}
