package slib.com.example.dto.kiosk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActivateDeviceRequest {
    @NotBlank(message = "Token không được để trống")
    @Size(max = 4096, message = "Token không được vượt quá 4096 ký tự")
    private String token;
}
