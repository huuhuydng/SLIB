package slib.com.example.dto.news;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryCreateRequest {
    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 50, message = "Tên danh mục không được vượt quá 50 ký tự")
    private String name;

    @Pattern(
            regexp = "^$|^#(?:[0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$",
            message = "Mã màu phải ở dạng #RGB hoặc #RRGGBB")
    @Size(max = 20, message = "Mã màu không được vượt quá 20 ký tự")
    private String colorCode;
}
