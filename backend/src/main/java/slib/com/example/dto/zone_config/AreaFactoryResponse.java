package slib.com.example.dto.zone_config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaFactoryResponse {

    private Long factoryId;
    private String factoryName;
    private Long areaId;
    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;
    private Boolean isLocked;
}
