package slib.com.example.dto.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import slib.com.example.entity.users.Role;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImportUserRequest {
    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự")
    private String fullName;

    @NotBlank(message = "Mã người dùng không được để trống")
    @Size(max = 20, message = "Mã người dùng không được vượt quá 20 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Mã người dùng chỉ được chứa chữ cái, số, dấu chấm, gạch ngang hoặc gạch dưới")
    private String userCode;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phone;

    private LocalDate dob;
    private Role role;
    private String avtUrl; // Avatar URL from Cloudinary
}
