package slib.com.example.service.booking;

import lombok.extern.slf4j.Slf4j;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.library.LibrarySetting;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.activity.ActivityLogRepository;
import slib.com.example.service.reputation.ReputationService;
import slib.com.example.entity.activity.ActivityLogEntity;
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
    public static final int CONFIRMED_LEAVE_CONFIRMATION_GRACE_MINUTES = 5;
    private static final String RESERVATION_REFERENCE_TYPE = "RESERVATION";

    private final ReservationRepository reservationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ReputationService reputationService;
    private final SeatStatusSyncService seatStatusSyncService;
    private final SimpMessagingTemplate messagingTemplate;
    private final LibrarySettingService librarySettingService;
    private final PushNotificationService pushNotificationService;
    private final TransactionTemplate reservationTransactionTemplate;
    private final Set<UUID> sentSeatLeaveNotifications = ConcurrentHashMap.newKeySet();

    public ReservationScheduler(ReservationRepository reservationRepository,
            ActivityLogRepository activityLogRepository,
            ReputationService reputationService,
            SeatStatusSyncService seatStatusSyncService,
            SimpMessagingTemplate messagingTemplate,
            LibrarySettingService librarySettingService,
            PushNotificationService pushNotificationService,
            PlatformTransactionManager transactionManager) {
        this.reservationRepository = reservationRepository;
        this.activityLogRepository = activityLogRepository;
        this.reputationService = reputationService;
        this.seatStatusSyncService = seatStatusSyncService;
        this.messagingTemplate = messagingTemplate;
        this.librarySettingService = librarySettingService;
        this.pushNotificationService = pushNotificationService;
        this.reservationTransactionTemplate = new TransactionTemplate(transactionManager);
        this.reservationTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Main scheduler: xử lý tất cả trạng thái reservation.
     * Chạy mỗi 10 giây.
     */
    @Scheduled(fixedRate = 10000)
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
            List<UUID> bookedIds = reservationRepository.findByStatus("BOOKED").stream()
                    .map(ReservationEntity::getReservationId)
                    .collect(Collectors.toList());
            List<SeatUpdatePayload> seatUpdates = new ArrayList<>();
            int totalChanged = 0;
            for (UUID reservationId : bookedIds) {
                ReservationProcessingResult result = processExpiredBookedReservation(reservationId, now,
                        autoCancelMinutes);
                if (result.changed()) {
                    totalChanged++;
                    seatUpdates.add(result.seatUpdate());
                }
            }

            // =========================================================
            // 2. CONFIRMED tới endTime → gửi nhắc rời chỗ trong 5 phút
            // Sau 5 phút chưa xác nhận rời chỗ → phạt + COMPLETED
            // =========================================================
            List<UUID> confirmedEndingNowIds = reservationRepository
                    .findByStatusAndEndTimeBetween("CONFIRMED", now.minusSeconds(10), now.plusSeconds(10)).stream()
                    .map(ReservationEntity::getReservationId)
                    .collect(Collectors.toList());
            for (UUID reservationId : confirmedEndingNowIds) {
                sendSeatLeaveNotificationIfNeeded(reservationId);
            }

            List<UUID> confirmedIds = reservationRepository
                    .findByEndTimeBeforeAndStatus(now.minusMinutes(CONFIRMED_LEAVE_CONFIRMATION_GRACE_MINUTES),
                            "CONFIRMED")
                    .stream()
                    .map(ReservationEntity::getReservationId)
                    .collect(Collectors.toList());
            for (UUID reservationId : confirmedIds) {
                ReservationProcessingResult result = processCompletedConfirmedReservation(reservationId, now, false);
                if (result.changed()) {
                    totalChanged++;
                    seatUpdates.add(result.seatUpdate());
                }
            }

            // =========================================================
            // 3. PROCESSING quá 2 phút → CANCEL (chưa xác nhận đặt chỗ)
            // =========================================================
            LocalDateTime processingCutoff = now.minusSeconds(120);
            List<UUID> processingIds = reservationRepository
                    .findByCreatedAtBeforeAndStatus(processingCutoff, "PROCESSING").stream()
                    .map(ReservationEntity::getReservationId)
                    .collect(Collectors.toList());
            for (UUID reservationId : processingIds) {
                ReservationProcessingResult result = processExpiredProcessingReservation(reservationId);
                if (result.changed()) {
                    totalChanged++;
                    seatUpdates.add(result.seatUpdate());
                }
            }

            for (SeatUpdatePayload seatUpdate : seatUpdates) {
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                        seatUpdate.seatId(),
                        seatUpdate.zoneId(),
                        seatUpdate.seatCode(),
                        seatUpdate.status(),
                        seatUpdate.startTime(),
                        seatUpdate.endTime());
            }

            if (totalChanged > 0) {
                try {
                    messagingTemplate.convertAndSend("/topic/dashboard",
                            java.util.Map.of("type", "BOOKING_UPDATE", "action", "AUTO_STATUS_CHANGE",
                                    "count", totalChanged, "timestamp", java.time.Instant.now().toString()));
                } catch (Exception wsErr) {
                    log.warn("Failed to broadcast dashboard update", wsErr);
                }
            }

            cleanupTrackingIfNeeded();
        } catch (Exception e) {
            log.error("Error in releaseExpiredSeats", e);
        }
    }

    /**
     * Thưởng điểm "Tuần hoàn hảo" cho user không có vi phạm trong tuần.
     * Chạy mỗi Chủ nhật lúc 23:55.
     */
    @Scheduled(cron = "0 55 23 * * SUN")
    public void applyWeeklyPerfectBonus() {
        try {
            LocalDateTime weekStart = LocalDateTime.now()
                    .with(DayOfWeek.MONDAY)
                    .with(LocalTime.MIN);
            LocalDateTime weekEnd = LocalDateTime.now();

            List<ReservationEntity> completedThisWeek = reservationRepository
                    .findByStatusAndEndTimeBetween("COMPLETED", weekStart, weekEnd);

            // Nhóm theo userId
            java.util.Set<UUID> activeUserIds = completedThisWeek.stream()
                    .map(r -> r.getUser().getId())
                    .collect(Collectors.toSet());

            int bonusCount = 0;
            for (UUID userId : activeUserIds) {
                // Kiểm tra user có penalty nào trong tuần không
                boolean hasPenalty = activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .filter(log -> log.getCreatedAt() != null
                                && !log.getCreatedAt().toLocalDateTime().isBefore(weekStart))
                        .anyMatch(log -> ActivityLogEntity.TYPE_LATE_CHECKIN_PENALTY.equals(log.getActivityType())
                                || ActivityLogEntity.TYPE_LATE_CHECKOUT_PENALTY.equals(log.getActivityType())
                                || ActivityLogEntity.TYPE_NO_SHOW.equals(log.getActivityType())
                                || ActivityLogEntity.TYPE_VIOLATION.equals(log.getActivityType()));

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

    private ReservationProcessingResult processExpiredBookedReservation(UUID reservationId, LocalDateTime now,
            int autoCancelMinutes) {
        return reservationTransactionTemplate.execute(status -> reservationRepository.findById(reservationId)
                .map(reservation -> processExpiredBookedReservation(reservation, now, autoCancelMinutes, false))
                .orElse(ReservationProcessingResult.unchanged()));
    }

    public boolean forceExpireBookedReservation(UUID reservationId) {
        LocalDateTime now = LocalDateTime.now();
        int resolvedAutoCancelMinutes = 15;
        try {
            LibrarySetting settings = librarySettingService.getSettings();
            if (settings.getAutoCancelMinutes() != null) {
                resolvedAutoCancelMinutes = settings.getAutoCancelMinutes();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve auto-cancel minutes for forceExpireBookedReservation, fallback to 15", e);
        }
        final int autoCancelMinutes = resolvedAutoCancelMinutes;

        ReservationProcessingResult result = reservationTransactionTemplate.execute(status -> reservationRepository
                .findById(reservationId)
                .map(reservation -> processExpiredBookedReservation(reservation, now, autoCancelMinutes, true))
                .orElse(ReservationProcessingResult.unchanged()));

        if (result != null && result.changed() && result.seatUpdate() != null) {
            SeatUpdatePayload seatUpdate = result.seatUpdate();
            seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                    seatUpdate.seatId(),
                    seatUpdate.zoneId(),
                    seatUpdate.seatCode(),
                    seatUpdate.status(),
                    seatUpdate.startTime(),
                    seatUpdate.endTime());
            try {
                messagingTemplate.convertAndSend("/topic/dashboard",
                        java.util.Map.of(
                                "type", "BOOKING_UPDATE",
                                "action", "FORCED_AUTO_CANCEL",
                                "count", 1,
                                "timestamp", java.time.Instant.now().toString()));
            } catch (Exception wsErr) {
                log.warn("Failed to broadcast forced auto-cancel dashboard update", wsErr);
            }
            return true;
        }

        return false;
    }

    private ReservationProcessingResult processExpiredBookedReservation(ReservationEntity reservation,
            LocalDateTime now, int autoCancelMinutes) {
        return processExpiredBookedReservation(reservation, now, autoCancelMinutes, false);
    }

    private ReservationProcessingResult processExpiredBookedReservation(ReservationEntity reservation,
            LocalDateTime now, int autoCancelMinutes, boolean force) {
        if (!"BOOKED".equals(reservation.getStatus())) {
            return ReservationProcessingResult.unchanged();
        }

        LocalDateTime baseTime = reservation.getStartTime().isAfter(reservation.getCreatedAt())
                ? reservation.getStartTime()
                : reservation.getCreatedAt();
        LocalDateTime deadline = baseTime.plusMinutes(autoCancelMinutes);
        if (!force && !now.isAfter(deadline)) {
            return ReservationProcessingResult.unchanged();
        }

        try {
            SeatEntity seat = reservation.getSeat();
            String zoneName = seat.getZone() != null ? seat.getZone().getZoneName() : "";
            reputationService.applyNoShowPenalty(
                    reservation.getUser().getId(),
                    seat.getSeatCode(),
                    zoneName,
                    reservation.getReservationId());
        } catch (Exception penaltyErr) {
            log.warn("Failed to apply NO_SHOW penalty for reservation {}", reservation.getReservationId(),
                    penaltyErr);
        }

        reservation.setStatus("EXPIRED");
        reservationRepository.save(reservation);

        try {
            SeatEntity seat = reservation.getSeat();
            String zoneName = seat.getZone() != null ? seat.getZone().getZoneName() : "";
            String timeStr = String.format("%02d:%02d - %02d:%02d",
                    reservation.getStartTime().getHour(), reservation.getStartTime().getMinute(),
                    reservation.getEndTime().getHour(), reservation.getEndTime().getMinute());
            pushNotificationService.sendToUser(
                    reservation.getUser().getId(),
                    "Đặt chỗ đã bị hủy",
                    String.format(
                            "Ghế %s tại %s (%s) đã bị hủy do không quét NFC sau %d phút. Điểm uy tín bị trừ.",
                            seat.getSeatCode(), zoneName, timeStr, autoCancelMinutes),
                    NotificationType.BOOKING,
                    reservation.getReservationId());
        } catch (Exception e) {
            log.warn("Failed to send auto-cancel notification for reservation {}", reservation.getReservationId(), e);
        }

        return ReservationProcessingResult.changed(toSeatUpdatePayload(reservation, "AVAILABLE"));
    }

    public boolean forceSendSeatLeaveNotification(UUID reservationId) {
        return reservationTransactionTemplate.execute(status -> reservationRepository.findById(reservationId)
                .map(reservation -> sendSeatLeaveNotificationIfNeeded(reservation, true))
                .orElse(false));
    }

    private boolean sendSeatLeaveNotificationIfNeeded(UUID reservationId) {
        return reservationTransactionTemplate.execute(status -> reservationRepository.findById(reservationId)
                .map(reservation -> sendSeatLeaveNotificationIfNeeded(reservation, false))
                .orElse(false));
    }

    private boolean sendSeatLeaveNotificationIfNeeded(ReservationEntity reservation, boolean force) {
        if (!"CONFIRMED".equalsIgnoreCase(reservation.getStatus())) {
            return false;
        }
        if (!force && sentSeatLeaveNotifications.contains(reservation.getReservationId())) {
            return false;
        }

        try {
            SeatEntity seat = reservation.getSeat();
            String seatCode = seat != null ? seat.getSeatCode() : "";
            String zoneName = seat != null && seat.getZone() != null ? seat.getZone().getZoneName() : "";
            String body = String.format(
                    "Phiên sử dụng ghế %s tại %s đã đến giờ kết thúc. Vui lòng xác nhận rời chỗ trong %d phút để tránh bị trừ điểm uy tín.",
                    seatCode,
                    zoneName != null && !zoneName.isBlank() ? zoneName : "thư viện",
                    CONFIRMED_LEAVE_CONFIRMATION_GRACE_MINUTES);

            pushNotificationService.sendToUser(
                    reservation.getUser().getId(),
                    "Đã đến giờ rời chỗ",
                    body,
                    NotificationType.BOOKING,
                    reservation.getReservationId(),
                    RESERVATION_REFERENCE_TYPE,
                    "BOOKING");
            sentSeatLeaveNotifications.add(reservation.getReservationId());
            return true;
        } catch (Exception e) {
            log.warn("Failed to send seat leave notification for reservation {}", reservation.getReservationId(), e);
            return false;
        }
    }

    private ReservationProcessingResult processCompletedConfirmedReservation(UUID reservationId) {
        return processCompletedConfirmedReservation(reservationId, LocalDateTime.now(), false);
    }

    private ReservationProcessingResult processCompletedConfirmedReservation(UUID reservationId, LocalDateTime now,
            boolean force) {
        return reservationTransactionTemplate.execute(status -> reservationRepository.findById(reservationId)
                .map(reservation -> processCompletedConfirmedReservation(reservation, now, force))
                .orElse(ReservationProcessingResult.unchanged()));
    }

    public boolean forceCompleteConfirmedReservationAfterGrace(UUID reservationId) {
        LocalDateTime now = LocalDateTime.now();
        ReservationProcessingResult result = processCompletedConfirmedReservation(reservationId, now, true);

        if (result != null && result.changed() && result.seatUpdate() != null) {
            SeatUpdatePayload seatUpdate = result.seatUpdate();
            seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                    seatUpdate.seatId(),
                    seatUpdate.zoneId(),
                    seatUpdate.seatCode(),
                    seatUpdate.status(),
                    seatUpdate.startTime(),
                    seatUpdate.endTime());
            try {
                messagingTemplate.convertAndSend("/topic/dashboard",
                        java.util.Map.of(
                                "type", "BOOKING_UPDATE",
                                "action", "FORCED_LATE_CHECKOUT_AUTO_COMPLETE",
                                "count", 1,
                                "timestamp", java.time.Instant.now().toString()));
            } catch (Exception wsErr) {
                log.warn("Failed to broadcast forced late-checkout completion update", wsErr);
            }
            return true;
        }

        return false;
    }

    private ReservationProcessingResult processCompletedConfirmedReservation(ReservationEntity reservation,
            LocalDateTime now, boolean force) {
        if (!"CONFIRMED".equals(reservation.getStatus())) {
            return ReservationProcessingResult.unchanged();
        }
        if (!force && reservation.getEndTime() != null
                && now.isBefore(reservation.getEndTime().plusMinutes(CONFIRMED_LEAVE_CONFIRMATION_GRACE_MINUTES))) {
            return ReservationProcessingResult.unchanged();
        }

        SeatEntity seat = reservation.getSeat();
        String seatCode = seat != null ? seat.getSeatCode() : "";
        String zoneName = seat != null && seat.getZone() != null ? seat.getZone().getZoneName() : "";

        try {
            reputationService.applyLateCheckoutPenalty(
                    reservation.getUser().getId(),
                    seatCode,
                    zoneName,
                    reservation.getReservationId());
        } catch (Exception penaltyErr) {
            log.warn("Failed to apply LATE_CHECKOUT penalty for reservation {}", reservation.getReservationId(),
                    penaltyErr);
        }

        reservation.setStatus("COMPLETED");
        reservationRepository.save(reservation);

        try {
            pushNotificationService.sendToUser(
                    reservation.getUser().getId(),
                    "Phiên ngồi đã tự động kết thúc",
                    String.format(
                            "Bạn chưa xác nhận rời ghế %s trong vòng %d phút sau khi hết giờ. Hệ thống đã tự động kết thúc phiên sử dụng và áp dụng trừ điểm uy tín.",
                            seatCode,
                            CONFIRMED_LEAVE_CONFIRMATION_GRACE_MINUTES),
                    NotificationType.BOOKING,
                    reservation.getReservationId(),
                    RESERVATION_REFERENCE_TYPE,
                    "BOOKING");
        } catch (Exception e) {
            log.warn("Failed to send late checkout auto-complete notification for reservation {}",
                    reservation.getReservationId(), e);
        }

        sentSeatLeaveNotifications.remove(reservation.getReservationId());
        return ReservationProcessingResult.changed(toSeatUpdatePayload(reservation, "AVAILABLE"));
    }

    private ReservationProcessingResult processExpiredProcessingReservation(UUID reservationId) {
        return reservationTransactionTemplate.execute(status -> reservationRepository.findById(reservationId)
                .map(this::processExpiredProcessingReservation)
                .orElse(ReservationProcessingResult.unchanged()));
    }

    private ReservationProcessingResult processExpiredProcessingReservation(ReservationEntity reservation) {
        if (!"PROCESSING".equals(reservation.getStatus())) {
            return ReservationProcessingResult.unchanged();
        }

        reservation.setStatus("CANCEL");
        reservationRepository.save(reservation);
        return ReservationProcessingResult.changed(toSeatUpdatePayload(reservation, "AVAILABLE"));
    }

    private void cleanupTrackingIfNeeded() {
        if (sentSeatLeaveNotifications.size() > 10000) {
            sentSeatLeaveNotifications.clear();
            log.info("Cleared seat-leave notification tracking cache");
        }
    }

    private SeatUpdatePayload toSeatUpdatePayload(ReservationEntity reservation, String status) {
        SeatEntity seat = reservation.getSeat();
        Integer zoneId = seat.getZone() != null ? seat.getZone().getZoneId() : null;
        return new SeatUpdatePayload(
                seat.getSeatId(),
                zoneId,
                seat.getSeatCode(),
                status,
                reservation.getStartTime(),
                reservation.getEndTime());
    }

    private record SeatUpdatePayload(
            Integer seatId,
            Integer zoneId,
            String seatCode,
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime) {
    }

    private record ReservationProcessingResult(boolean changed, SeatUpdatePayload seatUpdate) {
        private static ReservationProcessingResult unchanged() {
            return new ReservationProcessingResult(false, null);
        }

        private static ReservationProcessingResult changed(SeatUpdatePayload seatUpdate) {
            return new ReservationProcessingResult(true, seatUpdate);
        }
    }
}
