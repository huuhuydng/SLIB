package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import slib.com.example.dto.analytics.BehaviorSummaryDTO;
import slib.com.example.dto.analytics.StudentBehaviorAnalyticsDTO;
import slib.com.example.entity.analytics.StudentBehaviorEntity;
import slib.com.example.repository.StudentBehaviorRepository;
import slib.com.example.repository.StudentProfileRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentBehaviorService {

    private final StudentBehaviorRepository behaviorRepository;
    private final StudentProfileRepository studentProfileRepository;

    /**
     * Lấy tổng hợp behavior của tất cả sinh viên (cho AI analytics)
     */
    public BehaviorSummaryDTO getBehaviorSummary(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);

        // Lấy stats cho tất cả sinh viên
        List<Object[]> allStats = behaviorRepository.getBehaviorStatsForAllStudents(fromDate);

        // Lấy top no-show
        List<Object[]> noShowStats = behaviorRepository.getStudentsWithMostNoShows(fromDate);

        // Lấy top active
        List<Object[]> activeStats = behaviorRepository.getMostActiveStudents(fromDate);

        // Tính toán
        long totalStudents = allStats.size();
        long totalBehaviors = 0;
        long totalNoShows = 0;
        long totalCancellations = 0;

        for (Object[] stat : allStats) {
            totalBehaviors += ((Number) stat[1]).longValue();
            totalNoShows += ((Number) stat[2]).longValue();
            totalCancellations += ((Number) stat[3]).longValue();
        }

        // Tính tỷ lệ trung bình
        double avgNoShowRate = totalStudents > 0 ? (double) totalNoShows / Math.max(1, totalBehaviors) : 0;
        double avgCancellationRate = totalStudents > 0 ? (double) totalCancellations / Math.max(1, totalBehaviors) : 0;

        // Build top lists
        List<BehaviorSummaryDTO.StudentBehaviorSummaryItem> topNoShowStudents = new ArrayList<>();
        for (Object[] stat : noShowStats.subList(0, Math.min(10, noShowStats.size()))) {
            topNoShowStudents.add(BehaviorSummaryDTO.StudentBehaviorSummaryItem.builder()
                    .userId((UUID) stat[0])
                    .noShowCount(((Number) stat[1]).longValue())
                    .build());
        }

        List<BehaviorSummaryDTO.StudentBehaviorSummaryItem> topActiveStudents = new ArrayList<>();
        for (Object[] stat : activeStats.subList(0, Math.min(10, activeStats.size()))) {
            topActiveStudents.add(BehaviorSummaryDTO.StudentBehaviorSummaryItem.builder()
                    .userId((UUID) stat[0])
                    .behaviorCount(((Number) stat[1]).longValue())
                    .build());
        }

        return BehaviorSummaryDTO.builder()
                .totalStudents(totalStudents)
                .totalBehaviors(totalBehaviors)
                .totalNoShows(totalNoShows)
                .totalCancellations(totalCancellations)
                .avgNoShowRate(Math.round(avgNoShowRate * 100.0) / 100.0)
                .avgCancellationRate(Math.round(avgCancellationRate * 100.0) / 100.0)
                .topNoShowStudents(topNoShowStudents)
                .topActiveStudents(topActiveStudents)
                .mostReliableStudents(new ArrayList<>()) // TODO: Tính sau
                .analyzedPeriod(days + " ngày gần nhất")
                .analyzedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    /**
     * Lấy danh sách sinh viên có hành vi bất thường (no-show cao, hủy nhiều)
     */
    public List<StudentBehaviorAnalyticsDTO> getStudentsWithBehaviorIssues(int days, double minNoShowRate) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        List<Object[]> allStats = behaviorRepository.getBehaviorStatsForAllStudents(fromDate);

        List<StudentBehaviorAnalyticsDTO> result = new ArrayList<>();
        for (Object[] stat : allStats) {
            UUID userId = (UUID) stat[0];
            long totalBehaviors = ((Number) stat[1]).longValue();
            long noShows = ((Number) stat[2]).longValue();
            long cancellations = ((Number) stat[3]).longValue();

            if (totalBehaviors > 0) {
                double noShowRate = (double) noShows / totalBehaviors;
                if (noShowRate >= minNoShowRate) {
                    // Lấy analytics chi tiết cho từng sinh viên
                    result.add(getStudentAnalytics(userId, days));
                }
            }
        }

        return result;
    }

    private static final int DEFAULT_ANALYSIS_DAYS = 30;

    /**
     * Ghi nhận một hành vi của sinh viên
     */
    public StudentBehaviorEntity recordBehavior(UUID userId, StudentBehaviorEntity.BehaviorType type, String description) {
        return recordBehavior(userId, type, description, null, null, null, null, null);
    }

    public StudentBehaviorEntity recordBehavior(UUID userId, StudentBehaviorEntity.BehaviorType type, String description,
                                                UUID bookingId, Integer seatId, Integer zoneId, Integer pointsImpact, String metadata) {
        StudentBehaviorEntity behavior = StudentBehaviorEntity.builder()
                .userId(userId)
                .behaviorType(type)
                .description(description)
                .relatedBookingId(bookingId)
                .relatedSeatId(seatId)
                .relatedZoneId(zoneId)
                .pointsImpact(pointsImpact)
                .metadata(metadata)
                .build();

        return behaviorRepository.save(behavior);
    }

    /**
     * Lấy analytics của một sinh viên
     */
    public StudentBehaviorAnalyticsDTO getStudentAnalytics(UUID userId) {
        return getStudentAnalytics(userId, DEFAULT_ANALYSIS_DAYS);
    }

    public StudentBehaviorAnalyticsDTO getStudentAnalytics(UUID userId, int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);

        // Lấy tất cả behaviors trong khoảng thời gian
        List<StudentBehaviorEntity> behaviors = behaviorRepository.findByUserIdAndDateRange(userId, fromDate);

        // Lấy tất cả behaviors (không giới hạn thời gian) để tính tổng
        List<StudentBehaviorEntity> allBehaviors = behaviorRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // Lấy profile để lấy reputation
        int currentReputation = studentProfileRepository.findByUserId(userId)
                .map(p -> p.getReputationScore() != null ? p.getReputationScore() : 0)
                .orElse(0);

        // Tính toán các thống kê
        long totalBookings = behaviors.stream()
                .filter(b -> b.getBehaviorType() == StudentBehaviorEntity.BehaviorType.BOOKING_CREATED)
                .count();

        long totalCheckIns = behaviors.stream()
                .filter(b -> b.getBehaviorType() == StudentBehaviorEntity.BehaviorType.CHECKIN_ON_TIME ||
                        b.getBehaviorType() == StudentBehaviorEntity.BehaviorType.CHECKIN_LATE ||
                        b.getBehaviorType() == StudentBehaviorEntity.BehaviorType.CHECKIN_EARLY)
                .count();

        long totalCancellations = behaviors.stream()
                .filter(b -> b.getBehaviorType() == StudentBehaviorEntity.BehaviorType.BOOKING_CANCELLED)
                .count();

        long totalViolations = behaviors.stream()
                .filter(b -> b.getBehaviorType() == StudentBehaviorEntity.BehaviorType.VIOLATION_CONFIRMED)
                .count();

        // Tính no-show (created nhưng không confirmed)
        long noShows = behaviors.stream()
                .filter(b -> b.getBehaviorType() == StudentBehaviorEntity.BehaviorType.BOOKING_NO_SHOW)
                .count();

        // Tính các tỷ lệ
        double noShowRate = totalBookings > 0 ? (double) noShows / totalBookings : 0.0;

        long onTimeCheckIns = behaviors.stream()
                .filter(b -> b.getBehaviorType() == StudentBehaviorEntity.BehaviorType.CHECKIN_ON_TIME)
                .count();
        double onTimeCheckInRate = totalCheckIns > 0 ? (double) onTimeCheckIns / totalCheckIns : 0.0;

        long onTimeCheckOuts = behaviors.stream()
                .filter(b -> b.getBehaviorType() == StudentBehaviorEntity.BehaviorType.CHECKOUT_ON_TIME)
                .count();
        long totalCheckOuts = onTimeCheckOuts + behaviors.stream()
                .filter(b -> b.getBehaviorType() == StudentBehaviorEntity.BehaviorType.CHECKOUT_LATE)
                .count();
        double onTimeCheckOutRate = totalCheckOuts > 0 ? (double) onTimeCheckOuts / totalCheckOuts : 0.0;

        double cancellationRate = totalBookings > 0 ? (double) totalCancellations / totalBookings : 0.0;

        // Tính tổng điểm
        int totalPointsEarned = allBehaviors.stream()
                .filter(b -> b.getPointsImpact() != null && b.getPointsImpact() > 0)
                .mapToInt(StudentBehaviorEntity::getPointsImpact)
                .sum();

        int totalPointsDeducted = allBehaviors.stream()
                .filter(b -> b.getPointsImpact() != null && b.getPointsImpact() < 0)
                .mapToInt(b -> Math.abs(b.getPointsImpact()))
                .sum();

        // Tính reliability score (0-100)
        int reliabilityScore = calculateReliabilityScore(noShowRate, onTimeCheckInRate, onTimeCheckOutRate, cancellationRate, totalViolations);

        // Behavior counts
        Map<String, Long> behaviorCounts = behaviors.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getBehaviorType().name(),
                        Collectors.counting()
                ));

        // Generate insights
        List<String> insights = generateInsights(noShowRate, onTimeCheckInRate, cancellationRate, totalViolations, behaviors);

        return StudentBehaviorAnalyticsDTO.builder()
                .userId(userId)
                .totalBookings(totalBookings)
                .totalCheckIns(totalCheckIns)
                .totalCancellations(totalCancellations)
                .totalViolations(totalViolations)
                .noShowRate(Math.round(noShowRate * 100.0) / 100.0)
                .onTimeCheckInRate(Math.round(onTimeCheckInRate * 100.0) / 100.0)
                .onTimeCheckOutRate(Math.round(onTimeCheckOutRate * 100.0) / 100.0)
                .cancellationRate(Math.round(cancellationRate * 100.0) / 100.0)
                .currentReputation(currentReputation)
                .totalPointsEarned(totalPointsEarned)
                .totalPointsDeducted(totalPointsDeducted)
                .behaviorCounts(behaviorCounts)
                .insights(insights)
                .reliabilityScore(reliabilityScore)
                .analyzedPeriod(days + " ngày gần nhất")
                .analyzedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    /**
     * Tính reliability score (0-100)
     */
    private int calculateReliabilityScore(double noShowRate, double onTimeCheckInRate,
                                          double onTimeCheckOutRate, double cancellationRate, long violations) {
        int score = 100;

        // Trừ điểm cho các hành vi xấu
        score -= (noShowRate * 30);           // No-show trừ nhiều nhất
        score -= ((1 - onTimeCheckInRate) * 20); // Không đúng giờ trừ 20
        score -= ((1 - onTimeCheckOutRate) * 15); // Checkout muộn trừ 15
        score -= (cancellationRate * 20);     // Hủy nhiều trừ 20
        score -= (violations * 5);           // Mỗi vi phạm trừ 5

        return Math.max(0, Math.min(100, score));
    }

    /**
     * Tạo insights dựa trên hành vi
     */
    private List<String> generateInsights(double noShowRate, double onTimeCheckInRate,
                                         double cancellationRate, long violations,
                                         List<StudentBehaviorEntity> behaviors) {
        List<String> insights = new ArrayList<>();

        // Phân tích no-show
        if (noShowRate > 0.3) {
            insights.add("⚠️ Tỷ lệ bỏ chỗ cao (" + Math.round(noShowRate * 100) + "%). Cần cải thiện.");
        } else if (noShowRate > 0.1) {
            insights.add("Tỷ lệ bỏ chỗ ở mức trung bình (" + Math.round(noShowRate * 100) + "%).");
        } else if (noShowRate == 0 && behaviors.stream().anyMatch(b -> b.getBehaviorType() == StudentBehaviorEntity.BehaviorType.BOOKING_CREATED)) {
            insights.add("✅ Không có trường hợp bỏ chỗ. Rất đáng khen!");
        }

        // Phân tích check-in đúng giờ
        if (onTimeCheckInRate > 0.9) {
            insights.add("✅ Check-in đúng giờ rất tốt (" + Math.round(onTimeCheckInRate * 100) + "%).");
        } else if (onTimeCheckInRate < 0.7) {
            insights.add("⚠️ Nên check-in đúng giờ hơn để giữ chỗ tốt.");
        }

        // Phân tích hủy
        if (cancellationRate > 0.3) {
            insights.add("⚠️ Tỷ lệ hủy cao (" + Math.round(cancellationRate * 100) + "%). Hủy sớm nếu không đến được.");
        }

        // Phân tích vi phạm
        if (violations > 5) {
            insights.add("⚠️ Có " + violations + " vi phạm. Cần tuân thủ nội quy thư viện.");
        } else if (violations == 0 && behaviors.stream().anyMatch(b -> b.getBehaviorType() == StudentBehaviorEntity.BehaviorType.VIOLATION_CONFIRMED)) {
            insights.add("✅ Không có vi phạm nào. Tiếp tục giữ gìn!");
        }

        // Xu hướng theo ngày
        Map<Integer, Long> bookingsByDayOfWeek = behaviors.stream()
                .filter(b -> b.getBehaviorType() == StudentBehaviorEntity.BehaviorType.BOOKING_CREATED)
                .collect(Collectors.groupingBy(
                        b -> b.getCreatedAt().getDayOfWeek().getValue(),
                        Collectors.counting()
                ));

        if (!bookingsByDayOfWeek.isEmpty()) {
            int maxDay = bookingsByDayOfWeek.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(0);

            String[] days = {"", "Chủ nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"};
            if (maxDay >= 1 && maxDay <= 7) {
                insights.add("📅 " + days[maxDay] + " là ngày bạn hay đặt chỗ nhất.");
            }
        }

        return insights;
    }

    /**
     * Ghi nhận no-show (gọi khi booking expired mà không confirmed)
     */
    public void recordNoShow(UUID userId, UUID bookingId) {
        recordBehavior(userId, StudentBehaviorEntity.BehaviorType.BOOKING_NO_SHOW,
                "Sinh viên không đến", bookingId, null, null, -10, null);
    }

    /**
     * Ghi nhận check-in đúng giờ
     */
    public void recordOnTimeCheckIn(UUID userId, UUID bookingId, Integer seatId, Integer zoneId) {
        recordBehavior(userId, StudentBehaviorEntity.BehaviorType.CHECKIN_ON_TIME,
                "Check-in đúng giờ", bookingId, seatId, zoneId, +5, null);
    }

    /**
     * Ghi nhận check-in muộn
     */
    public void recordLateCheckIn(UUID userId, UUID bookingId, Integer seatId, Integer zoneId, int minutesLate) {
        recordBehavior(userId, StudentBehaviorEntity.BehaviorType.CHECKIN_LATE,
                "Check-in muộn " + minutesLate + " phút", bookingId, seatId, zoneId, 0,
                "{\"minutesLate\": " + minutesLate + "}");
    }
}
