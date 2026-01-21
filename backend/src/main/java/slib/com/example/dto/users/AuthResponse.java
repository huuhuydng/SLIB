package slib.com.example.dto.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String studentCode;
    private String role;
    private String id;
    private String fullName;
    private String email;
    private Long expiresIn;
}