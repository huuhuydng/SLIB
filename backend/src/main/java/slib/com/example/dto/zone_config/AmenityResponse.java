package slib.com.example.dto.zone_config;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmenityResponse {

    private Integer amenityId;
    private Integer zoneId;
    @NotBlank(message = "Tên tiện ích không được để trống")
    private String amenityName;
}
