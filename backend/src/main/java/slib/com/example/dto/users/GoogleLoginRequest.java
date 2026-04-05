package slib.com.example.dto.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank(message = "ID token không được để trống")
    private String idToken;

    @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự")
    private String fullName;

    @Size(max = 4096, message = "FCM token không được vượt quá 4096 ký tự")
    private String fcmToken;

    @Size(max = 500, message = "Thông tin thiết bị không được vượt quá 500 ký tự")
    private String deviceInfo;
}
