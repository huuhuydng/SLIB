package slib.com.example.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatisticDTO {

    private OverviewDTO overview;
    private BookingAnalysisDTO bookingAnalysis;
    private List<ViolationTypeStatDTO> violationsByType;
    private FeedbackSummaryDTO feedbackSummary;
    private List<ZoneUsageDTO> zoneUsage;
    private List<PeakHourDTO> peakHours;

    // ---- Overview ----
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OverviewDTO {
        private long totalCheckIns;
        private long totalBookings;
        private long totalViolations;
        private long totalFeedbacks;
        private long totalComplaints;
    }

    // ---- Booking Analysis ----
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingAnalysisDTO {
        private long totalBookings;
        private long usedBookings; // CONFIRMED + EXPIRED (actually used)
        private long cancelledBookings; // CANCELLED / CANCEL
        private long expiredNoShow; // bookings that expired without check-in
        private double usedPercent;
        private double cancelledPercent;
        private double expiredPercent;
    }

    // ---- Violation by Type ----
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ViolationTypeStatDTO {
        private String violationType;
        private String label;
        private long count;
    }

    // ---- Feedback Summary ----
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeedbackSummaryDTO {
        private double averageRating;
        private long totalCount;
        private List<RatingDistributionDTO> ratingDistribution;
        private List<RecentFeedbackDTO> recentFeedbacks;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RatingDistributionDTO {
        private int rating;
        private long count;
        private double percent;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentFeedbackDTO {
        private UUID id;
        private String userName;
        private String userCode;
        private String avatarUrl;
        private Integer rating;
        private String content;
        private LocalDateTime createdAt;
    }

    // ---- Zone Usage ----
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ZoneUsageDTO {
        private Integer zoneId;
        private String zoneName;
        private String areaName;
        private long totalSeats;
        private long totalBookings;
        private double usagePercent;
    }

    // ---- Peak Hours ----
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PeakHourDTO {
        private int hour;
        private long count;
    }
}
