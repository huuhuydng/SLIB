package slib.com.example.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.entity.LibrarySetting;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.repository.ReservationRepository;
import slib.com.example.repository.SeatRepository;
import slib.com.example.repository.activity.ActivityLogRepository;
import slib.com.example.service.ReputationService;
import slib.com.example.service.SeatStatusSyncService;

@Service
public class ReservationScheduler {
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ReputationService reputationService;
    private final SeatStatusSyncService seatStatusSyncService;
    private final SimpMessagingTemplate messagingTemplate;
    private final LibrarySettingService librarySettingService;
    private final PushNotificationService pushNotificationService;

    public ReservationScheduler(ReservationRepository reservationRepository,
            SeatRepository seatRepository,
            ActivityLogRepository activityLogRepository,
            ReputationService reputationService,
            SeatStatusSyncService seatStatusSyncService,
            SimpMessagingTemplate messagingTemplate,
            LibrarySettingService librarySettingService,
            PushNotificationService pushNotificationService) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.activityLogRepository = activityLogRepository;
        this.reputationService = reputationService;
        this.seatStatusSyncService = seatStatusSyncService;
        this.messagingTemplate = messagingTemplate;
        this.librarySettingService = librarySettingService;
        this.pushNotificationService = pushNotificationService;
    }

    @Scheduled(fixedRate = 10000) // check mỗi 10s cho real-time
    @Transactional
    public void releaseExpiredSeats() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LibrarySetting settings = librarySettingService.getSettings();
            int autoCancelMinutes = settings.getAutoCancelMinutes() != null
                    ? settings.getAutoCancelMinutes()
                    : 15;

            // 1. Auto-cancel BOOKED chưa check-in sau autoCancelMinutes
            // Deadline = MAX(startTime, createdAt) + autoCancelMinutes
            // - Đặt trước (createdAt < startTime): đếm từ startTime
            // - Đặt trong slot (createdAt > startTime): đếm từ createdAt
            List<ReservationEntity> bookedList = reservationRepository.findByStatus("BOOKED");
            for (ReservationEntity r : bookedList) {
                LocalDateTime baseTime = r.getStartTime().isAfter(r.getCreatedAt())
                        ? r.getStartTime()
                        : r.getCreatedAt();
                LocalDateTime deadline = baseTime.plusMinutes(autoCancelMinutes);

                if (now.isAfter(deadline)) {
                    r.setStatus("EXPIRED");
                    reservationRepository.save(r);
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
                                String.format("Ghế %s tại %s (%s) đã bị hủy do không check-in sau %d phút.",
                                        seat.getSeatCode(), zoneName, timeStr, autoCancelMinutes),
                                NotificationType.BOOKING,
                                r.getReservationId());
                    } catch (Exception e) {
                        System.err.println("Failed to send auto-cancel notification: " + e.getMessage());
                    }
                }
            }

            // 2. Hoàn thành CONFIRMED đã hết hạn (endTime < now)
            // CONFIRMED = đã check-in NFC thành công, hết giờ → COMPLETED
            List<ReservationEntity> completedConfirmed = reservationRepository.findByEndTimeBeforeAndStatus(now,
                    "CONFIRMED");
            for (ReservationEntity r : completedConfirmed) {
                r.setStatus("COMPLETED");
                reservationRepository.save(r);
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                        r.getSeat(), "AVAILABLE", r.getStartTime(), r.getEndTime());
            }

            // 3. Hủy PROCESSING quá 2 phút (chưa xác nhận đặt chỗ)
            LocalDateTime processingCutoff = now.minusSeconds(120);
            List<ReservationEntity> processingExpired = reservationRepository.findByCreatedAtBeforeAndStatus(
                    processingCutoff, "PROCESSING");
            for (ReservationEntity r : processingExpired) {
                r.setStatus("CANCEL");
                reservationRepository.save(r);
                seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                        r.getSeat(), "AVAILABLE", r.getStartTime(), r.getEndTime());
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
                    System.err.println("Failed to broadcast dashboard update: " + wsErr.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error in releaseExpiredSeats: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check for reservations that haven't been checked in within 15 minutes and
     * apply penalty.
     * 
     * Case 1: If user books BEFORE the time slot starts, they must check in within
     * 15 minutes
     * AFTER the time slot starts.
     * Case 2: If user books DURING the time slot, they must check in within 15
     * minutes
     * AFTER the booking creation time.
     */
    @Scheduled(fixedRate = 60000) // Check every 60 seconds
    @Transactional
    public void checkLateCheckInsAndApplyPenalty() {
        LocalDateTime now = LocalDateTime.now();

        // Find all BOOKED reservations that have started
        List<ReservationEntity> activeReservations = reservationRepository.findBookedReservationsStarted(now);

        for (ReservationEntity reservation : activeReservations) {
            try {
                // Skip if already penalized
                if (activityLogRepository.hasLateCheckinPenalty(reservation.getReservationId())) {
                    continue;
                }

                // Skip if already checked in (NFC confirmed)
                if (activityLogRepository.hasNfcConfirmation(reservation.getReservationId())) {
                    continue;
                }

                // Calculate check-in deadline based on booking time
                LocalDateTime checkInDeadline;

                // Case 1: Booked BEFORE time slot started (createdAt < startTime)
                // Deadline = startTime + 15 minutes
                if (reservation.getCreatedAt().isBefore(reservation.getStartTime())) {
                    checkInDeadline = reservation.getStartTime().plusMinutes(15);
                }
                // Case 2: Booked DURING time slot (createdAt >= startTime)
                // Deadline = createdAt + 15 minutes
                else {
                    checkInDeadline = reservation.getCreatedAt().plusMinutes(15);
                }

                // Check if deadline has passed
                if (now.isAfter(checkInDeadline)) {
                    // Apply penalty
                    applyLateCheckInPenalty(reservation);
                }
            } catch (Exception e) {
                System.err.println("Error processing late check-in for reservation " +
                        reservation.getReservationId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Apply late check-in penalty to a user using NO_SHOW rule
     * This now uses ReputationService for dynamic rule application
     * AND cancels the reservation to release the seat for others
     */
    private void applyLateCheckInPenalty(ReservationEntity reservation) {
        SeatEntity seat = reservation.getSeat();
        ZoneEntity zone = seat.getZone();
        String seatCode = seat.getSeatCode();
        String zoneName = zone != null ? zone.getZoneName() : "";

        // Step 1: Apply NO_SHOW penalty (deduct reputation points)
        boolean penaltySuccess = reputationService.applyNoShowPenalty(
                reservation.getUser().getId(),
                seatCode,
                zoneName,
                reservation.getReservationId());

        if (penaltySuccess) {
            System.out.println(String.format(
                    "Successfully applied NO_SHOW penalty to user %s for reservation %s",
                    reservation.getUser().getId(),
                    reservation.getReservationId()));
        } else {
            System.err.println(String.format(
                    "Failed to apply NO_SHOW penalty to user %s for reservation %s",
                    reservation.getUser().getId(),
                    reservation.getReservationId()));
        }

        // Step 2: Cancel reservation and release seat for others
        try {
            reservation.setStatus("CANCELLED");
            reservationRepository.save(reservation);

            // Step 3: Broadcast seat status change via WebSocket
            // This allows other users to book the seat immediately
            seatStatusSyncService.broadcastSeatUpdateWithTimeSlot(
                    seat,
                    "AVAILABLE",
                    reservation.getStartTime(),
                    reservation.getEndTime());

            System.out.println(String.format(
                    "Cancelled reservation %s and released seat %s for rebooking",
                    reservation.getReservationId(),
                    seatCode));
        } catch (Exception e) {
            System.err.println(String.format(
                    "Failed to cancel reservation %s: %s",
                    reservation.getReservationId(),
                    e.getMessage()));
        }
    }
}
