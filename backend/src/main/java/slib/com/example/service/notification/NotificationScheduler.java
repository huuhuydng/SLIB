package slib.com.example.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.library.LibrarySetting;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.system.SystemLogEntity.LogLevel;
import slib.com.example.repository.booking.ReservationRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import slib.com.example.service.system.SystemLogService;
import slib.com.example.service.system.LibrarySettingService;

/**
 * Scheduler for sending notification reminders before bookings
 */
@Service
@Slf4j
public class NotificationScheduler {
    private static final String RESERVATION_REFERENCE_TYPE = "RESERVATION";

    private final ReservationRepository reservationRepository;
    private final PushNotificationService pushNotificationService;
    private final SystemLogService systemLogService;
    private final LibrarySettingService librarySettingService;
    private final TransactionTemplate readOnlyTransactionTemplate;

    // Track sent reminders to avoid duplicates (in memory - will reset on restart)
    private final Set<UUID> sentReminders = ConcurrentHashMap.newKeySet();
    private final Set<UUID> sentExpiryWarnings = ConcurrentHashMap.newKeySet();
    private final Set<UUID> sentSeatStartNotifications = ConcurrentHashMap.newKeySet();

    public NotificationScheduler(ReservationRepository reservationRepository,
            PushNotificationService pushNotificationService,
            SystemLogService systemLogService,
            LibrarySettingService librarySettingService,
            PlatformTransactionManager transactionManager) {
        this.reservationRepository = reservationRepository;
        this.pushNotificationService = pushNotificationService;
        this.systemLogService = systemLogService;
        this.librarySettingService = librarySettingService;
        this.readOnlyTransactionTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTransactionTemplate.setReadOnly(true);
    }

    /**
     * Run every minute to check for upcoming reservations
     * Sends reminders 15 minutes before booking start time
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    public void sendBookingReminders() {
        LibrarySetting settings = librarySettingService.getSettings();
        int bookingReminderLeadMinutes = settings.getBookingReminderLeadMinutes() != null
                ? settings.getBookingReminderLeadMinutes()
                : 15;
        int expiryWarningLeadMinutes = settings.getExpiryWarningLeadMinutes() != null
                ? settings.getExpiryWarningLeadMinutes()
                : 10;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(bookingReminderLeadMinutes);
        LocalDateTime expiryWarningTime = now.plusMinutes(expiryWarningLeadMinutes);
        LocalDateTime seatStartWindowStart = now.minusSeconds(30);
        LocalDateTime seatStartWindowEnd = now.plusSeconds(45);

        List<ReminderPayload> upcomingReservations = loadUpcomingReminders(now, reminderTime.plusMinutes(1));
        for (ReminderPayload reservation : upcomingReservations) {
            try {
                sendReminderNotification(reservation);
                sentReminders.add(reservation.reservationId());
                log.info("Sent reminder for reservation: {}", reservation.reservationId());
            } catch (Exception e) {
                log.error("Failed to send reminder for reservation {}: {}", reservation.reservationId(),
                        e.getMessage());
                systemLogService.logJobEvent(LogLevel.ERROR, "NotificationScheduler",
                        "Failed to send booking reminder: " + e.getMessage());
            }
        }

        List<ReminderPayload> startingReservations = loadSeatStartNotifications(seatStartWindowStart, seatStartWindowEnd);
        for (ReminderPayload reservation : startingReservations) {
            try {
                sendSeatStartNotification(reservation);
                sentSeatStartNotifications.add(reservation.reservationId());
                log.info("Sent seat start notification for reservation: {}", reservation.reservationId());
            } catch (Exception e) {
                log.error("Failed to send seat start notification for reservation {}: {}", reservation.reservationId(),
                        e.getMessage());
                systemLogService.logJobEvent(LogLevel.ERROR, "NotificationScheduler",
                        "Failed to send seat start notification: " + e.getMessage());
            }
        }

        List<ReminderPayload> endingReservations = loadExpiryWarnings(now, expiryWarningTime.plusMinutes(1));
        for (ReminderPayload reservation : endingReservations) {
            try {
                sendExpiryWarningNotification(reservation);
                sentExpiryWarnings.add(reservation.reservationId());
                log.info("Sent expiry warning for reservation: {}", reservation.reservationId());
            } catch (Exception e) {
                log.error("Failed to send expiry warning for reservation {}: {}", reservation.reservationId(),
                        e.getMessage());
                systemLogService.logJobEvent(LogLevel.ERROR, "NotificationScheduler",
                        "Failed to send expiry warning: " + e.getMessage());
            }
        }

        // Clean up old reminders (older than 1 hour) to prevent memory leak
        cleanupOldReminders();
    }

    private void sendReminderNotification(ReminderPayload reservation) {
        String title = "Nhắc nhở đặt chỗ";
        String body = String.format(
                "Bạn có lịch đặt ghế %s lúc %s. Hãy đến đúng giờ để check-in!",
                reservation.seatCode(), reservation.timeDisplay());

        pushNotificationService.sendToUser(
                reservation.userId(),
                title,
                body,
                NotificationType.REMINDER,
                reservation.reservationId(),
                PushNotificationService.DELIVERY_KEY_CHECKIN_REMINDER);
    }

    private void sendSeatStartNotification(ReminderPayload reservation) {
        String title = "Đã đến giờ sử dụng ghế";
        String body = String.format(
                "Lịch đặt ghế %s của bạn đã bắt đầu lúc %s. Hãy tới thư viện và xác nhận chỗ ngồi ngay để tránh bị hủy.",
                reservation.seatCode(), reservation.timeDisplay());

        pushNotificationService.sendToUser(
                reservation.userId(),
                title,
                body,
                NotificationType.BOOKING,
                reservation.reservationId(),
                RESERVATION_REFERENCE_TYPE,
                "BOOKING");
    }

    private void sendExpiryWarningNotification(ReminderPayload reservation) {
        String title = "Sắp hết giờ sử dụng";
        String body = String.format(
                "Phiên sử dụng ghế %s của bạn sẽ kết thúc lúc %s. Hãy chủ động sắp xếp và rời chỗ đúng giờ.",
                reservation.seatCode(), reservation.timeDisplay());

        pushNotificationService.sendToUser(
                reservation.userId(),
                title,
                body,
                NotificationType.REMINDER,
                reservation.reservationId(),
                PushNotificationService.DELIVERY_KEY_TIME_EXPIRY);
    }

    private List<ReminderPayload> loadUpcomingReminders(LocalDateTime now, LocalDateTime reminderWindowEnd) {
        return readOnlyTransactionTemplate.execute(status -> {
            List<ReminderPayload> payloads = new ArrayList<>();
            for (ReservationEntity reservation : reservationRepository
                    .findByStatusAndStartTimeBetween("BOOKED", now, reminderWindowEnd)) {
                if (sentReminders.contains(reservation.getReservationId()) || reservation.getUser() == null) {
                    continue;
                }
                String startTimeStr = reservation.getStartTime() != null
                        ? reservation.getStartTime().toLocalTime().toString().substring(0, 5)
                        : "N/A";
                payloads.add(new ReminderPayload(
                        reservation.getReservationId(),
                        reservation.getUser().getId(),
                        reservation.getSeat() != null ? reservation.getSeat().getSeatCode() : "N/A",
                        startTimeStr));
            }
            return payloads;
        });
    }

    private List<ReminderPayload> loadSeatStartNotifications(LocalDateTime windowStart, LocalDateTime windowEnd) {
        return readOnlyTransactionTemplate.execute(status -> {
            List<ReminderPayload> payloads = new ArrayList<>();
            for (ReservationEntity reservation : reservationRepository
                    .findByStatusAndStartTimeBetween("BOOKED", windowStart, windowEnd)) {
                if (sentSeatStartNotifications.contains(reservation.getReservationId()) || reservation.getUser() == null) {
                    continue;
                }
                String startTimeStr = reservation.getStartTime() != null
                        ? reservation.getStartTime().toLocalTime().toString().substring(0, 5)
                        : "N/A";
                payloads.add(new ReminderPayload(
                        reservation.getReservationId(),
                        reservation.getUser().getId(),
                        reservation.getSeat() != null ? reservation.getSeat().getSeatCode() : "N/A",
                        startTimeStr));
            }
            return payloads;
        });
    }

    private List<ReminderPayload> loadExpiryWarnings(LocalDateTime now, LocalDateTime warningWindowEnd) {
        return readOnlyTransactionTemplate.execute(status -> {
            List<ReminderPayload> payloads = new ArrayList<>();
            for (ReservationEntity reservation : reservationRepository
                    .findByStatusAndEndTimeBetween("CONFIRMED", now, warningWindowEnd)) {
                if (sentExpiryWarnings.contains(reservation.getReservationId()) || reservation.getUser() == null) {
                    continue;
                }
                String endTimeStr = reservation.getEndTime() != null
                        ? reservation.getEndTime().toLocalTime().toString().substring(0, 5)
                        : "N/A";
                payloads.add(new ReminderPayload(
                        reservation.getReservationId(),
                        reservation.getUser().getId(),
                        reservation.getSeat() != null ? reservation.getSeat().getSeatCode() : "N/A",
                        endTimeStr));
            }
            return payloads;
        });
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
        if (sentSeatStartNotifications.size() > 10000) {
            sentSeatStartNotifications.clear();
            log.info("Cleared seat start notification cache");
        }
    }

    public void clearTracking(UUID reservationId) {
        if (reservationId == null) {
            return;
        }
        sentReminders.remove(reservationId);
        sentExpiryWarnings.remove(reservationId);
        sentSeatStartNotifications.remove(reservationId);
    }

    public void markSeatStartNotificationSent(UUID reservationId) {
        if (reservationId == null) {
            return;
        }
        sentSeatStartNotifications.add(reservationId);
    }

    private record ReminderPayload(UUID reservationId, UUID userId, String seatCode, String timeDisplay) {
    }
}
