package slib.com.example.service.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.dto.dashboard.DashboardStatsDTO;
import slib.com.example.entity.chat.Conversation;
import slib.com.example.entity.chat.ConversationStatus;
import slib.com.example.entity.chat.Message;
import slib.com.example.dto.hce.AccessLogStatsDTO;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.feedback.SeatViolationReportEntity;
import slib.com.example.entity.hce.AccessLog;
import slib.com.example.entity.support.SupportRequestStatus;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.chat.ConversationRepository;
import slib.com.example.repository.chat.MessageRepository;
import slib.com.example.repository.complaint.ComplaintRepository;
import slib.com.example.repository.feedback.FeedbackRepository;
import slib.com.example.repository.feedback.SeatStatusReportRepository;
import slib.com.example.repository.feedback.SeatViolationReportRepository;
import slib.com.example.repository.hce.AccessLogRepository;
import slib.com.example.repository.support.SupportRequestRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.repository.zone_config.AreaRepository;
import slib.com.example.repository.zone_config.SeatRepository;
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
    private static final long ADMIN_PRIORITY_SUPPORT_MINUTES = 10;

    private final CheckInService checkInService;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final SeatViolationReportRepository violationReportRepository;
    private final SupportRequestRepository supportRequestRepository;
    private final AreaRepository areaRepository;
    private final AccessLogRepository accessLogRepository;
    private final ComplaintRepository complaintRepository;
    private final FeedbackRepository feedbackRepository;
    private final SeatStatusReportRepository seatStatusReportRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public DashboardStatsDTO getDashboardStats() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
            List<String> bookingStatuses = List.of("PROCESSING", "BOOKED", "CONFIRMED", "COMPLETED", "EXPIRED");

            // 1. Check-in/out stats
            AccessLogStatsDTO accessStats = checkInService.getTodayStats();

            // 2. Seat stats
            long totalSeats = seatRepository.countByIsActiveTrue();
            long currentlyInLibrary = Math.max(0, accessStats.getCurrentlyInLibrary());
            long occupiedSeats = reservationRepository.countActiveReservationsAtTime(now, List.of("CONFIRMED"));
            long reservedSeats = reservationRepository.countActiveReservationsAtTime(now, List.of("BOOKED"));
            long activeBookings = occupiedSeats + reservedSeats;
            double occupancyRate = totalSeats > 0
                    ? Math.round((double) occupiedSeats / totalSeats * 10000.0) / 100.0
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
                    Set.of(SeatViolationReportEntity.ReportStatus.PENDING));

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

            // 18. Priority tasks for admin
            List<DashboardStatsDTO.PriorityTaskDTO> priorityTasks = getPriorityTasks(now);

            // 19. Chat attention summary
            DashboardStatsDTO.ChatAttentionDTO chatAttention = getChatAttention();

            // 20. Zones that need attention
            List<DashboardStatsDTO.AttentionZoneDTO> attentionZones = getAttentionZones(zoneOccupancies);

            // 21. Recent operational activity feed
            List<DashboardStatsDTO.ActivityFeedItemDTO> recentActivities = getRecentActivities();

            return DashboardStatsDTO.builder()
                    .totalCheckInsToday(accessStats.getTotalCheckInsToday())
                    .totalCheckOutsToday(accessStats.getTotalCheckOutsToday())
                    .currentlyInLibrary(currentlyInLibrary)
                    .totalSeats(totalSeats)
                    .occupiedSeats(occupiedSeats)
                    .reservedSeats(reservedSeats)
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
                    .priorityTasks(priorityTasks)
                    .chatAttention(chatAttention)
                    .attentionZones(attentionZones)
                    .recentActivities(recentActivities)
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
                    .priorityTasks(Collections.emptyList())
                    .attentionZones(Collections.emptyList())
                    .recentActivities(Collections.emptyList())
                    .chatAttention(DashboardStatsDTO.ChatAttentionDTO.builder().targetPath("/librarian/chat").build())
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
                    Set.of(SeatViolationReportEntity.ReportStatus.PENDING))
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
            List<Object[]> data = reservationRepository.findTopStudentsByReservationTime(since);
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

    private List<DashboardStatsDTO.PriorityTaskDTO> getPriorityTasks(LocalDateTime now) {
        try {
            long overdueSupport10m = supportRequestRepository.countByStatusAndCreatedAtBefore(
                    SupportRequestStatus.PENDING,
                    now.minusMinutes(ADMIN_PRIORITY_SUPPORT_MINUTES));
            long waitingChats = conversationRepository.countByStatus(ConversationStatus.QUEUE_WAITING);
            long pendingViolations = violationReportRepository.countByStatusIn(
                    Set.of(SeatViolationReportEntity.ReportStatus.PENDING));
            long pendingSeatReports = seatStatusReportRepository.countByStatusIn(
                    Set.of(
                            slib.com.example.entity.feedback.SeatStatusReportEntity.ReportStatus.PENDING));
            long pendingComplaints = complaintRepository.countByStatus(
                    slib.com.example.entity.complaint.ComplaintEntity.ComplaintStatus.PENDING);
            long newFeedbacks = feedbackRepository.countByStatus(
                    slib.com.example.entity.feedback.FeedbackEntity.FeedbackStatus.NEW);

            List<DashboardStatsDTO.PriorityTaskDTO> tasks = new ArrayList<>();
            tasks.add(buildPriorityTask(
                    "support-overdue",
                    "Yêu cầu hỗ trợ quá 10 phút",
                    "Các yêu cầu hỗ trợ mới chưa được nhận xử lý trong ngưỡng theo dõi.",
                    overdueSupport10m,
                    overdueSupport10m > 0 ? "critical" : "normal",
                    null));
            tasks.add(buildPriorityTask(
                    "chat-waiting",
                    "Phiên chat đang chờ thủ thư",
                    "Sinh viên đã yêu cầu gặp thủ thư nhưng chưa có người tiếp nhận.",
                    waitingChats,
                    waitingChats > 0 ? "critical" : "normal",
                    null));
            tasks.add(buildPriorityTask(
                    "seat-status",
                    "Báo cáo ghế chưa xác minh",
                    "Các sự cố ghế hoặc thiết bị đang chờ xác minh tại hiện trường.",
                    pendingSeatReports,
                    pendingSeatReports > 0 ? "warning" : "normal",
                    null));
            tasks.add(buildPriorityTask(
                    "violations",
                    "Vi phạm chưa khép lại",
                    "Các báo cáo vi phạm còn đang chờ xác minh hoặc xử lý tiếp theo.",
                    pendingViolations,
                    pendingViolations > 0 ? "warning" : "normal",
                    null));
            tasks.add(buildPriorityTask(
                    "complaints",
                    "Khiếu nại chờ phản hồi",
                    "Các đơn khiếu nại mới từ sinh viên chưa có quyết định cuối cùng.",
                    pendingComplaints,
                    pendingComplaints > 0 ? "warning" : "normal",
                    null));
            tasks.add(buildPriorityTask(
                    "feedback",
                    "Phản hồi mới chưa xem",
                    "Các góp ý mới đang chờ bộ phận vận hành đọc và phản hồi.",
                    newFeedbacks,
                    newFeedbacks > 0 ? "normal" : "normal",
                    null));

            return tasks.stream()
                    .sorted(Comparator
                            .comparingInt((DashboardStatsDTO.PriorityTaskDTO task) -> prioritySeverityScore(task.getSeverity()))
                            .reversed()
                            .thenComparing(DashboardStatsDTO.PriorityTaskDTO::getCount, Comparator.reverseOrder()))
                    .limit(6)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error building priority tasks: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private DashboardStatsDTO.PriorityTaskDTO buildPriorityTask(
            String key,
            String title,
            String description,
            long count,
            String severity,
            String targetPath) {
        return DashboardStatsDTO.PriorityTaskDTO.builder()
                .key(key)
                .title(title)
                .description(description)
                .count(count)
                .severity(severity)
                .targetPath(targetPath)
                .build();
    }

    private int prioritySeverityScore(String severity) {
        return switch ((severity == null ? "" : severity).toLowerCase(Locale.ROOT)) {
            case "critical" -> 3;
            case "warning" -> 2;
            default -> 1;
        };
    }

    private DashboardStatsDTO.ChatAttentionDTO getChatAttention() {
        try {
            long waitingCount = conversationRepository.countByStatus(ConversationStatus.QUEUE_WAITING);
            long activeCount = conversationRepository.countByStatus(ConversationStatus.HUMAN_CHATTING);

            Conversation latestConversation = conversationRepository.findByStatusIn(
                    List.of(ConversationStatus.QUEUE_WAITING, ConversationStatus.HUMAN_CHATTING)).stream()
                    .findFirst()
                    .orElse(null);

            String latestStudentName = null;
            String latestStudentCode = null;
            String latestMessagePreview = null;
            LocalDateTime latestMessageAt = null;

            if (latestConversation != null) {
                latestStudentName = latestConversation.getStudent() != null
                        ? latestConversation.getStudent().getFullName()
                        : null;
                latestStudentCode = latestConversation.getStudent() != null
                        ? latestConversation.getStudent().getUserCode()
                        : null;

                Optional<Message> latestMessage = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(
                        latestConversation.getId());
                if (latestMessage.isPresent()) {
                    latestMessagePreview = abbreviateMessage(latestMessage.get().getContent(), latestMessage.get().getAttachmentUrl());
                    latestMessageAt = latestMessage.get().getCreatedAt();
                } else {
                    latestMessagePreview = latestConversation.getEscalationReason();
                    latestMessageAt = latestConversation.getUpdatedAt();
                }
            }

            long oldestWaitingMinutes = conversationRepository.findByStatusOrderByEscalatedAtAsc(ConversationStatus.QUEUE_WAITING)
                    .stream()
                    .findFirst()
                    .map(conversation -> {
                        LocalDateTime anchor = conversation.getEscalatedAt() != null
                                ? conversation.getEscalatedAt()
                                : conversation.getCreatedAt();
                        return Math.max(0L, java.time.Duration.between(anchor, LocalDateTime.now()).toMinutes());
                    })
                    .orElse(0L);

            return DashboardStatsDTO.ChatAttentionDTO.builder()
                    .waitingCount(waitingCount)
                    .activeCount(activeCount)
                    .latestStudentName(latestStudentName)
                    .latestStudentCode(latestStudentCode)
                    .latestMessagePreview(latestMessagePreview)
                    .latestMessageAt(latestMessageAt)
                    .oldestWaitingMinutes(oldestWaitingMinutes)
                    .targetPath("/librarian/chat")
                    .build();
        } catch (Exception e) {
            log.error("Error building chat attention: {}", e.getMessage());
            return DashboardStatsDTO.ChatAttentionDTO.builder()
                    .targetPath("/librarian/chat")
                    .build();
        }
    }

    private String abbreviateMessage(String content, String attachmentUrl) {
        if (content != null && !content.isBlank()) {
            return content.length() > 96 ? content.substring(0, 96) + "..." : content;
        }
        if (attachmentUrl != null && !attachmentUrl.isBlank()) {
            return "Sinh viên vừa gửi một tệp đính kèm.";
        }
        return "Có cập nhật mới trong phiên chat.";
    }

    private String translateViolationType(String type) {
        return switch ((type == null ? "OTHER" : type).toUpperCase(Locale.ROOT)) {
            case "UNAUTHORIZED_USE" -> "Sử dụng ghế không đúng quy định";
            case "LEFT_BELONGINGS" -> "Để đồ giữ chỗ";
            case "NOISE" -> "Gây ồn ào";
            case "FEET_ON_SEAT" -> "Gác chân lên ghế";
            case "FOOD_DRINK" -> "Ăn uống trong thư viện";
            case "SLEEPING" -> "Ngủ tại chỗ ngồi";
            default -> "Vi phạm nội quy";
        };
    }

    private List<DashboardStatsDTO.AttentionZoneDTO> getAttentionZones(
            List<DashboardStatsDTO.ZoneOccupancyDTO> zoneOccupancies) {
        try {
            Map<Integer, Long> pendingSeatReportsByZone = seatStatusReportRepository
                    .findByStatusInOrderByCreatedAtDesc(
                            Set.of(
                                    slib.com.example.entity.feedback.SeatStatusReportEntity.ReportStatus.PENDING))
                    .stream()
                    .filter(report -> report.getSeat() != null && report.getSeat().getZone() != null)
                    .collect(Collectors.groupingBy(
                            report -> report.getSeat().getZone().getZoneId(),
                            Collectors.counting()));

            Map<Integer, Long> pendingViolationsByZone = violationReportRepository
                    .findByStatusInOrderByCreatedAtDesc(
                            Set.of(
                                    SeatViolationReportEntity.ReportStatus.PENDING))
                    .stream()
                    .filter(report -> report.getSeat() != null && report.getSeat().getZone() != null)
                    .collect(Collectors.groupingBy(
                            report -> report.getSeat().getZone().getZoneId(),
                            Collectors.counting()));

            return zoneOccupancies.stream()
                    .map(zone -> {
                        long pendingSeatReports = pendingSeatReportsByZone.getOrDefault(zone.getZoneId(), 0L);
                        long pendingViolations = pendingViolationsByZone.getOrDefault(zone.getZoneId(), 0L);
                        double occupancy = zone.getOccupancyPercentage();
                        String severity = resolveAttentionZoneSeverity(occupancy, pendingSeatReports, pendingViolations);
                        if ("normal".equals(severity)) {
                            return null;
                        }

                        return DashboardStatsDTO.AttentionZoneDTO.builder()
                                .zoneId(zone.getZoneId())
                                .zoneName(zone.getZoneName())
                                .areaName(zone.getAreaName())
                                .occupiedSeats(zone.getOccupiedSeats())
                                .totalSeats(zone.getTotalSeats())
                                .occupancyPercentage(occupancy)
                                .pendingSeatReports(pendingSeatReports)
                                .pendingViolations(pendingViolations)
                                .severity(severity)
                                .reason(buildAttentionZoneReason(occupancy, pendingSeatReports, pendingViolations))
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator
                            .comparingInt((DashboardStatsDTO.AttentionZoneDTO zone) -> prioritySeverityScore(zone.getSeverity()))
                            .reversed()
                            .thenComparing(DashboardStatsDTO.AttentionZoneDTO::getOccupancyPercentage, Comparator.reverseOrder())
                            .thenComparing(DashboardStatsDTO.AttentionZoneDTO::getPendingSeatReports, Comparator.reverseOrder())
                            .thenComparing(DashboardStatsDTO.AttentionZoneDTO::getPendingViolations, Comparator.reverseOrder()))
                    .limit(4)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error building attention zones: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String resolveAttentionZoneSeverity(double occupancy, long pendingSeatReports, long pendingViolations) {
        if (occupancy >= 90 || pendingSeatReports >= 3 || pendingViolations >= 2) {
            return "critical";
        }
        if (occupancy >= 75 || pendingSeatReports > 0 || pendingViolations > 0) {
            return "warning";
        }
        return "normal";
    }

    private String buildAttentionZoneReason(double occupancy, long pendingSeatReports, long pendingViolations) {
        List<String> reasons = new ArrayList<>();
        if (occupancy >= 90) {
            reasons.add(String.format(Locale.US, "Mật độ sử dụng %.0f%%", occupancy));
        } else if (occupancy >= 75) {
            reasons.add(String.format(Locale.US, "Đang sử dụng %.0f%% số ghế", occupancy));
        }
        if (pendingSeatReports > 0) {
            reasons.add(pendingSeatReports + " báo cáo ghế chờ xử lý");
        }
        if (pendingViolations > 0) {
            reasons.add(pendingViolations + " vi phạm chưa khép lại");
        }
        if (reasons.isEmpty()) {
            return "Khu vực đang cần theo dõi kỹ hơn.";
        }
        return String.join(" · ", reasons);
    }

    private List<DashboardStatsDTO.ActivityFeedItemDTO> getRecentActivities() {
        try {
            List<DashboardStatsDTO.ActivityFeedItemDTO> items = new ArrayList<>();

            accessLogRepository.findRecentLogs(PageRequest.of(0, 6)).forEach(logItem -> items.add(
                    DashboardStatsDTO.ActivityFeedItemDTO.builder()
                            .type(resolveAccessLogType(logItem))
                            .title(resolveAccessLogTitle(logItem))
                            .description(resolveAccessLogDescription(logItem))
                            .actorName(logItem.getUser() != null ? logItem.getUser().getFullName() : "Sinh viên")
                            .actorCode(logItem.getUser() != null ? logItem.getUser().getUserCode() : null)
                            .severity("info")
                            .targetPath(null)
                            .createdAt(resolveAccessLogTime(logItem))
                            .build()));

            supportRequestRepository.findTop5ByOrderByCreatedAtDesc().stream()
                    .limit(3)
                    .forEach(request -> items.add(DashboardStatsDTO.ActivityFeedItemDTO.builder()
                            .type("support")
                            .title("Yêu cầu hỗ trợ mới")
                            .description(truncate(request.getDescription(), 92))
                            .actorName(request.getStudent() != null ? request.getStudent().getFullName() : "Sinh viên")
                            .actorCode(request.getStudent() != null ? request.getStudent().getUserCode() : null)
                            .severity("warning")
                            .createdAt(request.getCreatedAt())
                            .build()));

            seatStatusReportRepository.findTop5ByOrderByCreatedAtDesc().stream()
                    .limit(3)
                    .forEach(report -> items.add(DashboardStatsDTO.ActivityFeedItemDTO.builder()
                            .type("seat-report")
                            .title("Báo cáo tình trạng ghế")
                            .description(String.format(
                                    "%s tại %s",
                                    report.getSeat() != null ? report.getSeat().getSeatCode() : "Ghế không xác định",
                                    report.getSeat() != null && report.getSeat().getZone() != null
                                            ? report.getSeat().getZone().getZoneName()
                                            : "khu vực chưa rõ"))
                            .actorName(report.getUser() != null ? report.getUser().getFullName() : "Sinh viên")
                            .actorCode(report.getUser() != null ? report.getUser().getUserCode() : null)
                            .severity("warning")
                            .createdAt(report.getCreatedAt())
                            .build()));

            complaintRepository.findTop5ByOrderByCreatedAtDesc().stream()
                    .limit(2)
                    .forEach(complaint -> items.add(DashboardStatsDTO.ActivityFeedItemDTO.builder()
                            .type("complaint")
                            .title("Khiếu nại mới")
                            .description(truncate(complaint.getSubject(), 92))
                            .actorName(complaint.getUser() != null ? complaint.getUser().getFullName() : "Sinh viên")
                            .actorCode(complaint.getUser() != null ? complaint.getUser().getUserCode() : null)
                            .severity("info")
                            .createdAt(complaint.getCreatedAt())
                            .build()));

            violationReportRepository.findTop5ByOrderByCreatedAtDesc().stream()
                    .limit(2)
                    .forEach(violation -> items.add(DashboardStatsDTO.ActivityFeedItemDTO.builder()
                            .type("violation")
                            .title("Vi phạm mới được ghi nhận")
                            .description(translateViolationType(violation.getViolationType() != null
                                    ? violation.getViolationType().name()
                                    : "OTHER"))
                            .actorName(violation.getViolator() != null ? violation.getViolator().getFullName() : "Sinh viên")
                            .actorCode(violation.getViolator() != null ? violation.getViolator().getUserCode() : null)
                            .severity("critical")
                            .createdAt(violation.getCreatedAt())
                            .build()));

            return items.stream()
                    .filter(item -> item.getCreatedAt() != null)
                    .sorted(Comparator.comparing(DashboardStatsDTO.ActivityFeedItemDTO::getCreatedAt).reversed())
                    .limit(8)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error building recent activities: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String resolveAccessLogType(AccessLog accessLog) {
        return accessLog.getCheckOutTime() != null ? "check-out" : "check-in";
    }

    private String resolveAccessLogTitle(AccessLog accessLog) {
        return accessLog.getCheckOutTime() != null ? "Sinh viên rời thư viện" : "Sinh viên vào thư viện";
    }

    private String resolveAccessLogDescription(AccessLog accessLog) {
        if (accessLog.getDeviceId() == null || accessLog.getDeviceId().isBlank()) {
            return "Nhật ký ra vào vừa được ghi nhận.";
        }
        return "Ghi nhận từ thiết bị " + accessLog.getDeviceId() + ".";
    }

    private LocalDateTime resolveAccessLogTime(AccessLog accessLog) {
        return accessLog.getCheckOutTime() != null ? accessLog.getCheckOutTime() : accessLog.getCheckInTime();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
    }
}
