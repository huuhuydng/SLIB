package slib.com.example.dto.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import slib.com.example.entity.users.StudentProfile;
import slib.com.example.entity.users.User;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfileResponse {

    private UUID userId;
    private Integer reputationScore;
    private Double totalStudyHours;
    private Integer violationCount;
    private Long totalBookings;

    // User info fields
    private String userCode;
    private String fullName;
    private String email;
    private String phone;
    private String dob;
    private String role;
    private String avtUrl;

    public static StudentProfileResponse fromEntity(StudentProfile profile) {
        StudentProfileResponseBuilder builder = StudentProfileResponse.builder()
                .userId(profile.getUserId())
                .reputationScore(profile.getReputationScore())
                .totalStudyHours(profile.getTotalStudyHours())
                .violationCount(profile.getViolationCount())
                .totalBookings(0L);

        // Add user info if available
        if (profile.getUser() != null) {
            User user = profile.getUser();
            builder.userCode(user.getUserCode())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .dob(user.getDob() != null ? user.getDob().toString() : null)
                    .role(user.getRole() != null ? user.getRole().name() : null)
                    .avtUrl(user.getAvtUrl());
        }

        return builder.build();
    }

    public static StudentProfileResponse fromEntity(StudentProfile profile, long bookingCount) {
        StudentProfileResponseBuilder builder = StudentProfileResponse.builder()
                .userId(profile.getUserId())
                .reputationScore(profile.getReputationScore())
                .totalStudyHours(profile.getTotalStudyHours())
                .violationCount(profile.getViolationCount())
                .totalBookings(bookingCount);

        // Add user info if available
        if (profile.getUser() != null) {
            User user = profile.getUser();
            builder.userCode(user.getUserCode())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .dob(user.getDob() != null ? user.getDob().toString() : null)
                    .role(user.getRole() != null ? user.getRole().name() : null)
                    .avtUrl(user.getAvtUrl());
        }

        return builder.build();
    }
}
