package slib.com.example.dto.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminResetPasswordRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;
}
