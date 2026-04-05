package slib.com.example.dto.kiosk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SessionTokenRequest {
    @NotBlank(message = "Session token không được để trống")
    @Size(max = 4096, message = "Session token không được vượt quá 4096 ký tự")
    private String sessionToken;
}
