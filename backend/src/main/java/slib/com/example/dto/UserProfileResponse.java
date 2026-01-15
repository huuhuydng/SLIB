package slib.com.example.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private UUID supabaseUid;
    private String email;
    private String fullName;
    private String studentCode;
    private String role;
    private int reputationScore;
    private boolean isActive;
}