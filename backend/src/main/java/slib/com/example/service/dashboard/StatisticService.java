package slib.com.example.service.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.dto.dashboard.StatisticDTO;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.complaint.ComplaintRepository;
import slib.com.example.repository.feedback.FeedbackRepository;
import slib.com.example.repository.feedback.SeatViolationReportRepository;
import slib.com.example.repository.hce.AccessLogRepository;
import slib.com.example.repository.system.SystemLogRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatisticService {

    private final AccessLogRepository accessLogRepository;
    private final ReservationRepository reservationRepository;
    private final SeatViolationReportRepository violationReportRepository;
    private final FeedbackRepository feedbackRepository;
    private final ComplaintRepository complaintRepository;

    private static final Map<String, String> VIOLATION_LABELS = Map.of(
            "UNAUTHORIZED_USE", "S\u1eed d\u1ee5ng tr\u00e1i ph\u00e9p",
            "LEFT_BELONGINGS", "\u0110\u1ec3 \u0111\u1ed3 \u0111\u1ea1c",
            "NOISE", "G\u00e2y \u1ed3n \u00e0o",
            "FEET_ON_SEAT", "G\u00e1c ch\u00e2n l\u00ean gh\u1ebf",
            "FOOD_DRINK", "\u0102n u\u1ed1ng",
            "SLEEPING", "Ng\u1ee7",
            "OTHER", "Kh\u00e1c");

    /**
     * Tổng hợp tất cả thống kê theo range (week/month/year)
     */
    public StatisticDTO getStatistics(String range) {
        LocalDateTime startDate = calculateStartDate(range);

        return StatisticDTO.builder()
                .overview(buildOverview(startDate))
                .bookingAnalysis(buildBookingAnalysis(startDate))
                .violationsByType(buildViolationsByType(startDate))
                .feedbackSummary(buildFeedbackSummary(startDate))
                .zoneUsage(buildZoneUsage(startDate))
                .peakHours(buildPeakHours(startDate))
                .build();
    }

    private LocalDateTime calculateStartDate(String range) {
        LocalDateTime now = LocalDateTime.now();
        if ("year".equalsIgnoreCase(range)) {
            return now.minusYears(1);
        } else if ("month".equalsIgnoreCase(range)) {
            return now.minusDays(30);
        } else if ("day".equalsIgnoreCase(range)) {
            return now.toLocalDate().atStartOfDay();
        } else {
            return now.minusDays(7);
        }
    }

    // ========== Overview ==========
    private StatisticDTO.OverviewDTO buildOverview(LocalDateTime startDate) {
        try {
            long totalCheckIns = accessLogRepository.countByCheckInTimeAfter(startDate);
            long totalFeedbacks = feedbackRepository.countByCreatedAtAfter(startDate);
            long totalViolations = violationReportRepository.countByCreatedAtAfter(startDate);
            long totalComplaints = complaintRepository.countByCreatedAtAfter(startDate);

            // Tổng bookings (tất cả status)
            List<Object[]> bookingsByStatus = reservationRepository.countBookingsGroupByStatus(startDate);
            long totalBookings = bookingsByStatus.stream()
                    .mapToLong(row -> ((Number) row[1]).longValue())
                    .sum();

            return StatisticDTO.OverviewDTO.builder()
                    .totalCheckIns(totalCheckIns)
                    .totalBookings(totalBookings)
                    .totalViolations(totalViolations)
                    .totalFeedbacks(totalFeedbacks)
                    .totalComplaints(totalComplaints)
                    .build();
        } catch (Exception e) {
            log.error("Error building overview: {}", e.getMessage());
            return StatisticDTO.OverviewDTO.builder().build();
        }
    }

    // ========== Booking Analysis ==========
    private StatisticDTO.BookingAnalysisDTO buildBookingAnalysis(LocalDateTime startDate) {
        try {
            List<Object[]> data = reservationRepository.countBookingsGroupByStatus(startDate);
            Map<String, Long> statusMap = new HashMap<>();
            long total = 0;
            for (Object[] row : data) {
                String status = (String) row[0];
                long count = ((Number) row[1]).longValue();
                statusMap.put(status, count);
                total += count;
            }

            long used = statusMap.getOrDefault("CONFIRMED", 0L) + statusMap.getOrDefault("COMPLETED", 0L);
            long cancelled = statusMap.getOrDefault("CANCELLED", 0L) + statusMap.getOrDefault("CANCEL", 0L);
            long expiredNoShow = statusMap.getOrDefault("EXPIRED", 0L);

            double usedP = total > 0 ? Math.round(used * 10000.0 / total) / 100.0 : 0;
            double cancelP = total > 0 ? Math.round(cancelled * 10000.0 / total) / 100.0 : 0;
            double expiredP = total > 0 ? Math.round(expiredNoShow * 10000.0 / total) / 100.0 : 0;

            return StatisticDTO.BookingAnalysisDTO.builder()
                    .totalBookings(total)
                    .usedBookings(used)
                    .cancelledBookings(cancelled)
                    .expiredNoShow(expiredNoShow)
                    .usedPercent(usedP)
                    .cancelledPercent(cancelP)
                    .expiredPercent(expiredP)
                    .build();
        } catch (Exception e) {
            log.error("Error building booking analysis: {}", e.getMessage());
            return StatisticDTO.BookingAnalysisDTO.builder().build();
        }
    }

    // ========== Violations by Type ==========
    private List<StatisticDTO.ViolationTypeStatDTO> buildViolationsByType(LocalDateTime startDate) {
        try {
            List<Object[]> data = violationReportRepository.countByViolationTypeAfter(startDate);
            return data.stream()
                    .map(row -> {
                        String type = (String) row[0];
                        long count = ((Number) row[1]).longValue();
                        return StatisticDTO.ViolationTypeStatDTO.builder()
                                .violationType(type)
                                .label(VIOLATION_LABELS.getOrDefault(type, type))
                                .count(count)
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error building violations by type: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ========== Feedback Summary ==========
    private StatisticDTO.FeedbackSummaryDTO buildFeedbackSummary(LocalDateTime startDate) {
        try {
            double avgRating = feedbackRepository.getAverageRatingAfter(startDate);
            avgRating = Math.round(avgRating * 10.0) / 10.0;

            long totalCount = feedbackRepository.countByCreatedAtAfter(startDate);

            // Rating distribution
            List<Object[]> ratingData = feedbackRepository.countByRatingAfter(startDate);
            List<StatisticDTO.RatingDistributionDTO> distribution = new ArrayList<>();

            // Đảm bảo có đủ 1-5 stars
            Map<Integer, Long> ratingMap = new HashMap<>();
            for (Object[] row : ratingData) {
                int rating = ((Number) row[0]).intValue();
                long count = ((Number) row[1]).longValue();
                ratingMap.put(rating, count);
            }

            long totalRated = ratingMap.values().stream().mapToLong(Long::longValue).sum();

            for (int i = 1; i <= 5; i++) {
                long count = ratingMap.getOrDefault(i, 0L);
                double percent = totalRated > 0 ? Math.round(count * 10000.0 / totalRated) / 100.0 : 0;
                distribution.add(StatisticDTO.RatingDistributionDTO.builder()
                        .rating(i).count(count).percent(percent).build());
            }

            // Recent feedbacks
            var recentEntities = feedbackRepository.findTop10ByCreatedAtAfterOrderByCreatedAtDesc(startDate);
            List<StatisticDTO.RecentFeedbackDTO> recentFeedbacks = recentEntities.stream()
                    .map(f -> StatisticDTO.RecentFeedbackDTO.builder()
                            .id(f.getId())
                            .userName(f.getUser() != null ? f.getUser().getFullName() : "N/A")
                            .userCode(f.getUser() != null ? f.getUser().getUserCode() : "N/A")
                            .avatarUrl(f.getUser() != null ? f.getUser().getAvtUrl() : null)
                            .rating(f.getRating())
                            .content(f.getContent())
                            .createdAt(f.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());

            return StatisticDTO.FeedbackSummaryDTO.builder()
                    .averageRating(avgRating)
                    .totalCount(totalCount)
                    .ratingDistribution(distribution)
                    .recentFeedbacks(recentFeedbacks)
                    .build();
        } catch (Exception e) {
            log.error("Error building feedback summary: {}", e.getMessage());
            return StatisticDTO.FeedbackSummaryDTO.builder()
                    .ratingDistribution(Collections.emptyList())
                    .recentFeedbacks(Collections.emptyList())
                    .build();
        }
    }

    // ========== Zone Usage ==========
    private List<StatisticDTO.ZoneUsageDTO> buildZoneUsage(LocalDateTime startDate) {
        try {
            List<Object[]> data = reservationRepository.countBookingsByZone(startDate);
            return data.stream()
                    .map(row -> {
                        int zoneId = ((Number) row[0]).intValue();
                        String zoneName = (String) row[1];
                        String areaName = (String) row[2];
                        long bookingCount = ((Number) row[3]).longValue();
                        long totalSeats = ((Number) row[4]).longValue();
                        double usagePercent = totalSeats > 0
                                ? Math.round(bookingCount * 10000.0 / totalSeats) / 100.0
                                : 0;
                        return StatisticDTO.ZoneUsageDTO.builder()
                                .zoneId(zoneId)
                                .zoneName(zoneName)
                                .areaName(areaName)
                                .totalSeats(totalSeats)
                                .totalBookings(bookingCount)
                                .usagePercent(usagePercent)
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error building zone usage: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ========== Peak Hours ==========
    private List<StatisticDTO.PeakHourDTO> buildPeakHours(LocalDateTime startDate) {
        try {
            List<Object[]> data = accessLogRepository.countCheckInsByHour(startDate);
            Map<Integer, Long> hourMap = new HashMap<>();
            for (Object[] row : data) {
                int hour = ((Number) row[0]).intValue();
                long count = ((Number) row[1]).longValue();
                hourMap.put(hour, count);
            }

            // Chỉ trả về giờ từ 6h-22h (giờ hoạt động thư viện)
            List<StatisticDTO.PeakHourDTO> result = new ArrayList<>();
            for (int h = 6; h <= 22; h++) {
                result.add(StatisticDTO.PeakHourDTO.builder()
                        .hour(h)
                        .count(hourMap.getOrDefault(h, 0L))
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.error("Error building peak hours: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
