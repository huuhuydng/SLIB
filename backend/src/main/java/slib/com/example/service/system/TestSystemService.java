package slib.com.example.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.notification.NotificationRepository;
import slib.com.example.service.notification.NotificationScheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestSystemService {

    private static final String REMINDER_REFERENCE_TYPE = "REMINDER";

    private final ReservationRepository reservationRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationScheduler notificationScheduler;

    public Map<String, Object> prepareCheckinReminder(UUID reservationId) {
        ReservationEntity reservation = getReservation(reservationId);
        validateReservation(reservation);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.plusMinutes(15).plusSeconds(30);
        long durationMinutes = resolveDurationMinutes(reservation, 120, 45, 240);

        reservation.setStatus("BOOKED");
        reservation.setStartTime(startTime);
        reservation.setEndTime(startTime.plusMinutes(durationMinutes));
        reservation.setConfirmedAt(null);
        reservation.setActualEndTime(null);
        reservation.setCancellationReason(null);
        reservation.setCancelledByUserId(null);
        reservationRepository.saveAndFlush(reservation);

        clearRecentReminderArtifacts(reservation);
        notificationScheduler.sendBookingReminders();

        log.info("[TestSystem] Prepared check-in reminder for reservation {}", reservationId);
        return buildResult(
                "Đã đưa lịch đặt về trạng thái còn khoảng 15 phút để bắt đầu và kích hoạt gửi nhắc lịch.",
                reservation,
                Map.of(
                        "scenario", "CHECKIN_REMINDER",
                        "triggeredAt", now.toString()));
    }

    public Map<String, Object> prepareExpiryWarning(UUID reservationId) {
        ReservationEntity reservation = getReservation(reservationId);
        validateReservation(reservation);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusMinutes(10).plusSeconds(30);
        long durationMinutes = resolveDurationMinutes(reservation, 120, 45, 240);
        LocalDateTime startTime = endTime.minusMinutes(durationMinutes);
        if (!startTime.isBefore(now)) {
            startTime = now.minusMinutes(60);
            endTime = startTime.plusMinutes(durationMinutes);
        }

        reservation.setStatus("CONFIRMED");
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setConfirmedAt(startTime);
        reservation.setActualEndTime(null);
        reservation.setCancellationReason(null);
        reservation.setCancelledByUserId(null);
        reservationRepository.saveAndFlush(reservation);

        clearRecentReminderArtifacts(reservation);
        notificationScheduler.sendBookingReminders();

        log.info("[TestSystem] Prepared expiry warning for reservation {}", reservationId);
        return buildResult(
                "Đã đưa lịch đặt về trạng thái còn khoảng 10 phút để kết thúc và kích hoạt cảnh báo sắp hết giờ.",
                reservation,
                Map.of(
                        "scenario", "TIME_EXPIRY_WARNING",
                        "triggeredAt", now.toString(),
                        "note", "Hệ thống hiện gửi cảnh báo trước khi hết giờ 10 phút."));
    }

    private ReservationEntity getReservation(UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch đặt: " + reservationId));
    }

    private void validateReservation(ReservationEntity reservation) {
        if (reservation.getUser() == null) {
            throw new RuntimeException("Lịch đặt không có người dùng liên kết.");
        }
        if (reservation.getSeat() == null) {
            throw new RuntimeException("Lịch đặt không có ghế liên kết.");
        }
    }

    private long resolveDurationMinutes(ReservationEntity reservation, long fallbackMinutes, long minMinutes,
            long maxMinutes) {
        if (reservation.getStartTime() == null || reservation.getEndTime() == null) {
            return fallbackMinutes;
        }

        long minutes = Duration.between(reservation.getStartTime(), reservation.getEndTime()).toMinutes();
        if (minutes <= 0) {
            return fallbackMinutes;
        }

        return Math.max(minMinutes, Math.min(minutes, maxMinutes));
    }

    private void clearRecentReminderArtifacts(ReservationEntity reservation) {
        UUID userId = reservation.getUser().getId();
        UUID reservationId = reservation.getReservationId();
        LocalDateTime since = LocalDateTime.now().minusMinutes(10);

        notificationRepository.deleteRecentByUserAndReference(
                userId,
                NotificationType.REMINDER,
                REMINDER_REFERENCE_TYPE,
                reservationId,
                since);
        notificationScheduler.clearTracking(reservationId);
    }

    private Map<String, Object> buildResult(String message, ReservationEntity reservation, Map<String, Object> extras) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("message", message);
        result.put("booking", buildBookingSummary(reservation));
        result.putAll(extras);
        return result;
    }

    private Map<String, Object> buildBookingSummary(ReservationEntity reservation) {
        String zoneName = reservation.getSeat().getZone() != null ? reservation.getSeat().getZone().getZoneName() : null;
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("reservationId", reservation.getReservationId());
        summary.put("status", reservation.getStatus());
        summary.put("startTime", reservation.getStartTime());
        summary.put("endTime", reservation.getEndTime());
        summary.put("userCode", reservation.getUser().getUserCode());
        summary.put("fullName", reservation.getUser().getFullName());
        summary.put("seatCode", reservation.getSeat().getSeatCode());
        summary.put("zoneName", zoneName != null ? zoneName : "");
        return summary;
    }
}
