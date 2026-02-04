package slib.com.example.dto.users;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String userCode;
    private String username;
    private String role;
    private boolean isActive;
    private LocalDate dob;
    private String phone;
    private String avtUrl;
    private Boolean passwordChanged;
}