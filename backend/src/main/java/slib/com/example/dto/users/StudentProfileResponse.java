package slib.com.example.dto.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import slib.com.example.entity.users.StudentProfile;

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

    public static StudentProfileResponse fromEntity(StudentProfile profile) {
        return StudentProfileResponse.builder()
                .userId(profile.getUserId())
                .reputationScore(profile.getReputationScore())
                .totalStudyHours(profile.getTotalStudyHours())
                .violationCount(profile.getViolationCount())
                .totalBookings(0L) // Default, will be set by service
                .build();
    }

    public static StudentProfileResponse fromEntity(StudentProfile profile, long bookingCount) {
        return StudentProfileResponse.builder()
                .userId(profile.getUserId())
                .reputationScore(profile.getReputationScore())
                .totalStudyHours(profile.getTotalStudyHours())
                .violationCount(profile.getViolationCount())
                .totalBookings(bookingCount)
                .build();
    }
}
