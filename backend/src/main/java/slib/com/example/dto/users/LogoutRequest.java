package slib.com.example.dto.users;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LogoutRequest {
    @Size(max = 4096, message = "Refresh token không được vượt quá 4096 ký tự")
    private String refreshToken;
}
