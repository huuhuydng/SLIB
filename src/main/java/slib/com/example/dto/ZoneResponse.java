package slib.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZoneResponse {

    private Integer zoneId;
    private String zoneName;
    private String zoneDes;
    private Boolean hasPowerOutlet;

    private Integer positionX;
    private Integer positionY;

    private Integer width;
    private Integer height;

    private Long areaId;

    private Boolean isLocked;
    private String color;
}
