package slib.com.example.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.system.SystemLogEntity.LogLevel;
import slib.com.example.repository.booking.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import slib.com.example.service.system.SystemLogService;

/**
 * Scheduler for sending notification reminders before bookings
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final ReservationRepository reservationRepository;
    private final PushNotificationService pushNotificationService;
    private final SystemLogService systemLogService;

    // Track sent reminders to avoid duplicates (in memory - will reset on restart)
    private final Set<UUID> sentReminders = ConcurrentHashMap.newKeySet();
    private final Set<UUID> sentExpiryWarnings = ConcurrentHashMap.newKeySet();

    /**
     * Run every minute to check for upcoming reservations
     * Sends reminders 15 minutes before booking start time
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    @Transactional(readOnly = true)
    public void sendBookingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(15);
        LocalDateTime expiryWarningTime = now.plusMinutes(10);

        // Find reservations starting in the next 15-16 minutes that haven't been
        // reminded
        List<ReservationEntity> upcomingReservations = reservationRepository
                .findByStatusIn(List.of("BOOKED")).stream()
                .filter(r -> r.getStartTime() != null)
                .filter(r -> {
                    LocalDateTime startTime = r.getStartTime();
                    return startTime.isAfter(now) && startTime.isBefore(reminderTime.plusMinutes(1));
                })
                .filter(r -> !sentReminders.contains(r.getReservationId()))
                .toList();

        for (ReservationEntity reservation : upcomingReservations) {
            try {
                sendReminderNotification(reservation);
                sentReminders.add(reservation.getReservationId());
                log.info("Sent reminder for reservation: {}", reservation.getReservationId());
            } catch (Exception e) {
                log.error("Failed to send reminder for reservation {}: {}", reservation.getReservationId(),
                        e.getMessage());
                systemLogService.logJobEvent(LogLevel.ERROR, "NotificationScheduler",
                        "Failed to send booking reminder: " + e.getMessage());
            }
        }

        List<ReservationEntity> endingReservations = reservationRepository
                .findByStatusIn(List.of("CONFIRMED")).stream()
                .filter(r -> r.getEndTime() != null)
                .filter(r -> {
                    LocalDateTime endTime = r.getEndTime();
                    return endTime.isAfter(now) && endTime.isBefore(expiryWarningTime.plusMinutes(1));
                })
                .filter(r -> !sentExpiryWarnings.contains(r.getReservationId()))
                .toList();

        for (ReservationEntity reservation : endingReservations) {
            try {
                sendExpiryWarningNotification(reservation);
                sentExpiryWarnings.add(reservation.getReservationId());
                log.info("Sent expiry warning for reservation: {}", reservation.getReservationId());
            } catch (Exception e) {
                log.error("Failed to send expiry warning for reservation {}: {}", reservation.getReservationId(),
                        e.getMessage());
                systemLogService.logJobEvent(LogLevel.ERROR, "NotificationScheduler",
                        "Failed to send expiry warning: " + e.getMessage());
            }
        }

        // Clean up old reminders (older than 1 hour) to prevent memory leak
        cleanupOldReminders();
    }

    private void sendReminderNotification(ReservationEntity reservation) {
        if (reservation.getUser() == null) {
            log.warn("Reservation {} has no user", reservation.getReservationId());
            return;
        }

        String seatCode = reservation.getSeat() != null ? reservation.getSeat().getSeatCode() : "N/A";
        String startTimeStr = reservation.getStartTime() != null
                ? reservation.getStartTime().toLocalTime().toString().substring(0, 5)
                : "N/A";

        String title = "Nhắc nhở đặt chỗ";
        String body = String.format(
                "Bạn có lịch đặt ghế %s lúc %s. Hãy đến đúng giờ để check-in!",
                seatCode, startTimeStr);

        pushNotificationService.sendToUser(
                reservation.getUser().getId(),
                title,
                body,
                NotificationType.REMINDER,
                reservation.getReservationId(),
                PushNotificationService.DELIVERY_KEY_CHECKIN_REMINDER);
    }

    private void sendExpiryWarningNotification(ReservationEntity reservation) {
        if (reservation.getUser() == null) {
            log.warn("Reservation {} has no user", reservation.getReservationId());
            return;
        }

        String seatCode = reservation.getSeat() != null ? reservation.getSeat().getSeatCode() : "N/A";
        String endTimeStr = reservation.getEndTime() != null
                ? reservation.getEndTime().toLocalTime().toString().substring(0, 5)
                : "N/A";

        String title = "Sắp hết giờ sử dụng";
        String body = String.format(
                "Phiên sử dụng ghế %s của bạn sẽ kết thúc lúc %s. Hãy chủ động lưu tài liệu và rời chỗ đúng giờ.",
                seatCode, endTimeStr);

        pushNotificationService.sendToUser(
                reservation.getUser().getId(),
                title,
                body,
                NotificationType.REMINDER,
                reservation.getReservationId(),
                PushNotificationService.DELIVERY_KEY_TIME_EXPIRY);
    }

    private void cleanupOldReminders() {
        // Simple cleanup - clear all if size exceeds threshold
        if (sentReminders.size() > 10000) {
            sentReminders.clear();
            log.info("Cleared reminder cache");
        }
        if (sentExpiryWarnings.size() > 10000) {
            sentExpiryWarnings.clear();
            log.info("Cleared expiry warning cache");
        }
    }
}
