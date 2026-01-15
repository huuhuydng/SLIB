package slib.com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibrarianLoginResponse {
    
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private String refreshToken;
    private UserProfileResponse user;
}
