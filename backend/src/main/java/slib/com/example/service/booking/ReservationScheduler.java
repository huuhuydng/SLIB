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
    private final ReservationRepository reservationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ReputationService reputationService;
    private final SeatStatusSyncService seatStatusSyncService;
    private final SimpMessagingTemplate messagingTemplate;
    private final LibrarySettingService librarySettingService;
    private final PushNotificationService pushNotificationService;
    private final TransactionTemplate reservationTransactionTemplate;

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
            // 2. CONFIRMED đã hết hạn (endTime < now) → COMPLETED
            // CONFIRMED = đã quét NFC thành công, hết giờ → hoàn thành
            // =========================================================
            List<UUID> confirmedIds = reservationRepository.findByEndTimeBeforeAndStatus(now, "CONFIRMED").stream()
                    .map(ReservationEntity::getReservationId)
                    .collect(Collectors.toList());
            for (UUID reservationId : confirmedIds) {
                ReservationProcessingResult result = processCompletedConfirmedReservation(reservationId);
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

    private ReservationProcessingResult processCompletedConfirmedReservation(UUID reservationId) {
        return reservationTransactionTemplate.execute(status -> reservationRepository.findById(reservationId)
                .map(this::processCompletedConfirmedReservation)
                .orElse(ReservationProcessingResult.unchanged()));
    }

    private ReservationProcessingResult processCompletedConfirmedReservation(ReservationEntity reservation) {
        if (!"CONFIRMED".equals(reservation.getStatus())) {
            return ReservationProcessingResult.unchanged();
        }

        reservation.setStatus("COMPLETED");
        reservationRepository.save(reservation);
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
