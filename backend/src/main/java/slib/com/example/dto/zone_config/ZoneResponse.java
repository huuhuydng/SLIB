package slib.com.example.dto.zone_config;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZoneResponse {

    private Integer zoneId;
    private String zoneName;
    private String zoneDes;

    @Min(value = 0, message = "Vị trí X không được âm")
    private Integer positionX;
    @Min(value = 0, message = "Vị trí Y không được âm")
    private Integer positionY;

    @Min(value = 1, message = "Chiều rộng khu vực phải lớn hơn 0")
    private Integer width;
    @Min(value = 1, message = "Chiều cao khu vực phải lớn hơn 0")
    private Integer height;

    private Long areaId;

    private Boolean isLocked;

    private List<AmenityResponse> amenities = new ArrayList<>();
}
