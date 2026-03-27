package slib.com.example.dto.zone_config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for zone occupancy information
 * Used by mobile app to display zone density colors
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneOccupancyDTO {
    private Integer zoneId;
    private String zoneName;
    private String color;
    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;
    private Boolean hasPowerOutlet;

    private Long totalSeats;
    private Long occupiedSeats;
    private Double occupancyRate; // 0.0 to 1.0
}
