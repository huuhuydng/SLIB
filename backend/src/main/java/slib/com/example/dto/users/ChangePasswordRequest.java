package slib.com.example.dto.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordRequest {
    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    @Size(max = 255, message = "Mật khẩu hiện tại không được vượt quá 255 ký tự")
    private String currentPassword;
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(max = 255, message = "Mật khẩu mới không được vượt quá 255 ký tự")
    private String newPassword;
}
