package slib.com.example.dto.kiosk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ActivateCodeRequest {
    @NotBlank(message = "Mã kích hoạt không được để trống")
    @Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "Mã kích hoạt phải gồm đúng 6 ký tự chữ hoặc số")
    private String code;
}
