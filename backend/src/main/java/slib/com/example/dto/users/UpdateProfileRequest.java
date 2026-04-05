package slib.com.example.dto.users;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự")
    private String fullName;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phone;

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "Ngày sinh phải đúng định dạng yyyy-MM-dd")
    private String dob; // yyyy-MM-dd format
}
