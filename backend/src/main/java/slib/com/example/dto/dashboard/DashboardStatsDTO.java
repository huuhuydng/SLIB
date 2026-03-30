package slib.com.example.dto.dashboard;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    // Check-in / Check-out stats
    private long totalCheckInsToday;
    private long totalCheckOutsToday;
    private long currentlyInLibrary;

    // Seat stats
    private long totalSeats;
    private long occupiedSeats;
    private double occupancyRate;

    // Booking stats
    private long totalBookingsToday;
    private long activeBookings;
    private long pendingBookings;

    // Violation stats
    private long violationsToday;
    private long pendingViolations;

    // Support request stats
    private long pendingSupportRequests;
    private long inProgressSupportRequests;

    // User stats
    private long totalUsers;

    // Recent bookings
    private List<RecentBookingDTO> recentBookings;

    // Area occupancy (real data)
    private List<AreaOccupancyDTO> areaOccupancies;

    // Weekly analytics (7 days)
    private List<WeeklyStatsDTO> weeklyStats;

    // Recent violations
    private List<ViolationItemDTO> recentViolations;

    // Top students
    private List<TopStudentDTO> topStudents;

    // Recent support requests
    private List<SupportRequestItemDTO> recentSupportRequests;

    // Recent complaints
    private List<ComplaintItemDTO> recentComplaints;

    // Recent feedbacks
    private List<FeedbackItemDTO> recentFeedbacks;

    // Zone occupancy
    private List<ZoneOccupancyDTO> zoneOccupancies;

    // Server time for frontend display
    private LocalDateTime serverTime;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentBookingDTO {
        private UUID reservationId;
        private String userName;
        private String userCode;
        private String seatCode;
        private String zoneName;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AreaOccupancyDTO {
        private Long areaId;
        private String areaName;
        private long totalSeats;
        private long occupiedSeats;
        private double occupancyPercentage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeeklyStatsDTO {
        private LocalDate date;
        private String dayOfWeek;
        private long checkInCount;
        private long bookingCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ViolationItemDTO {
        private UUID id;
        private String violatorName;
        private String violatorCode;
        private String violationType;
        private String status;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopStudentDTO {
        private UUID userId;
        private String fullName;
        private String userCode;
        private long totalVisits;
        private long totalMinutes;
        private String avatarUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SupportRequestItemDTO {
        private UUID id;
        private String studentName;
        private String studentCode;
        private String description;
        private String status;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComplaintItemDTO {
        private UUID id;
        private String userName;
        private String userCode;
        private String subject;
        private String status;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeedbackItemDTO {
        private UUID id;
        private String userName;
        private String userCode;
        private Integer rating;
        private String content;
        private String status;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ZoneOccupancyDTO {
        private Integer zoneId;
        private String zoneName;
        private String areaName;
        private long totalSeats;
        private long occupiedSeats;
        private double occupancyPercentage;
    }
}
