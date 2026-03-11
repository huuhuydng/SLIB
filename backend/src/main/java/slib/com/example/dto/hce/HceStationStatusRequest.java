package slib.com.example.dto.hce;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HceStationStatusRequest {
    private String status; // ACTIVE, INACTIVE, MAINTENANCE
}
