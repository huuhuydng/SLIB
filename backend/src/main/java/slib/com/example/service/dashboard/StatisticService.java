package slib.com.example.service.dashboard;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        PeriodWindow periodWindow = calculatePeriodWindow(range);
        LocalDateTime startDate = periodWindow.currentStart();

        StatisticDTO.OverviewDTO overview = buildOverview(startDate);
        StatisticDTO.BookingAnalysisDTO bookingAnalysis = buildBookingAnalysis(startDate);
        List<StatisticDTO.ViolationTypeStatDTO> violationsByType = buildViolationsByType(startDate);
        StatisticDTO.FeedbackSummaryDTO feedbackSummary = buildFeedbackSummary(startDate);
        List<StatisticDTO.ZoneUsageDTO> zoneUsage = buildZoneUsage(startDate);
        List<StatisticDTO.PeakHourDTO> peakHours = buildPeakHours(startDate);

        return StatisticDTO.builder()
                .overview(overview)
                .comparison(buildOverviewComparison(periodWindow))
                .bookingAnalysis(bookingAnalysis)
                .violationsByType(violationsByType)
                .feedbackSummary(feedbackSummary)
                .zoneUsage(zoneUsage)
                .peakHours(peakHours)
                .insights(buildInsights(bookingAnalysis, violationsByType, zoneUsage, peakHours, feedbackSummary))
                .build();
    }

    public byte[] exportStatisticsToExcel(String range) throws IOException {
        StatisticDTO statistic = getStatistics(range);
        String rangeLabel = getRangeLabel(range);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            createOverviewSheet(workbook, statistic, rangeLabel, headerStyle, dataStyle, titleStyle);
            createBookingSheet(workbook, statistic, headerStyle, dataStyle, titleStyle);
            createViolationSheet(workbook, statistic, headerStyle, dataStyle, titleStyle);
            createFeedbackSheet(workbook, statistic, headerStyle, dataStyle, titleStyle, dateTimeFormatter);
            createZoneUsageSheet(workbook, statistic, headerStyle, dataStyle, titleStyle);
            createPeakHoursSheet(workbook, statistic, headerStyle, dataStyle, titleStyle);
            createInsightsSheet(workbook, statistic, headerStyle, dataStyle, titleStyle);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private PeriodWindow calculatePeriodWindow(String range) {
        LocalDateTime now = LocalDateTime.now();
        if ("year".equalsIgnoreCase(range)) {
            LocalDateTime currentStart = now.minusYears(1);
            return new PeriodWindow(currentStart, now, currentStart.minusYears(1), currentStart);
        } else if ("month".equalsIgnoreCase(range)) {
            LocalDateTime currentStart = now.minusDays(30);
            return new PeriodWindow(currentStart, now, currentStart.minusDays(30), currentStart);
        } else if ("day".equalsIgnoreCase(range)) {
            LocalDateTime currentStart = now.toLocalDate().atStartOfDay();
            return new PeriodWindow(currentStart, now, currentStart.minusDays(1), currentStart);
        } else {
            LocalDateTime currentStart = now.minusDays(7);
            return new PeriodWindow(currentStart, now, currentStart.minusDays(7), currentStart);
        }
    }

    private StatisticDTO.OverviewComparisonDTO buildOverviewComparison(PeriodWindow periodWindow) {
        try {
            return StatisticDTO.OverviewComparisonDTO.builder()
                    .checkIns(buildMetricDelta(
                            accessLogRepository.countByCheckInTimeBetween(periodWindow.currentStart(), periodWindow.currentEnd()),
                            accessLogRepository.countByCheckInTimeBetween(periodWindow.previousStart(), periodWindow.previousEnd())))
                    .bookings(buildMetricDelta(
                            reservationRepository.countByCreatedAtBetween(periodWindow.currentStart(), periodWindow.currentEnd()),
                            reservationRepository.countByCreatedAtBetween(periodWindow.previousStart(), periodWindow.previousEnd())))
                    .violations(buildMetricDelta(
                            violationReportRepository.countByCreatedAtBetween(periodWindow.currentStart(), periodWindow.currentEnd()),
                            violationReportRepository.countByCreatedAtBetween(periodWindow.previousStart(), periodWindow.previousEnd())))
                    .feedbacks(buildMetricDelta(
                            feedbackRepository.countByCreatedAtBetween(periodWindow.currentStart(), periodWindow.currentEnd()),
                            feedbackRepository.countByCreatedAtBetween(periodWindow.previousStart(), periodWindow.previousEnd())))
                    .complaints(buildMetricDelta(
                            complaintRepository.countByCreatedAtBetween(periodWindow.currentStart(), periodWindow.currentEnd()),
                            complaintRepository.countByCreatedAtBetween(periodWindow.previousStart(), periodWindow.previousEnd())))
                    .build();
        } catch (Exception e) {
            log.error("Error building overview comparison: {}", e.getMessage());
            return StatisticDTO.OverviewComparisonDTO.builder().build();
        }
    }

    private StatisticDTO.MetricDeltaDTO buildMetricDelta(long currentValue, long previousValue) {
        long changeValue = currentValue - previousValue;
        double changePercent;
        if (previousValue == 0) {
            changePercent = currentValue > 0 ? 100.0 : 0.0;
        } else {
            changePercent = Math.round((changeValue * 10000.0 / previousValue)) / 100.0;
        }
        return StatisticDTO.MetricDeltaDTO.builder()
                .currentValue(currentValue)
                .previousValue(previousValue)
                .changeValue(changeValue)
                .changePercent(changePercent)
                .build();
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
            long totalBookingsInRange = data.stream()
                    .mapToLong(row -> ((Number) row[3]).longValue())
                    .sum();
            return data.stream()
                    .map(row -> {
                        int zoneId = ((Number) row[0]).intValue();
                        String zoneName = (String) row[1];
                        String areaName = (String) row[2];
                        long bookingCount = ((Number) row[3]).longValue();
                        long totalSeats = ((Number) row[4]).longValue();
                        double usagePercent = totalBookingsInRange > 0
                                ? Math.round(bookingCount * 10000.0 / totalBookingsInRange) / 100.0
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

    private List<StatisticDTO.InsightDTO> buildInsights(
            StatisticDTO.BookingAnalysisDTO bookingAnalysis,
            List<StatisticDTO.ViolationTypeStatDTO> violationsByType,
            List<StatisticDTO.ZoneUsageDTO> zoneUsage,
            List<StatisticDTO.PeakHourDTO> peakHours,
            StatisticDTO.FeedbackSummaryDTO feedbackSummary) {
        List<StatisticDTO.InsightDTO> insights = new ArrayList<>();

        peakHours.stream()
                .max(Comparator.comparingLong(StatisticDTO.PeakHourDTO::getCount))
                .filter(item -> item.getCount() > 0)
                .ifPresent(item -> insights.add(StatisticDTO.InsightDTO.builder()
                        .type("peak")
                        .title("Khung giờ cao điểm")
                        .description("Khung " + item.getHour() + ":00 đang có lưu lượng check-in cao nhất trong giai đoạn này.")
                        .tone("orange")
                        .build()));

        violationsByType.stream()
                .max(Comparator.comparingLong(StatisticDTO.ViolationTypeStatDTO::getCount))
                .filter(item -> item.getCount() > 0)
                .ifPresent(item -> insights.add(StatisticDTO.InsightDTO.builder()
                        .type("violation")
                        .title("Vi phạm nổi bật")
                        .description(item.getLabel() + " đang là nhóm vi phạm xuất hiện nhiều nhất.")
                        .tone("red")
                        .build()));

        zoneUsage.stream()
                .max(Comparator.comparingDouble(StatisticDTO.ZoneUsageDTO::getUsagePercent))
                .filter(item -> item.getUsagePercent() > 0)
                .ifPresent(item -> insights.add(StatisticDTO.InsightDTO.builder()
                        .type("zone")
                        .title("Khu vực bận rộn nhất")
                        .description(item.getZoneName() + " đang có tỷ lệ sử dụng khoảng " + Math.round(item.getUsagePercent()) + "%.")
                        .tone("blue")
                        .build()));

        if (bookingAnalysis != null && bookingAnalysis.getExpiredNoShow() > 0) {
            insights.add(StatisticDTO.InsightDTO.builder()
                    .type("booking")
                    .title("Rủi ro không đến")
                    .description("Có " + bookingAnalysis.getExpiredNoShow() + " lượt đặt chỗ không đến, chiếm "
                            + Math.round(bookingAnalysis.getExpiredPercent()) + "% tổng lượt đặt.")
                    .tone(bookingAnalysis.getExpiredPercent() >= 20 ? "red" : "amber")
                    .build());
        } else if (feedbackSummary != null && feedbackSummary.getAverageRating() >= 4.5) {
            insights.add(StatisticDTO.InsightDTO.builder()
                    .type("feedback")
                    .title("Chất lượng phản hồi tích cực")
                    .description("Điểm đánh giá trung bình đang giữ ở mức " + feedbackSummary.getAverageRating() + "/5.")
                    .tone("green")
                    .build());
        }

        return insights.stream().limit(4).collect(Collectors.toList());
    }

    private record PeriodWindow(
            LocalDateTime currentStart,
            LocalDateTime currentEnd,
            LocalDateTime previousStart,
            LocalDateTime previousEnd) {
    }

    private String getRangeLabel(String range) {
        return switch (range == null ? "" : range.toLowerCase(Locale.ROOT)) {
            case "day" -> "Hôm nay";
            case "month" -> "Tháng này";
            case "year" -> "Năm nay";
            default -> "Tuần này";
        };
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private void createOverviewSheet(
            Workbook workbook,
            StatisticDTO statistic,
            String rangeLabel,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle titleStyle) {
        Sheet sheet = workbook.createSheet("Tong quan");
        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Báo cáo thống kê thư viện");
        titleCell.setCellStyle(titleStyle);

        Row rangeRow = sheet.createRow(rowIndex++);
        rangeRow.createCell(0).setCellValue("Giai đoạn");
        rangeRow.createCell(1).setCellValue(rangeLabel);

        rowIndex++;

        String[] headers = { "Chỉ số", "Giá trị hiện tại", "Kỳ trước", "Chênh lệch", "Tỷ lệ thay đổi (%)" };
        Row headerRow = sheet.createRow(rowIndex++);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        StatisticDTO.OverviewDTO overview = statistic.getOverview();
        StatisticDTO.OverviewComparisonDTO comparison = statistic.getComparison();
        writeMetricRow(sheet, rowIndex++, "Lượt check-in", overview != null ? overview.getTotalCheckIns() : 0,
                comparison != null ? comparison.getCheckIns() : null, dataStyle);
        writeMetricRow(sheet, rowIndex++, "Lượt đặt chỗ", overview != null ? overview.getTotalBookings() : 0,
                comparison != null ? comparison.getBookings() : null, dataStyle);
        writeMetricRow(sheet, rowIndex++, "Vi phạm", overview != null ? overview.getTotalViolations() : 0,
                comparison != null ? comparison.getViolations() : null, dataStyle);
        writeMetricRow(sheet, rowIndex++, "Phản hồi", overview != null ? overview.getTotalFeedbacks() : 0,
                comparison != null ? comparison.getFeedbacks() : null, dataStyle);
        writeMetricRow(sheet, rowIndex++, "Khiếu nại", overview != null ? overview.getTotalComplaints() : 0,
                comparison != null ? comparison.getComplaints() : null, dataStyle);

        setColumnWidths(sheet, 24, 18, 18, 16, 18);
    }

    private void writeMetricRow(Sheet sheet, int rowIndex, String label, long currentValue,
            StatisticDTO.MetricDeltaDTO delta, CellStyle dataStyle) {
        Row row = sheet.createRow(rowIndex);
        writeCell(row, 0, label, dataStyle);
        writeCell(row, 1, currentValue, dataStyle);
        writeCell(row, 2, delta != null ? delta.getPreviousValue() : 0, dataStyle);
        writeCell(row, 3, delta != null ? delta.getChangeValue() : 0, dataStyle);
        writeCell(row, 4, delta != null ? delta.getChangePercent() : 0, dataStyle);
    }

    private void createBookingSheet(
            Workbook workbook,
            StatisticDTO statistic,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle titleStyle) {
        Sheet sheet = workbook.createSheet("Dat cho");
        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Phân tích đặt chỗ");
        titleCell.setCellStyle(titleStyle);
        rowIndex++;

        Row headerRow = sheet.createRow(rowIndex++);
        String[] headers = { "Chỉ số", "Giá trị", "Tỷ lệ (%)" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        StatisticDTO.BookingAnalysisDTO booking = statistic.getBookingAnalysis();
        if (booking != null) {
            writeSimpleRow(sheet, rowIndex++, "Tổng lượt đặt chỗ", booking.getTotalBookings(), null, dataStyle);
            writeSimpleRow(sheet, rowIndex++, "Đã sử dụng", booking.getUsedBookings(), booking.getUsedPercent(), dataStyle);
            writeSimpleRow(sheet, rowIndex++, "Đã hủy", booking.getCancelledBookings(), booking.getCancelledPercent(), dataStyle);
            writeSimpleRow(sheet, rowIndex++, "Không đến", booking.getExpiredNoShow(), booking.getExpiredPercent(), dataStyle);
        }

        setColumnWidths(sheet, 24, 16, 16);
    }

    private void writeSimpleRow(Sheet sheet, int rowIndex, String label, Number value, Double percent, CellStyle dataStyle) {
        Row row = sheet.createRow(rowIndex);
        writeCell(row, 0, label, dataStyle);
        writeCell(row, 1, value != null ? value.doubleValue() : 0, dataStyle);
        writeCell(row, 2, percent != null ? percent : null, dataStyle);
    }

    private void createViolationSheet(
            Workbook workbook,
            StatisticDTO statistic,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle titleStyle) {
        Sheet sheet = workbook.createSheet("Vi pham");
        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Thống kê vi phạm");
        titleCell.setCellStyle(titleStyle);
        rowIndex++;

        Row headerRow = sheet.createRow(rowIndex++);
        String[] headers = { "Loại vi phạm", "Mã loại", "Số lượt" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<StatisticDTO.ViolationTypeStatDTO> rows = statistic.getViolationsByType();
        if (rows != null) {
            for (StatisticDTO.ViolationTypeStatDTO item : rows) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, item.getLabel(), dataStyle);
                writeCell(row, 1, item.getViolationType(), dataStyle);
                writeCell(row, 2, item.getCount(), dataStyle);
            }
        }

        setColumnWidths(sheet, 28, 22, 12);
    }

    private void createFeedbackSheet(
            Workbook workbook,
            StatisticDTO statistic,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle titleStyle,
            DateTimeFormatter dateTimeFormatter) {
        Sheet sheet = workbook.createSheet("Phan hoi");
        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Phản hồi gần đây");
        titleCell.setCellStyle(titleStyle);

        StatisticDTO.FeedbackSummaryDTO feedback = statistic.getFeedbackSummary();
        rowIndex++;
        Row summaryRow = sheet.createRow(rowIndex++);
        writeCell(summaryRow, 0, "Điểm trung bình", dataStyle);
        writeCell(summaryRow, 1, feedback != null ? feedback.getAverageRating() : 0, dataStyle);
        writeCell(summaryRow, 2, "Tổng phản hồi", dataStyle);
        writeCell(summaryRow, 3, feedback != null ? feedback.getTotalCount() : 0, dataStyle);

        rowIndex++;
        Row headerRow = sheet.createRow(rowIndex++);
        String[] headers = { "Sinh viên", "Mã số", "Đánh giá", "Nội dung", "Thời gian" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        if (feedback != null && feedback.getRecentFeedbacks() != null) {
            for (StatisticDTO.RecentFeedbackDTO item : feedback.getRecentFeedbacks()) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, item.getUserName(), dataStyle);
                writeCell(row, 1, item.getUserCode(), dataStyle);
                writeCell(row, 2, item.getRating(), dataStyle);
                writeCell(row, 3, item.getContent(), dataStyle);
                writeCell(row, 4, item.getCreatedAt() != null ? item.getCreatedAt().format(dateTimeFormatter) : "-", dataStyle);
            }
        }

        setColumnWidths(sheet, 24, 14, 10, 48, 20);
    }

    private void createZoneUsageSheet(
            Workbook workbook,
            StatisticDTO statistic,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle titleStyle) {
        Sheet sheet = workbook.createSheet("Khu vuc");
        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Mức sử dụng khu vực");
        titleCell.setCellStyle(titleStyle);
        rowIndex++;

        Row headerRow = sheet.createRow(rowIndex++);
        String[] headers = { "Khu vực", "Phòng", "Tổng ghế", "Lượt sử dụng", "Tỷ lệ sử dụng (%)" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<StatisticDTO.ZoneUsageDTO> rows = statistic.getZoneUsage();
        if (rows != null) {
            for (StatisticDTO.ZoneUsageDTO item : rows) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, item.getZoneName(), dataStyle);
                writeCell(row, 1, item.getAreaName(), dataStyle);
                writeCell(row, 2, item.getTotalSeats(), dataStyle);
                writeCell(row, 3, item.getTotalBookings(), dataStyle);
                writeCell(row, 4, item.getUsagePercent(), dataStyle);
            }
        }

        setColumnWidths(sheet, 24, 24, 12, 14, 18);
    }

    private void createPeakHoursSheet(
            Workbook workbook,
            StatisticDTO statistic,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle titleStyle) {
        Sheet sheet = workbook.createSheet("Khung gio");
        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Khung giờ check-in");
        titleCell.setCellStyle(titleStyle);
        rowIndex++;

        Row headerRow = sheet.createRow(rowIndex++);
        String[] headers = { "Khung giờ", "Lượt check-in" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<StatisticDTO.PeakHourDTO> rows = statistic.getPeakHours();
        if (rows != null) {
            for (StatisticDTO.PeakHourDTO item : rows) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, item.getHour() + ":00", dataStyle);
                writeCell(row, 1, item.getCount(), dataStyle);
            }
        }

        setColumnWidths(sheet, 16, 14);
    }

    private void createInsightsSheet(
            Workbook workbook,
            StatisticDTO statistic,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle titleStyle) {
        Sheet sheet = workbook.createSheet("Nhan dinh");
        int rowIndex = 0;
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Nhận định nổi bật");
        titleCell.setCellStyle(titleStyle);
        rowIndex++;

        Row headerRow = sheet.createRow(rowIndex++);
        String[] headers = { "Nhóm", "Tiêu đề", "Mô tả", "Mức độ" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<StatisticDTO.InsightDTO> rows = statistic.getInsights();
        if (rows != null) {
            for (StatisticDTO.InsightDTO item : rows) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, item.getType(), dataStyle);
                writeCell(row, 1, item.getTitle(), dataStyle);
                writeCell(row, 2, item.getDescription(), dataStyle);
                writeCell(row, 3, item.getTone(), dataStyle);
            }
        }

        setColumnWidths(sheet, 16, 28, 52, 12);
    }

    private void writeCell(Row row, int colIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        if (value == null) {
            cell.setCellValue("-");
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
        cell.setCellStyle(style);
    }

    private void setColumnWidths(Sheet sheet, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }
}
