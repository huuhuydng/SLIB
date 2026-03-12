package slib.com.example.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BehaviorSummaryDTO {

    // Tổng quan
    private Long totalStudents;
    private Long totalBehaviors;
    private Long totalBookings;
    private Long totalNoShows;
    private Long totalCancellations;
    private Long totalCheckIns;

    // Tỷ lệ trung bình
    private Double avgNoShowRate;
    private Double avgCancellationRate;
    private Double avgOnTimeRate;

    // Top students
    private List<StudentBehaviorSummaryItem> topNoShowStudents;
    private List<StudentBehaviorSummaryItem> topActiveStudents;
    private List<StudentBehaviorSummaryItem> mostReliableStudents;

    // Xu hướng
    private String trend; // "increasing", "decreasing", "stable"
    private String analyzedPeriod;
    private String analyzedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentBehaviorSummaryItem {
        private UUID userId;
        private Long behaviorCount;
        private Long noShowCount;
        private Long cancellationCount;
        private Double noShowRate;
        private Integer reliabilityScore;
    }
}
