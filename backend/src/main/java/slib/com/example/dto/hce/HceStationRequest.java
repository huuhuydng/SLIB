package slib.com.example.dto.hce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HceStationRequest {
    private String deviceId;
    private String deviceName;
    private String deviceType; // ENTRY_GATE, EXIT_GATE, SEAT_READER
    private String location;
    private String status; // ACTIVE, INACTIVE, MAINTENANCE
    private Long areaId;
}
