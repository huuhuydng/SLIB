package slib.com.example.dto.hce;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO tổng hợp chi tiết sinh viên cho thủ thư (chỉ đọc)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDetailDTO {

    // --- Thông tin cơ bản ---
    private UUID id;
    private String fullName;
    private String email;
    private String userCode;
    private String phone;
    private LocalDate dob;
    private String avtUrl;
    private String role;
    private Boolean isActive;
    private LocalDateTime createdAt;

    // --- Thống kê ---
    private long totalCheckIns;
    private long totalStudyMinutes;
    private long totalBookings;
    private int reputationScore;
    private int violationCount;

    // --- Lịch sử hoạt động gần đây ---
    private List<ActivityItem> recentActivities;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityItem {
        private UUID id;
        private String activityType;
        private String title;
        private String description;
        private String seatCode;
        private String zoneName;
        private Integer durationMinutes;
        private ZonedDateTime createdAt;
    }
}
