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
import slib.com.example.repository.ReservationRepository;
import slib.com.example.repository.SeatRepository;

@Service
public class ReservationScheduler {
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final SeatStatusSyncService seatStatusSyncService;
    private final SimpMessagingTemplate messagingTemplate;
    private final LibrarySettingService librarySettingService;
    private final PushNotificationService pushNotificationService;

    public ReservationScheduler(ReservationRepository reservationRepository,
            SeatRepository seatRepository,
            SeatStatusSyncService seatStatusSyncService,
            SimpMessagingTemplate messagingTemplate,
            LibrarySettingService librarySettingService,
            PushNotificationService pushNotificationService) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
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
}
