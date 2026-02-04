package slib.com.example.dto.users;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    private String email; // backward compatible
    private String identifier; // username hoặc email hoặc MSSV
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
