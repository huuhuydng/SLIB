package slib.com.example.service.booking;

import lombok.extern.slf4j.Slf4j;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.library.LibrarySetting;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.activity.ActivityLogRepository;
import slib.com.example.service.reputation.ReputationService;
import slib.com.example.service.zone_config.SeatStatusSyncService;
import slib.com.example.service.system.LibrarySettingService;
import slib.com.example.service.notification.PushNotificationService;

/**
 * Scheduler for handling reservation expirations, penalties, and rewards.
 * 
 * Flow: PROCESSING → BOOKED → CONFIRMED (NFC) → COMPLETED
 * - PROCESSING: User vừa tạo booking, chờ xác nhận (2 phút)
 * - BOOKED: User đã xác nhận booking (chờ quét NFC)
 * - CONFIRMED: User đã quét NFC tại ghế (check-in thực tế)
 * - COMPLETED: Hết giờ, tự động hoàn thành
 */
@Service
@Slf4j
public class ReservationScheduler {
    private final ReservationRepository reservationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ReputationService reputationService;
    private final SeatStatusSyncService seatStatusSyncService;
    private final SimpMessagingTemplate messagingTemplate;
    private final LibrarySettingService librarySettingService;
    private final PushNotificationService pushNotificationService;

    public ReservationScheduler(ReservationRepository reservationRepository,
            ActivityLogRepository activityLogRepository,
            ReputationService reputationService,
            SeatStatusSyncService seatStatusSyncService,
            SimpMessagingTemplate messagingTemplate,
            LibrarySettingService librarySettingService,
            PushNotificationService pushNotificationService) {
        this.reservationRepository = reservationRepository;
        this.activityLogRepository = activityLogRepository;
        this.reputationService = reputationService;
        this.seatStatusSyncService = seatStatusSyncService;
        this.messagingTemplate = messagingTemplate;
        this.librarySettingService = librarySettingService;
        this.pushNotificationService = pushNotificationService;
    }

    /**
     * Main scheduler: xử lý tất cả trạng thái reservation.
     * Chạy mỗi 10 giây.
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void releaseExpiredSeats() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LibrarySetting settings = librarySettingService.getSettings();
            int autoCancelMinutes = settings.getAutoCancelMinutes() != null
                    ? settings.getAutoCancelMinutes()
                    : 15;

            // =========================================================
            // 1. BOOKED chưa quét NFC (chưa CONFIRMED) sau autoCancelMinutes
            // → Phạt điểm uy tín + set EXPIRED + giải phóng ghế
            //
            // Deadline = MAX(startTime, createdAt) + autoCancelMinutes
            // - Đặt trước (createdAt < startTime): đếm từ startTime
            // - Đặt trong slot (createdAt > startTime): đếm từ createdAt
            // =========================================================
            List<ReservationEntity> bookedList = reservationRepository.findByStatus("BOOKED");
            List<ReservationEntity> expiredBookings = new ArrayList<>();
            for (ReservationEntity r : bookedList) {
                LocalDateTime baseTime = r.getStartTime().isAfter(r.getCreatedAt())
                        ? r.getStartTime()
                        : r.getCreatedAt();
                LocalDateTime deadline = baseTime.plusMinutes(autoCancelMinutes);

                if (now.isAfter(deadline)) {
                    // Áp dụng phạt NO_SHOW (chưa quét NFC trong thời gian quy định)
                    try {
                        SeatEntity seat = r.getSeat();
                        String zoneName = seat.getZone() != null ? seat.getZone().getZoneName() : "";
                        reputationService.applyNoShowPenalty(
                                r.getUser().getId(),
                                seat.getSeatCode(),
                                zoneName,
                                r.getReservationId());
                    } catch (Exception penaltyErr) {
                        log.warn("Failed to apply NO_SHOW penalty for reservation {}", r.getReservationId(),
                                penaltyErr);
                    }

                    // Set EXPIRED và giải phóng ghế
                    r.setStatus("EXPIRED");
                    expiredBookings.add(r);
                    seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                            r.getSeat(), "AVAILABLE", r.getStartTime(), r.getEndTime());

                    // Gửi notification thông báo đã tự hủy
                    try {
                        SeatEntity seat = r.getSeat();
                        String zoneName = seat.getZone() != null ? seat.getZone().getZoneName() : "";
                        String timeStr = String.format("%02d:%02d - %02d:%02d",
                                r.getStartTime().getHour(), r.getStartTime().getMinute(),
                                r.getEndTime().getHour(), r.getEndTime().getMinute());
                        pushNotificationService.sendToUser(
                                r.getUser().getId(),
                                "Đặt chỗ đã bị hủy",
                                String.format(
                                        "Ghế %s tại %s (%s) đã bị hủy do không quét NFC sau %d phút. Điểm uy tín bị trừ.",
                                        seat.getSeatCode(), zoneName, timeStr, autoCancelMinutes),
                                NotificationType.BOOKING,
                                r.getReservationId());
                    } catch (Exception e) {
                        log.warn("Failed to send auto-cancel notification for reservation {}", r.getReservationId(),
                                e);
                    }
                }
            }
            if (!expiredBookings.isEmpty()) {
                reservationRepository.saveAll(expiredBookings);
            }

            // =========================================================
            // 2. CONFIRMED đã hết hạn (endTime < now) → COMPLETED
            // CONFIRMED = đã quét NFC thành công, hết giờ → hoàn thành
            // =========================================================
            List<ReservationEntity> completedConfirmed = reservationRepository.findByEndTimeBeforeAndStatus(now,
                    "CONFIRMED");
            for (ReservationEntity r : completedConfirmed) {
                r.setStatus("COMPLETED");
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                        r.getSeat(), "AVAILABLE", r.getStartTime(), r.getEndTime());
            }
            if (!completedConfirmed.isEmpty()) {
                reservationRepository.saveAll(completedConfirmed);
            }

            // =========================================================
            // 3. PROCESSING quá 2 phút → CANCEL (chưa xác nhận đặt chỗ)
            // =========================================================
            LocalDateTime processingCutoff = now.minusSeconds(120);
            List<ReservationEntity> processingExpired = reservationRepository.findByCreatedAtBeforeAndStatus(
                    processingCutoff, "PROCESSING");
            for (ReservationEntity r : processingExpired) {
                r.setStatus("CANCEL");
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                        r.getSeat(), "AVAILABLE", r.getStartTime(), r.getEndTime());
            }
            if (!processingExpired.isEmpty()) {
                reservationRepository.saveAll(processingExpired);
            }

            // Broadcast dashboard update nếu có thay đổi
            int totalChanged = bookedList.stream()
                    .mapToInt(r -> "EXPIRED".equals(r.getStatus()) ? 1 : 0).sum()
                    + completedConfirmed.size() + processingExpired.size();
            if (totalChanged > 0) {
                try {
                    messagingTemplate.convertAndSend("/topic/dashboard",
                            java.util.Map.of("type", "BOOKING_UPDATE", "action", "AUTO_STATUS_CHANGE",
                                    "count", totalChanged, "timestamp", java.time.Instant.now().toString()));
                } catch (Exception wsErr) {
                    log.warn("Failed to broadcast dashboard update", wsErr);
                }
            }
        } catch (Exception e) {
            log.error("Error in releaseExpiredSeats", e);
        }
    }

    /**
     * Thưởng điểm "Tuần hoàn hảo" cho user không có vi phạm trong tuần.
     * Chạy mỗi Chủ nhật lúc 23:55.
     */
    @Scheduled(cron = "0 55 23 * * SUN")
    @Transactional
    public void applyWeeklyPerfectBonus() {
        try {
            LocalDateTime weekStart = LocalDateTime.now()
                    .with(DayOfWeek.MONDAY)
                    .with(LocalTime.MIN);
            LocalDateTime weekEnd = LocalDateTime.now();

            // Lấy tất cả user IDs có reservation COMPLETED trong tuần dựa trên thời điểm sử dụng thực tế
            List<ReservationEntity> completedThisWeek = reservationRepository
                    .findByStatus("COMPLETED").stream()
                    .filter(r -> r.getEndTime() != null
                            && !r.getEndTime().isBefore(weekStart)
                            && !r.getEndTime().isAfter(weekEnd))
                    .collect(Collectors.toList());

            // Nhóm theo userId
            java.util.Set<UUID> activeUserIds = completedThisWeek.stream()
                    .map(r -> r.getUser().getId())
                    .collect(Collectors.toSet());

            int bonusCount = 0;
            for (UUID userId : activeUserIds) {
                // Kiểm tra user có penalty nào trong tuần không
                boolean hasPenalty = activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .filter(log -> log.getCreatedAt() != null
                                && log.getCreatedAt().toLocalDateTime().isAfter(weekStart))
                        .anyMatch(log -> "LATE_CHECKIN_PENALTY".equals(log.getActivityType())
                                || "NO_SHOW".equals(log.getActivityType())
                                || "VIOLATION".equals(log.getActivityType()));

                if (!hasPenalty) {
                    try {
                        reputationService.applyWeeklyPerfectBonus(userId);
                        bonusCount++;
                    } catch (Exception e) {
                        log.warn("Failed to apply weekly bonus for user {}", userId, e);
                    }
                }
            }

            if (bonusCount > 0) {
                log.info("Applied WEEKLY_PERFECT bonus to {} users", bonusCount);
            }
        } catch (Exception e) {
            log.error("Error in applyWeeklyPerfectBonus", e);
        }
    }
}
