package slib.com.example.dto.kiosk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CompleteKioskSessionRequest {
    @NotBlank(message = "Session token không được để trống")
    @Size(max = 4096, message = "Session token không được vượt quá 4096 ký tự")
    private String sessionToken;

    @NotNull(message = "Người dùng không được để trống")
    private UUID userId;
}
