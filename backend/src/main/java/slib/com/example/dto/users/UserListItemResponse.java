package slib.com.example.dto.users;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record UserListItemResponse(
        UUID id,
        String fullName,
        String email,
        String userCode,
        String role,
        Boolean isActive,
        String avtUrl,
        LocalDateTime createdAt
) {
}
