package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.dto.DashboardStatsDTO;
import slib.com.example.dto.hce.AccessLogStatsDTO;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.feedback.SeatViolationReportEntity;
import slib.com.example.entity.support.SupportRequestStatus;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.repository.*;
import slib.com.example.repository.support.SupportRequestRepository;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

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

    public DashboardStatsDTO getDashboardStats() {
        try {
            // 1. Check-in/out stats
            AccessLogStatsDTO accessStats = checkInService.getTodayStats();

            // 2. Seat stats
            long totalSeats = seatRepository.count();
            long currentlyInLibrary = Math.max(0, accessStats.getCurrentlyInLibrary());
            double occupancyRate = totalSeats > 0
                    ? Math.round((double) currentlyInLibrary / totalSeats * 10000.0) / 100.0
                    : 0;

            // 3. Booking stats
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
            long totalBookingsToday = reservationRepository.countByStatusAndCreatedAtBetween("BOOKED", startOfDay,
                    endOfDay);
            long activeBookings = reservationRepository.countByStatus("CONFIRMED");
            long pendingBookings = reservationRepository.countByStatus("BOOKED");

            // 4. Violations today
            long violationsToday = violationReportRepository.countByCreatedAtBetween(startOfDay, endOfDay);
            long pendingViolations = violationReportRepository.countByStatus(
                    SeatViolationReportEntity.ReportStatus.PENDING);

            // 5. Support requests
            long pendingSupportRequests = supportRequestRepository.countByStatus(SupportRequestStatus.PENDING);
            long inProgressSupportRequests = supportRequestRepository.countByStatus(SupportRequestStatus.IN_PROGRESS);

            // 6. Total users
            long totalUsers = userRepository.count();

            // 7. Recent bookings (top 7)
            List<ReservationEntity> recentReservations = reservationRepository
                    .findTop7ByStatusOrderByCreatedAtDesc("BOOKED");
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

            // 8. Area occupancy (real data)
            List<DashboardStatsDTO.AreaOccupancyDTO> areaOccupancies = getAreaOccupancies();

            // 9. Weekly stats (7 days)
            List<DashboardStatsDTO.WeeklyStatsDTO> weeklyStats = getWeeklyStats();

            // 10. Recent violations (top 5)
            List<DashboardStatsDTO.ViolationItemDTO> recentViolations = getRecentViolations();

            // 11. Top students
            List<DashboardStatsDTO.TopStudentDTO> topStudents = getTopStudents();

            // 12. Recent support requests
            List<DashboardStatsDTO.SupportRequestItemDTO> recentSupportRequests = getRecentSupportRequests();

            // 13. Recent complaints
            List<DashboardStatsDTO.ComplaintItemDTO> recentComplaints = getRecentComplaints();

            // 14. Recent feedbacks
            List<DashboardStatsDTO.FeedbackItemDTO> recentFeedbacks = getRecentFeedbacks();

            // 15. Zone occupancy
            List<DashboardStatsDTO.ZoneOccupancyDTO> zoneOccupancies = getZoneOccupancies();

            return DashboardStatsDTO.builder()
                    .totalCheckInsToday(accessStats.getTotalCheckInsToday())
                    .totalCheckOutsToday(accessStats.getTotalCheckOutsToday())
                    .currentlyInLibrary(currentlyInLibrary)
                    .totalSeats(totalSeats)
                    .occupiedSeats(currentlyInLibrary)
                    .occupancyRate(occupancyRate)
                    .totalBookingsToday(totalBookingsToday)
                    .activeBookings(activeBookings)
                    .pendingBookings(pendingBookings)
                    .violationsToday(violationsToday)
                    .pendingViolations(pendingViolations)
                    .pendingSupportRequests(pendingSupportRequests)
                    .inProgressSupportRequests(inProgressSupportRequests)
                    .totalUsers(totalUsers)
                    .recentBookings(recentBookings)
                    .areaOccupancies(areaOccupancies)
                    .weeklyStats(weeklyStats)
                    .recentViolations(recentViolations)
                    .topStudents(topStudents)
                    .recentSupportRequests(recentSupportRequests)
                    .recentComplaints(recentComplaints)
                    .recentFeedbacks(recentFeedbacks)
                    .zoneOccupancies(zoneOccupancies)
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
                    .zoneOccupancies(Collections.emptyList())
                    .build();
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

    private List<DashboardStatsDTO.ViolationItemDTO> getRecentViolations() {
        try {
            return violationReportRepository.findTop5ByOrderByCreatedAtDesc().stream()
                    .map(v -> DashboardStatsDTO.ViolationItemDTO.builder()
                            .id(v.getId())
                            .violatorName(v.getViolator() != null ? v.getViolator().getFullName() : "N/A")
                            .violatorCode(v.getViolator() != null ? v.getViolator().getUserCode() : "N/A")
                            .violationType(v.getViolationType() != null ? v.getViolationType().name() : "N/A")
                            .status(v.getStatus() != null ? v.getStatus().name() : "N/A")
                            .createdAt(v.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching recent violations: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DashboardStatsDTO.TopStudentDTO> getTopStudents() {
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            List<Object[]> data = accessLogRepository.findTopStudentsByStudyTime(thirtyDaysAgo);
            return data.stream()
                    .map(row -> DashboardStatsDTO.TopStudentDTO.builder()
                            .userId((UUID) row[0])
                            .fullName((String) row[1])
                            .userCode((String) row[2])
                            .totalVisits(((Number) row[3]).longValue())
                            .totalMinutes(((Number) row[4]).longValue())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching top students: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DashboardStatsDTO.SupportRequestItemDTO> getRecentSupportRequests() {
        try {
            return supportRequestRepository.findTop5ByOrderByCreatedAtDesc().stream()
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
            log.error("Error fetching recent support requests: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DashboardStatsDTO.ComplaintItemDTO> getRecentComplaints() {
        try {
            return complaintRepository.findTop5ByOrderByCreatedAtDesc().stream()
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
            log.error("Error fetching recent complaints: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DashboardStatsDTO.FeedbackItemDTO> getRecentFeedbacks() {
        try {
            return feedbackRepository.findTop5ByOrderByCreatedAtDesc().stream()
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
            log.error("Error fetching recent feedbacks: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DashboardStatsDTO.ZoneOccupancyDTO> getZoneOccupancies() {
        try {
            var areas = areaRepository.findAll();
            List<DashboardStatsDTO.ZoneOccupancyDTO> result = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (var area : areas) {
                List<ZoneEntity> zones = zoneRepository.findByArea_AreaId(area.getAreaId());

                for (ZoneEntity zone : zones) {
                    List<SeatEntity> seats = seatRepository.findByZone_ZoneId(zone.getZoneId());
                    long zoneTotalSeats = seats.size();
                    long zoneOccupiedSeats = 0;

                    for (SeatEntity seat : seats) {
                        List<ReservationEntity> activeReservations = reservationRepository
                                .findOverlappingReservations(seat.getSeatId(), now.minusHours(1), now.plusHours(1));
                        if (!activeReservations.isEmpty()) {
                            zoneOccupiedSeats++;
                        }
                    }

                    double percentage = zoneTotalSeats > 0
                            ? Math.round((double) zoneOccupiedSeats / zoneTotalSeats * 10000.0) / 100.0
                            : 0;

                    result.add(DashboardStatsDTO.ZoneOccupancyDTO.builder()
                            .zoneId(zone.getZoneId())
                            .zoneName(zone.getZoneName())
                            .areaName(area.getAreaName())
                            .totalSeats(zoneTotalSeats)
                            .occupiedSeats(zoneOccupiedSeats)
                            .occupancyPercentage(percentage)
                            .build());
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Error calculating zone occupancies: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DashboardStatsDTO.AreaOccupancyDTO> getAreaOccupancies() {
        try {
            var areas = areaRepository.findAll();
            List<DashboardStatsDTO.AreaOccupancyDTO> result = new ArrayList<>();

            for (var area : areas) {
                List<ZoneEntity> zones = zoneRepository.findByArea_AreaId(area.getAreaId());
                long areaTotalSeats = 0;
                long areaOccupiedSeats = 0;

                for (ZoneEntity zone : zones) {
                    List<SeatEntity> seats = seatRepository.findByZone_ZoneId(zone.getZoneId());
                    areaTotalSeats += seats.size();

                    LocalDateTime now = LocalDateTime.now();
                    for (SeatEntity seat : seats) {
                        List<ReservationEntity> activeReservations = reservationRepository
                                .findOverlappingReservations(seat.getSeatId(), now.minusHours(1), now.plusHours(1));
                        if (!activeReservations.isEmpty()) {
                            areaOccupiedSeats++;
                        }
                    }
                }

                double percentage = areaTotalSeats > 0
                        ? Math.round((double) areaOccupiedSeats / areaTotalSeats * 10000.0) / 100.0
                        : 0;

                result.add(DashboardStatsDTO.AreaOccupancyDTO.builder()
                        .areaId(area.getAreaId())
                        .areaName(area.getAreaName())
                        .totalSeats(areaTotalSeats)
                        .occupiedSeats(areaOccupiedSeats)
                        .occupancyPercentage(percentage)
                        .build());
            }

            return result;
        } catch (Exception e) {
            log.error("Error calculating area occupancies: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
