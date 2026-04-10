package slib.com.example.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.notification.NotificationRepository;
import slib.com.example.service.booking.ReservationScheduler;
import slib.com.example.service.notification.NotificationScheduler;
import slib.com.example.service.notification.PushNotificationService;

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
    private static final String RESERVATION_REFERENCE_TYPE = "RESERVATION";

    private final ReservationRepository reservationRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationScheduler notificationScheduler;
    private final ReservationScheduler reservationScheduler;
    private final PushNotificationService pushNotificationService;

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

    public Map<String, Object> prepareNearReminder(UUID reservationId) {
        ReservationEntity reservation = getReservation(reservationId);
        validateReservation(reservation);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.plusMinutes(3).plusSeconds(30);
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

        log.info("[TestSystem] Prepared near reminder for reservation {}", reservationId);
        return buildResult(
                "Đã đưa lịch đặt về trạng thái còn khoảng 3 phút để bắt đầu và kích hoạt gửi nhắc lịch gần tới.",
                reservation,
                Map.of(
                        "scenario", "NEAR_CHECKIN_REMINDER",
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

    public Map<String, Object> prepareSeatLeavePrompt(UUID reservationId) {
        ReservationEntity reservation = getReservation(reservationId);
        validateReservation(reservation);

        LocalDateTime now = LocalDateTime.now();
        long durationMinutes = resolveDurationMinutes(reservation, 120, 45, 240);
        LocalDateTime endTime = now.plusSeconds(30);
        LocalDateTime startTime = endTime.minusMinutes(durationMinutes);
        if (!startTime.isBefore(now)) {
            startTime = now.minusMinutes(60);
            endTime = now.plusSeconds(30);
        }

        reservation.setStatus("CONFIRMED");
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setConfirmedAt(startTime);
        reservation.setActualEndTime(null);
        reservation.setCancellationReason(null);
        reservation.setCancelledByUserId(null);
        reservationRepository.saveAndFlush(reservation);

        clearRecentBookingArtifacts(reservation);
        reservationScheduler.forceSendSeatLeaveNotification(reservationId);

        log.info("[TestSystem] Prepared seat-leave prompt for reservation {}", reservationId);
        return buildResult(
                "Đã đưa lịch đặt tới đúng giờ kết thúc và gửi thông báo yêu cầu rời chỗ trong 5 phút.",
                reservation,
                Map.of(
                        "scenario", "SEAT_LEAVE_PROMPT",
                        "triggeredAt", now.toString(),
                        "notificationTitle", "Đã đến giờ rời chỗ"));
    }

    public Map<String, Object> prepareLateCheckoutAutoComplete(UUID reservationId) {
        ReservationEntity reservation = getReservation(reservationId);
        validateReservation(reservation);

        LocalDateTime now = LocalDateTime.now();
        long durationMinutes = resolveDurationMinutes(reservation, 120, 45, 240);
        LocalDateTime endTime = now.minusMinutes(ReservationScheduler.CONFIRMED_LEAVE_CONFIRMATION_GRACE_MINUTES + 1L);
        LocalDateTime startTime = endTime.minusMinutes(durationMinutes);

        reservation.setStatus("CONFIRMED");
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setConfirmedAt(startTime);
        reservation.setActualEndTime(null);
        reservation.setCancellationReason(null);
        reservation.setCancelledByUserId(null);
        reservationRepository.saveAndFlush(reservation);

        clearRecentBookingArtifacts(reservation);
        boolean changed = reservationScheduler.forceCompleteConfirmedReservationAfterGrace(reservationId);
        ReservationEntity updatedReservation = getReservation(reservationId);
        if (!changed || !"COMPLETED".equalsIgnoreCase(updatedReservation.getStatus())) {
            throw new RuntimeException("Không thể kích hoạt auto-complete và phạt quá giờ cho lịch đặt này.");
        }

        log.info("[TestSystem] Prepared late-checkout auto completion for reservation {}", reservationId);
        return buildResult(
                "Đã kích hoạt quá 5 phút chưa rời chỗ: hệ thống tự kết thúc phiên và trừ điểm uy tín.",
                updatedReservation,
                Map.of(
                        "scenario", "LATE_CHECKOUT_AUTO_COMPLETE",
                        "triggeredAt", now.toString(),
                        "note", "Flow này dùng đúng nhánh scheduler xử lý quá giờ sau khi đang ngồi."));
    }

    public Map<String, Object> prepareSeatStartNow(UUID reservationId) {
        ReservationEntity reservation = getReservation(reservationId);
        validateReservation(reservation);

        LocalDateTime now = LocalDateTime.now();
        long durationMinutes = resolveDurationMinutes(reservation, 120, 45, 240);
        LocalDateTime startTime = now.plusSeconds(30);

        reservation.setStatus("BOOKED");
        reservation.setStartTime(startTime);
        reservation.setEndTime(startTime.plusMinutes(durationMinutes));
        reservation.setConfirmedAt(null);
        reservation.setActualEndTime(null);
        reservation.setCancellationReason(null);
        reservation.setCancelledByUserId(null);
        reservationRepository.saveAndFlush(reservation);

        clearRecentBookingArtifacts(reservation);
        String title = "Đã đến giờ sử dụng ghế";
        String body = String.format(
                "Lịch đặt ghế %s của bạn đã bắt đầu. Hãy tới thư viện và xác nhận chỗ ngồi ngay để tránh bị hủy.",
                reservation.getSeat().getSeatCode());
        pushNotificationService.sendToUser(
                reservation.getUser().getId(),
                title,
                body,
                NotificationType.BOOKING,
                reservation.getReservationId(),
                RESERVATION_REFERENCE_TYPE,
                "BOOKING");
        notificationScheduler.markSeatStartNotificationSent(reservation.getReservationId());

        log.info("[TestSystem] Prepared seat-start notification for reservation {}", reservationId);
        return buildResult(
                "Đã đưa lịch đặt về trạng thái vừa đến giờ sử dụng và gửi thông báo vào app.",
                reservation,
                Map.of(
                        "scenario", "SEAT_START_NOW",
                        "triggeredAt", now.toString(),
                        "notificationTitle", title));
    }

    public Map<String, Object> prepareNoCheckinAutoCancel(UUID reservationId) {
        ReservationEntity reservation = getReservation(reservationId);
        validateReservation(reservation);

        LocalDateTime now = LocalDateTime.now();
        long durationMinutes = resolveDurationMinutes(reservation, 120, 45, 240);
        LocalDateTime startTime = now.minusHours(2);

        reservation.setStatus("BOOKED");
        reservation.setStartTime(startTime);
        reservation.setEndTime(startTime.plusMinutes(durationMinutes + 120));
        reservation.setConfirmedAt(null);
        reservation.setActualEndTime(null);
        reservation.setCancellationReason(null);
        reservation.setCancelledByUserId(null);
        reservationRepository.saveAndFlush(reservation);

        clearRecentBookingArtifacts(reservation);
        boolean changed = reservationScheduler.forceExpireBookedReservation(reservationId);
        ReservationEntity updatedReservation = getReservation(reservationId);
        if (!changed || !"EXPIRED".equalsIgnoreCase(updatedReservation.getStatus())) {
            throw new RuntimeException("Không thể kích hoạt auto-cancel cho lịch đặt này.");
        }

        log.info("[TestSystem] Prepared no-checkin auto cancel for reservation {}", reservationId);
        return buildResult(
                "Đã kích hoạt hủy lịch do chưa xác nhận chỗ ngồi và gửi thông báo hủy vào app.",
                updatedReservation,
                Map.of(
                        "scenario", "AUTO_CANCEL_NO_CHECKIN",
                        "triggeredAt", now.toString(),
                        "note", "Flow này dùng đúng luồng auto-cancel của hệ thống."));
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

    private void clearRecentBookingArtifacts(ReservationEntity reservation) {
        UUID userId = reservation.getUser().getId();
        UUID reservationId = reservation.getReservationId();
        LocalDateTime since = LocalDateTime.now().minusMinutes(10);

        clearRecentReminderArtifacts(reservation);
        notificationRepository.deleteRecentByUserAndReference(
                userId,
                NotificationType.BOOKING,
                RESERVATION_REFERENCE_TYPE,
                reservationId,
                since);
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
