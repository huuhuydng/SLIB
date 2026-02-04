package slib.com.example.dto.users;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String studentCode;
    private String role;
    private boolean isActive;
}