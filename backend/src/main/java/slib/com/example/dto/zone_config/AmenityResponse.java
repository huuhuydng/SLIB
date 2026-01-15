package slib.com.example.dto.zone_config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmenityResponse {

    private Integer amenityId;
    private Integer zoneId;
    private String amenityName;
}