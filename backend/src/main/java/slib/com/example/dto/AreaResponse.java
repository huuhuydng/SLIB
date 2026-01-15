package slib.com.example.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaResponse {

    private Long areaId;

    @NotBlank(message = "Tên khu vực không được để trống")
    private String areaName;

    @NotNull(message = "Chiều rộng không được để trống")
    @Min(value = 1, message = "Chiều rộng phải lớn hơn 0")
    private Integer width;

    @NotNull(message = "Chiều cao không được để trống")
    @Min(value = 1, message = "Chiều cao phải lớn hơn 0")
    private Integer height;

    @Min(value = 0, message = "Vị trí X không được âm")
    private Integer positionX;

    @Min(value = 0, message = "Vị trí Y không được âm")
    private Integer positionY;

    private Boolean isActive;

    private Boolean locked;
}