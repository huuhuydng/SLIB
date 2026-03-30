package slib.com.example.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.entity.library.LibrarySetting;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.system.SystemLogEntity.LogLevel;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.feedback.SeatViolationReportRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import slib.com.example.service.system.LibrarySettingService;
import slib.com.example.service.system.SystemLogService;

/**
 * Scheduler gui bao cao tong ket hang tuan cho ADMIN.
 * Chay moi thu Hai luc 08:00 sang.
 * Chi gui khi notifyWeeklyReport = true trong cau hinh he thong.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyReportScheduler {

    private final LibrarySettingService librarySettingService;
    private final PushNotificationService pushNotificationService;
    private final ReservationRepository reservationRepository;
    private final SeatViolationReportRepository violationReportRepository;
    private final SystemLogService systemLogService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM");

    /**
     * Gui bao cao tuan cho tat ca ADMIN users.
     * Thong ke 7 ngay truoc do: tong dat cho, tong check-in, tong vi pham.
     */
    @Scheduled(cron = "0 0 8 * * MON")
    @Transactional(readOnly = true)
    public void sendWeeklyReport() {
        try {
            LibrarySetting settings = librarySettingService.getSettings();
            if (!Boolean.TRUE.equals(settings.getNotifyWeeklyReport())) {
                log.info("Bao cao tuan da bi tat trong cau hinh he thong, bo qua");
                return;
            }

            LocalDateTime weekEnd = LocalDate.now().atStartOfDay();
            LocalDateTime weekStart = weekEnd.minusDays(7);

            // Thong ke tuan
            var reservations = reservationRepository.findAll();
            long totalBookings = reservations.stream()
                    .filter(r -> r.getStartTime() != null)
                    .filter(r -> !r.getStartTime().isBefore(weekStart) && r.getStartTime().isBefore(weekEnd))
                    .filter(r -> Set.of("BOOKED", "CONFIRMED", "COMPLETED", "EXPIRED")
                            .contains(r.getStatus() != null ? r.getStatus().toUpperCase() : ""))
                    .count();
            long totalCheckIns = reservations.stream()
                    .filter(r -> r.getConfirmedAt() != null)
                    .filter(r -> !r.getConfirmedAt().isBefore(weekStart) && r.getConfirmedAt().isBefore(weekEnd))
                    .count();
            long totalViolations = violationReportRepository.countByCreatedAtBetween(weekStart, weekEnd);

            // Dinh dang khoang ngay
            String dateRange = weekStart.format(DATE_FMT) + " - " + weekEnd.minusDays(1).format(DATE_FMT);

            String title = "Bao cao tuan " + dateRange;
            String body = String.format(
                    "Tong ket tuan %s: %d luot dat cho, %d luot check-in, %d vi pham.",
                    dateRange, totalBookings, totalCheckIns, totalViolations);

            pushNotificationService.sendToRole("ADMIN", title, body, NotificationType.SYSTEM, null);

            log.info("Da gui bao cao tuan cho admin: {} bookings, {} check-ins, {} violations",
                    totalBookings, totalCheckIns, totalViolations);
            systemLogService.logJobEvent(LogLevel.INFO, "WeeklyReportScheduler",
                    "Sent weekly report: " + totalBookings + " bookings, "
                            + totalCheckIns + " check-ins, " + totalViolations + " violations");

        } catch (Exception e) {
            log.error("Loi khi gui bao cao tuan: {}", e.getMessage(), e);
            systemLogService.logJobEvent(LogLevel.ERROR, "WeeklyReportScheduler",
                    "Failed to send weekly report: " + e.getMessage());
        }
    }
}
