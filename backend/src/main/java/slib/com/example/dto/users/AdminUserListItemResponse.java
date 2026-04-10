package slib.com.example.dto.users;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record AdminUserListItemResponse(
        UUID id,
        String fullName,
        String email,
        String userCode,
        String role,
        Boolean isActive,
        String lockReason,
        Integer reputationScore,
        String avtUrl,
        Boolean passwordChanged,
        String phone,
        LocalDate dob,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
