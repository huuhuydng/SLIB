package slib.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaResponse {

    private Long areaId;
    private String areaName;

    private Integer width;
    private Integer height;

    private Integer positionX;
    private Integer positionY;

    private Boolean isActive;

    private Boolean locked;
}
