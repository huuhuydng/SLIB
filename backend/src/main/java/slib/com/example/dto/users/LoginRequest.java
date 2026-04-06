package slib.com.example.dto.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email; // backward compatible
    @Size(max = 255, message = "Thông tin đăng nhập không được vượt quá 255 ký tự")
    private String identifier; // username hoặc email hoặc MSSV
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(max = 255, message = "Mật khẩu không được vượt quá 255 ký tự")
    private String password;

    /**
     * Lấy identifier để tìm user
     * Ưu tiên identifier, nếu không có thì dùng email
     */
    public String getLoginIdentifier() {
        if (identifier != null && !identifier.isEmpty()) {
            return identifier;
        }
        return email;
    }
}
