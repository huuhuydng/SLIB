package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.system.SystemLogEntity.LogLevel;
import slib.com.example.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * Run every minute to check for upcoming reservations
     * Sends reminders 15 minutes before booking start time
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    @Transactional(readOnly = true)
    public void sendBookingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(15);

        // Find reservations starting in the next 15-16 minutes that haven't been
        // reminded
        List<ReservationEntity> upcomingReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStartTime() != null)
                .filter(r -> "BOOKED".equals(r.getStatus()) || "CONFIRMED".equals(r.getStatus()))
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
                reservation.getReservationId());
    }

    private void cleanupOldReminders() {
        // Simple cleanup - clear all if size exceeds threshold
        if (sentReminders.size() > 10000) {
            sentReminders.clear();
            log.info("Cleared reminder cache");
        }
    }
}
