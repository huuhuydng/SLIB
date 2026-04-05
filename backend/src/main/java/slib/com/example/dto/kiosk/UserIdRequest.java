package slib.com.example.dto.kiosk;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UserIdRequest {
    @NotNull(message = "Người dùng không được để trống")
    private UUID userId;
}
