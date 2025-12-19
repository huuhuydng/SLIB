package slib.com.example.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String studentCode;
    private String role;
    private int reputationScore;
    
    // ĐÃ XÓA: private String avatarUrl;
}