package slib.com.example.service.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.dto.dashboard.DashboardStatsDTO;
import slib.com.example.dto.hce.AccessLogStatsDTO;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.feedback.SeatViolationReportEntity;
import slib.com.example.entity.support.SupportRequestStatus;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.complaint.ComplaintRepository;
import slib.com.example.repository.feedback.FeedbackRepository;
import slib.com.example.repository.feedback.SeatStatusReportRepository;
import slib.com.example.repository.feedback.SeatViolationReportRepository;
import slib.com.example.repository.hce.AccessLogRepository;
import slib.com.example.repository.support.SupportRequestRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.repository.zone_config.AreaRepository;
import slib.com.example.repository.zone_config.SeatRepository;
import slib.com.example.repository.zone_config.ZoneRepository;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import slib.com.example.service.hce.CheckInService;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private static final long SUPPORT_OVERDUE_MINUTES = 30;

    private final CheckInService checkInService;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final SeatViolationReportRepository violationReportRepository;
    private final SupportRequestRepository supportRequestRepository;
    private final AreaRepository areaRepository;
    private final ZoneRepository zoneRepository;
    private final AccessLogRepository accessLogRepository;
    private final ComplaintRepository complaintRepository;
    private final FeedbackRepository feedbackRepository;
    private final SeatStatusReportRepository seatStatusReportRepository;

    public DashboardStatsDTO getDashboardStats() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
            List<String> activeBookingStatuses = List.of("BOOKED", "CONFIRMED");
            List<String> bookingStatuses = List.of("PROCESSING", "BOOKED", "CONFIRMED", "COMPLETED", "EXPIRED");

            // 1. Check-in/out stats
            AccessLogStatsDTO accessStats = checkInService.getTodayStats();

            // 2. Seat stats
            long totalSeats = seatRepository.countByIsActiveTrue();
            long currentlyInLibrary = Math.max(0, accessStats.getCurrentlyInLibrary());
            long activeBookings = reservationRepository.countActiveReservationsAtTime(now, activeBookingStatuses);
            double occupancyRate = totalSeats > 0
                    ? Math.round((double) activeBookings / totalSeats * 10000.0) / 100.0
                    : 0;

            // 3. Booking stats
            long totalBookingsToday = reservationRepository.countByStartTimeBetweenAndStatusIn(
                    startOfDay,
                    endOfDay,
                    bookingStatuses);
            long pendingBookings = reservationRepository.countUpcomingReservationsBetween(
                    now,
                    endOfDay,
                    List.of("PROCESSING", "BOOKED"));

            // 4. Violations today
            long violationsToday = violationReportRepository.countByCreatedAtBetween(startOfDay, endOfDay);
            long pendingViolations = violationReportRepository.countByStatusIn(
                    Set.of(
                            SeatViolationReportEntity.ReportStatus.PENDING,
                            SeatViolationReportEntity.ReportStatus.VERIFIED));

            // 5. Support requests
            long pendingSupportRequests = supportRequestRepository.countByStatus(SupportRequestStatus.PENDING);
            long inProgressSupportRequests = supportRequestRepository.countByStatus(SupportRequestStatus.IN_PROGRESS);
            long overdueSupportRequests = supportRequestRepository.countByStatusAndCreatedAtBefore(
                    SupportRequestStatus.PENDING,
                    now.minusMinutes(SUPPORT_OVERDUE_MINUTES));

            // 6. Seat status reports and complaints
            long pendingSeatStatusReports = seatStatusReportRepository.countByStatusIn(
                    Set.of(
                            slib.com.example.entity.feedback.SeatStatusReportEntity.ReportStatus.PENDING));
            long pendingComplaints = complaintRepository.countByStatus(
                    slib.com.example.entity.complaint.ComplaintEntity.ComplaintStatus.PENDING);

            // 7. Total users
            long totalUsers = userRepository.count();

            // 8. Recent bookings (today only)
            List<ReservationEntity> recentReservations = reservationRepository.findByStartTimeBetweenOrderByCreatedAtDesc(startOfDay, endOfDay)
                    .stream()
                    .sorted(Comparator
                            .comparing(ReservationEntity::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(ReservationEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
            List<DashboardStatsDTO.RecentBookingDTO> recentBookings = recentReservations.stream()
                    .map(r -> DashboardStatsDTO.RecentBookingDTO.builder()
                            .reservationId(r.getReservationId())
                            .userName(r.getUser() != null ? r.getUser().getFullName() : "N/A")
                            .userCode(r.getUser() != null ? r.getUser().getUserCode() : "N/A")
                            .seatCode(r.getSeat() != null ? r.getSeat().getSeatCode() : "N/A")
                            .zoneName(r.getSeat() != null && r.getSeat().getZone() != null
                                    ? r.getSeat().getZone().getZoneName()
                                    : "N/A")
                            .status(r.getStatus())
                            .startTime(r.getStartTime())
                            .endTime(r.getEndTime())
                            .createdAt(r.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());

            // 9. Area occupancy (real data)
            List<DashboardStatsDTO.ZoneOccupancyDTO> zoneOccupancies = getZoneOccupancies(now);
            List<DashboardStatsDTO.AreaOccupancyDTO> areaOccupancies = getAreaOccupancies(zoneOccupancies);

            // 10. Weekly stats (7 days)
            List<DashboardStatsDTO.WeeklyStatsDTO> weeklyStats = getWeeklyStats();

            // 11. Recent violations (top 5)
            List<DashboardStatsDTO.ViolationItemDTO> recentViolations = getRecentViolations();

            // 12. Top students
            List<DashboardStatsDTO.TopStudentDTO> topStudents = getTopStudents();

            // 13. Recent support requests
            List<DashboardStatsDTO.SupportRequestItemDTO> recentSupportRequests = getRecentSupportRequests();

            // 14. Recent complaints
            List<DashboardStatsDTO.ComplaintItemDTO> recentComplaints = getRecentComplaints();

            // 15. Recent feedbacks
            List<DashboardStatsDTO.FeedbackItemDTO> recentFeedbacks = getRecentFeedbacks();

            // 16. Recent seat status reports
            List<DashboardStatsDTO.SeatStatusReportItemDTO> recentSeatStatusReports = getRecentSeatStatusReports();

            // 17. Trend summary
            DashboardStatsDTO.TrendSummaryDTO trendSummary = getTrendSummary();

            return DashboardStatsDTO.builder()
                    .totalCheckInsToday(accessStats.getTotalCheckInsToday())
                    .totalCheckOutsToday(accessStats.getTotalCheckOutsToday())
                    .currentlyInLibrary(currentlyInLibrary)
                    .totalSeats(totalSeats)
                    .occupiedSeats(activeBookings)
                    .occupancyRate(occupancyRate)
                    .totalBookingsToday(totalBookingsToday)
                    .activeBookings(activeBookings)
                    .pendingBookings(pendingBookings)
                    .violationsToday(violationsToday)
                    .pendingViolations(pendingViolations)
                    .pendingSupportRequests(pendingSupportRequests)
                    .inProgressSupportRequests(inProgressSupportRequests)
                    .overdueSupportRequests(overdueSupportRequests)
                    .pendingSeatStatusReports(pendingSeatStatusReports)
                    .pendingComplaints(pendingComplaints)
                    .totalUsers(totalUsers)
                    .recentBookings(recentBookings)
                    .areaOccupancies(areaOccupancies)
                    .weeklyStats(weeklyStats)
                    .recentViolations(recentViolations)
                    .topStudents(topStudents)
                    .recentSupportRequests(recentSupportRequests)
                    .recentComplaints(recentComplaints)
                    .recentFeedbacks(recentFeedbacks)
                    .recentSeatStatusReports(recentSeatStatusReports)
                    .zoneOccupancies(zoneOccupancies)
                    .trendSummary(trendSummary)
                    .serverTime(now)
                    .build();

        } catch (Exception e) {
            log.error("Error building dashboard stats: {}", e.getMessage(), e);
            return DashboardStatsDTO.builder()
                    .recentBookings(Collections.emptyList())
                    .areaOccupancies(Collections.emptyList())
                    .weeklyStats(Collections.emptyList())
                    .recentViolations(Collections.emptyList())
                    .topStudents(Collections.emptyList())
                    .recentSupportRequests(Collections.emptyList())
                    .recentComplaints(Collections.emptyList())
                    .recentFeedbacks(Collections.emptyList())
                    .recentSeatStatusReports(Collections.emptyList())
                    .zoneOccupancies(Collections.emptyList())
                    .build();
        }
    }

    /**
     * Lightweight library status for mobile home screen
     */
    public Map<String, Object> getLibraryStatus() {
        try {
            AccessLogStatsDTO accessStats = checkInService.getTodayStats();
            long totalSeats = seatRepository.count();
            long currentlyInLibrary = Math.max(0, accessStats.getCurrentlyInLibrary());
            double occupancyRate = totalSeats > 0
                    ? Math.round((double) currentlyInLibrary / totalSeats * 10000.0) / 100.0
                    : 0;

            return Map.of(
                    "totalSeats", totalSeats,
                    "occupiedSeats", currentlyInLibrary,
                    "occupancyRate", occupancyRate,
                    "currentlyInLibrary", currentlyInLibrary);
        } catch (Exception e) {
            log.error("Error getting library status: {}", e.getMessage());
            return Map.of(
                    "totalSeats", 0L,
                    "occupiedSeats", 0L,
                    "occupancyRate", 0.0,
                    "currentlyInLibrary", 0L);
        }
    }

    private List<DashboardStatsDTO.WeeklyStatsDTO> getWeeklyStats() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate startDate = today.minusDays(6);
            LocalDateTime startDateTime = startDate.atStartOfDay();

            // Get check-in counts by day
            List<Object[]> checkInData = accessLogRepository.countCheckInsByDay(startDateTime);
            Map<LocalDate, Long> checkInMap = new HashMap<>();
            for (Object[] row : checkInData) {
                LocalDate date = ((Date) row[0]).toLocalDate();
                long count = ((Number) row[1]).longValue();
                checkInMap.put(date, count);
            }

            // Get booking counts by day
            List<Object[]> bookingData = reservationRepository.countBookingsByDay(startDateTime);
            Map<LocalDate, Long> bookingMap = new HashMap<>();
            for (Object[] row : bookingData) {
                LocalDate date = ((Date) row[0]).toLocalDate();
                long count = ((Number) row[1]).longValue();
                bookingMap.put(date, count);
            }

            // Build 7-day stats
            List<DashboardStatsDTO.WeeklyStatsDTO> result = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                LocalDate date = startDate.plusDays(i);
                DayOfWeek dow = date.getDayOfWeek();
                String dayName = dow.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("vi"));

                result.add(DashboardStatsDTO.WeeklyStatsDTO.builder()
                        .date(date)
                        .dayOfWeek(dayName)
                        .checkInCount(checkInMap.getOrDefault(date, 0L))
                        .bookingCount(bookingMap.getOrDefault(date, 0L))
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.error("Error calculating weekly stats: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Chart stats with range filter: week (7 days), month (30 days), year (12
     * months)
     */
    public List<Map<String, Object>> getChartStats(String range) {
        try {
            LocalDate today = LocalDate.now();

            if ("day".equalsIgnoreCase(range)) {
                LocalDateTime dayStartTime = today.atStartOfDay();

                List<Object[]> checkInData = accessLogRepository.countCheckInsByHour(dayStartTime);
                Map<Integer, Long> checkInByHour = new HashMap<>();
                for (Object[] row : checkInData) {
                    int hour = ((Number) row[0]).intValue();
                    checkInByHour.put(hour, ((Number) row[1]).longValue());
                }

                List<Object[]> bookingData = reservationRepository.countBookingsByHour(dayStartTime);
                Map<Integer, Long> bookingByHour = new HashMap<>();
                for (Object[] row : bookingData) {
                    int hour = ((Number) row[0]).intValue();
                    bookingByHour.put(hour, ((Number) row[1]).longValue());
                }

                List<Map<String, Object>> result = new ArrayList<>();
                for (int h = 6; h <= 22; h++) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("label", String.format("%02d:00", h));
                    item.put("checkInCount", checkInByHour.getOrDefault(h, 0L));
                    item.put("bookingCount", bookingByHour.getOrDefault(h, 0L));
                    result.add(item);
                }
                return result;

            } else if ("year".equalsIgnoreCase(range)) {
                // 12 months aggregation
                List<Map<String, Object>> result = new ArrayList<>();
                String[] monthNames = { "Th 1", "Th 2", "Th 3", "Th 4", "Th 5", "Th 6",
                        "Th 7", "Th 8", "Th 9", "Th 10", "Th 11", "Th 12" };

                LocalDate yearStart = today.withDayOfYear(1);
                LocalDateTime yearStartTime = yearStart.atStartOfDay();

                List<Object[]> checkInData = accessLogRepository.countCheckInsByDay(yearStartTime);
                List<Object[]> bookingData = reservationRepository.countBookingsByDay(yearStartTime);

                // Aggregate by month
                Map<Integer, Long> checkInByMonth = new HashMap<>();
                Map<Integer, Long> bookingByMonth = new HashMap<>();

                for (Object[] row : checkInData) {
                    LocalDate date = ((Date) row[0]).toLocalDate();
                    int month = date.getMonthValue();
                    checkInByMonth.merge(month, ((Number) row[1]).longValue(), Long::sum);
                }
                for (Object[] row : bookingData) {
                    LocalDate date = ((Date) row[0]).toLocalDate();
                    int month = date.getMonthValue();
                    bookingByMonth.merge(month, ((Number) row[1]).longValue(), Long::sum);
                }

                for (int m = 1; m <= 12; m++) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("label", monthNames[m - 1]);
                    item.put("checkInCount", checkInByMonth.getOrDefault(m, 0L));
                    item.put("bookingCount", bookingByMonth.getOrDefault(m, 0L));
                    result.add(item);
                }
                return result;

            } else if ("month".equalsIgnoreCase(range)) {
                // Aggregate by week within current month
                LocalDate monthStart = today.withDayOfMonth(1);
                LocalDateTime monthStartTime = monthStart.atStartOfDay();

                List<Object[]> checkInData = accessLogRepository.countCheckInsByDay(monthStartTime);
                List<Object[]> bookingData = reservationRepository.countBookingsByDay(monthStartTime);

                // Group daily data into weeks
                Map<Integer, Long> checkInByWeek = new HashMap<>();
                Map<Integer, Long> bookingByWeek = new HashMap<>();

                for (Object[] row : checkInData) {
                    LocalDate date = ((Date) row[0]).toLocalDate();
                    int weekNum = (date.getDayOfMonth() - 1) / 7 + 1;
                    checkInByWeek.merge(weekNum, ((Number) row[1]).longValue(), Long::sum);
                }
                for (Object[] row : bookingData) {
                    LocalDate date = ((Date) row[0]).toLocalDate();
                    int weekNum = (date.getDayOfMonth() - 1) / 7 + 1;
                    bookingByWeek.merge(weekNum, ((Number) row[1]).longValue(), Long::sum);
                }

                // Max 5 weeks in a month
                int totalWeeks = (today.lengthOfMonth() - 1) / 7 + 1;
                List<Map<String, Object>> result = new ArrayList<>();
                for (int w = 1; w <= totalWeeks; w++) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("label", "Tuần " + w);
                    item.put("checkInCount", checkInByWeek.getOrDefault(w, 0L));
                    item.put("bookingCount", bookingByWeek.getOrDefault(w, 0L));
                    result.add(item);
                }
                return result;

            } else {
                // week = 7 days, label by day of week
                LocalDate startDate = today.minusDays(6);
                LocalDateTime startDateTime = startDate.atStartOfDay();

                List<Object[]> checkInData = accessLogRepository.countCheckInsByDay(startDateTime);
                Map<LocalDate, Long> checkInMap = new HashMap<>();
                for (Object[] row : checkInData) {
                    LocalDate date = ((Date) row[0]).toLocalDate();
                    checkInMap.put(date, ((Number) row[1]).longValue());
                }

                List<Object[]> bookingData = reservationRepository.countBookingsByDay(startDateTime);
                Map<LocalDate, Long> bookingMap = new HashMap<>();
                for (Object[] row : bookingData) {
                    LocalDate date = ((Date) row[0]).toLocalDate();
                    bookingMap.put(date, ((Number) row[1]).longValue());
                }

                List<Map<String, Object>> result = new ArrayList<>();
                for (int i = 0; i <= 6; i++) {
                    LocalDate date = startDate.plusDays(i);
                    DayOfWeek dow = date.getDayOfWeek();
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("label", dow.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("vi")));
                    item.put("checkInCount", checkInMap.getOrDefault(date, 0L));
                    item.put("bookingCount", bookingMap.getOrDefault(date, 0L));
                    result.add(item);
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Error calculating chart stats for range {}: {}", range, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DashboardStatsDTO.ViolationItemDTO> getRecentViolations() {
        try {
            return violationReportRepository.findByStatusInOrderByCreatedAtDesc(
                    Set.of(
                            SeatViolationReportEntity.ReportStatus.PENDING,
                            SeatViolationReportEntity.ReportStatus.VERIFIED))
                    .stream()
                    .limit(5)
                    .map(v -> DashboardStatsDTO.ViolationItemDTO.builder()
                            .id(v.getId())
                            .violatorName(v.getViolator() != null ? v.getViolator().getFullName() : "N/A")
                            .violatorCode(v.getViolator() != null ? v.getViolator().getUserCode() : "N/A")
                            .avatarUrl(v.getViolator() != null ? v.getViolator().getAvtUrl() : null)
                            .violationType(v.getViolationType() != null ? v.getViolationType().name() : "N/A")
                            .status(v.getStatus() != null ? v.getStatus().name() : "N/A")
                            .createdAt(v.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching pending violations: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DashboardStatsDTO.TopStudentDTO> getTopStudents() {
        return getTopStudents("month");
    }

    /**
     * Top students with range filter: week, month, year
     */
    public List<DashboardStatsDTO.TopStudentDTO> getTopStudents(String range) {
        try {
            int days;
            if ("week".equalsIgnoreCase(range)) {
                days = 7;
            } else if ("year".equalsIgnoreCase(range)) {
                days = 365;
            } else {
                days = 30;
            }
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            List<Object[]> data = accessLogRepository.findTopStudentsByStudyTime(since);
            return data.stream()
                    .map(row -> DashboardStatsDTO.TopStudentDTO.builder()
                            .userId((UUID) row[0])
                            .fullName((String) row[1])
                            .userCode((String) row[2])
                            .totalVisits(((Number) row[3]).longValue())
                            .totalMinutes(((Number) row[4]).longValue())
                            .avatarUrl(row.length > 5 ? (String) row[5] : null)
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching top students for range {}: {}", range, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DashboardStatsDTO.SupportRequestItemDTO> getRecentSupportRequests() {
        try {
            return supportRequestRepository.findByStatusInOrderByCreatedAtDesc(
                    Set.of(SupportRequestStatus.PENDING, SupportRequestStatus.IN_PROGRESS)
            ).stream()
                    .limit(5)
                    .map(sr -> DashboardStatsDTO.SupportRequestItemDTO.builder()
                            .id(sr.getId())
                            .studentName(sr.getStudent() != null ? sr.getStudent().getFullName() : "N/A")
                            .studentCode(sr.getStudent() != null ? sr.getStudent().getUserCode() : "N/A")
                            .description(sr.getDescription() != null && sr.getDescription().length() > 80
                                    ? sr.getDescription().substring(0, 80) + "..."
                                    : sr.getDescription())
                            .status(sr.getStatus() != null ? sr.getStatus().name() : "N/A")
                            .createdAt(sr.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching pending support requests: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DashboardStatsDTO.ComplaintItemDTO> getRecentComplaints() {
        try {
            return complaintRepository.findByStatusOrderByCreatedAtDesc(
                    slib.com.example.entity.complaint.ComplaintEntity.ComplaintStatus.PENDING
            ).stream()
                    .limit(5)
                    .map(c -> DashboardStatsDTO.ComplaintItemDTO.builder()
                            .id(c.getId())
                            .userName(c.getUser() != null ? c.getUser().getFullName() : "N/A")
                            .userCode(c.getUser() != null ? c.getUser().getUserCode() : "N/A")
                            .subject(c.getSubject())
                            .status(c.getStatus() != null ? c.getStatus().name() : "N/A")
                            .createdAt(c.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching pending complaints: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DashboardStatsDTO.FeedbackItemDTO> getRecentFeedbacks() {
        try {
            return feedbackRepository.findByStatusOrderByCreatedAtDesc(
                    slib.com.example.entity.feedback.FeedbackEntity.FeedbackStatus.NEW
            ).stream()
                    .limit(5)
                    .map(f -> DashboardStatsDTO.FeedbackItemDTO.builder()
                            .id(f.getId())
                            .userName(f.getUser() != null ? f.getUser().getFullName() : "N/A")
                            .userCode(f.getUser() != null ? f.getUser().getUserCode() : "N/A")
                            .rating(f.getRating())
                            .content(f.getContent() != null && f.getContent().length() > 80
                                    ? f.getContent().substring(0, 80) + "..."
                                    : f.getContent())
                            .status(f.getStatus() != null ? f.getStatus().name() : "N/A")
                            .createdAt(f.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching new feedbacks: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DashboardStatsDTO.SeatStatusReportItemDTO> getRecentSeatStatusReports() {
        try {
            return seatStatusReportRepository.findByStatusInOrderByCreatedAtDesc(
                    Set.of(slib.com.example.entity.feedback.SeatStatusReportEntity.ReportStatus.PENDING)).stream()
                    .limit(5)
                    .map(report -> DashboardStatsDTO.SeatStatusReportItemDTO.builder()
                            .id(report.getId())
                            .userName(report.getUser() != null ? report.getUser().getFullName() : "N/A")
                            .userCode(report.getUser() != null ? report.getUser().getUserCode() : "N/A")
                            .seatCode(report.getSeat() != null ? report.getSeat().getSeatCode() : "N/A")
                            .zoneName(report.getSeat() != null && report.getSeat().getZone() != null
                                    ? report.getSeat().getZone().getZoneName()
                                    : "N/A")
                            .areaName(report.getSeat() != null && report.getSeat().getZone() != null
                                    && report.getSeat().getZone().getArea() != null
                                            ? report.getSeat().getZone().getArea().getAreaName()
                                            : "N/A")
                            .issueType(report.getIssueType() != null ? report.getIssueType().name() : "OTHER")
                            .status(report.getStatus() != null ? report.getStatus().name() : "N/A")
                            .description(report.getDescription())
                            .createdAt(report.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching recent seat status reports: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private DashboardStatsDTO.TrendSummaryDTO getTrendSummary() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            LocalDateTime startOfToday = today.atStartOfDay();
            LocalDateTime endOfToday = today.atTime(LocalTime.MAX);
            LocalDateTime startOfYesterday = yesterday.atStartOfDay();
            LocalDateTime endOfYesterday = yesterday.atTime(LocalTime.MAX);

            List<String> bookingStatuses = List.of("BOOKED", "CONFIRMED", "COMPLETED", "EXPIRED");

            return DashboardStatsDTO.TrendSummaryDTO.builder()
                    .checkInsToday(accessLogRepository.countByCheckInTimeBetween(startOfToday, endOfToday))
                    .checkInsYesterday(accessLogRepository.countByCheckInTimeBetween(startOfYesterday, endOfYesterday))
                    .bookingsToday(reservationRepository.countByStartTimeBetweenAndStatusIn(
                            startOfToday,
                            endOfToday,
                            bookingStatuses))
                    .bookingsYesterday(reservationRepository.countByStartTimeBetweenAndStatusIn(
                            startOfYesterday,
                            endOfYesterday,
                            bookingStatuses))
                    .violationsToday(violationReportRepository.countByCreatedAtBetween(startOfToday, endOfToday))
                    .violationsYesterday(violationReportRepository.countByCreatedAtBetween(startOfYesterday, endOfYesterday))
                    .supportToday(supportRequestRepository.countByCreatedAtBetween(startOfToday, endOfToday))
                    .supportYesterday(supportRequestRepository.countByCreatedAtBetween(startOfYesterday, endOfYesterday))
                    .build();
        } catch (Exception e) {
            log.error("Error calculating trend summary: {}", e.getMessage());
            return DashboardStatsDTO.TrendSummaryDTO.builder().build();
        }
    }

    private List<DashboardStatsDTO.ZoneOccupancyDTO> getZoneOccupancies(LocalDateTime now) {
        try {
            List<DashboardStatsDTO.ZoneOccupancyDTO> result = new ArrayList<>();
            List<Object[]> snapshots = reservationRepository.getZoneOccupancySnapshot(now);

            for (Object[] row : snapshots) {
                int zoneId = ((Number) row[0]).intValue();
                String zoneName = (String) row[1];
                String areaName = (String) row[2];
                long totalSeats = ((Number) row[3]).longValue();
                long occupiedSeats = ((Number) row[4]).longValue();
                double percentage = totalSeats > 0
                        ? Math.round((double) occupiedSeats / totalSeats * 10000.0) / 100.0
                        : 0;

                result.add(DashboardStatsDTO.ZoneOccupancyDTO.builder()
                        .zoneId(zoneId)
                        .zoneName(zoneName)
                        .areaName(areaName)
                        .totalSeats(totalSeats)
                        .occupiedSeats(occupiedSeats)
                        .occupancyPercentage(percentage)
                        .build());
            }

            return result;
        } catch (Exception e) {
            log.error("Error calculating zone occupancies: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DashboardStatsDTO.AreaOccupancyDTO> getAreaOccupancies(
            List<DashboardStatsDTO.ZoneOccupancyDTO> zoneOccupancies) {
        try {
            Map<String, List<DashboardStatsDTO.ZoneOccupancyDTO>> byArea = zoneOccupancies.stream()
                    .collect(Collectors.groupingBy(DashboardStatsDTO.ZoneOccupancyDTO::getAreaName));

            List<DashboardStatsDTO.AreaOccupancyDTO> result = areaRepository.findAll().stream()
                    .map(area -> {
                        List<DashboardStatsDTO.ZoneOccupancyDTO> zones = byArea.getOrDefault(
                                area.getAreaName(),
                                Collections.emptyList());
                        long areaTotalSeats = zones.stream()
                                .mapToLong(DashboardStatsDTO.ZoneOccupancyDTO::getTotalSeats)
                                .sum();
                        long areaOccupiedSeats = zones.stream()
                                .mapToLong(DashboardStatsDTO.ZoneOccupancyDTO::getOccupiedSeats)
                                .sum();
                        double percentage = areaTotalSeats > 0
                                ? Math.round((double) areaOccupiedSeats / areaTotalSeats * 10000.0) / 100.0
                                : 0;

                        return DashboardStatsDTO.AreaOccupancyDTO.builder()
                                .areaId(area.getAreaId())
                                .areaName(area.getAreaName())
                                .totalSeats(areaTotalSeats)
                                .occupiedSeats(areaOccupiedSeats)
                                .occupancyPercentage(percentage)
                                .build();
                    })
                    .collect(Collectors.toList());

            return result;
        } catch (Exception e) {
            log.error("Error calculating area occupancies: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
