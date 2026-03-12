package slib.com.example.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentBehaviorAnalyticsDTO {

    private UUID userId;

    // Thống kê tổng quan
    private Long totalBookings;
    private Long totalCheckIns;
    private Long totalCancellations;
    private Long totalViolations;

    // Tỷ lệ
    private Double noShowRate;           // Tỷ lệ bỏ chỗ
    private Double onTimeCheckInRate;    // Tỷ lệ check-in đúng giờ
    private Double onTimeCheckOutRate;   // Tỷ lệ check-out đúng giờ
    private Double cancellationRate;     // Tỷ lệ hủy

    // Điểm uy tín
    private Integer currentReputation;
    private Integer totalPointsEarned;
    private Integer totalPointsDeducted;

    // Chi tiết theo loại hành vi
    private Map<String, Long> behaviorCounts;

    // Insights từ AI (có thể gọi AI service)
    private List<String> insights;

    // Score độ tin cậy (0-100)
    private Integer reliabilityScore;

    // Thời gian phân tích
    private String analyzedPeriod;
    private String analyzedAt;
}
