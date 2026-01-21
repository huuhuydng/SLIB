package slib.com.example.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.SeatStatus;
import slib.com.example.repository.SeatRepository;

/**
 * Service cập nhật seat status ngay lập tức khi có thay đổi booking
 * và broadcast qua WebSocket để clients update real-time
 */
@Service
public class SeatStatusSyncService {

    private final SeatRepository seatRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public SeatStatusSyncService(SeatRepository seatRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.seatRepository = seatRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Cập nhật seat status ngay lập tức dựa trên reservations
     * Gọi method này sau khi tạo/update/cancel booking
     */
    public void updateSeatStatus(SeatEntity seat, LocalDateTime startTime, LocalDateTime endTime) {
        updateSeatStatus(seat, startTime, endTime, null);
    }

    /**
     * Cập nhật seat status với reservation status cụ thể
     * Dùng khi vừa tạo/update reservation và seat chưa refresh
     */
    public void updateSeatStatus(SeatEntity seat, LocalDateTime startTime, LocalDateTime endTime,
            String reservationStatus) {
        LocalDateTime now = LocalDateTime.now();

        // Chỉ update seat_status nếu reservation đang trong khung giờ hiện tại
        boolean isCurrentlyActive = !now.isBefore(startTime) && now.isBefore(endTime);

        if (isCurrentlyActive) {
            SeatStatus newStatus;

            if (reservationStatus != null) {
                // Dùng reservation status được truyền vào (khi seat chưa refresh)
                if ("BOOKED".equalsIgnoreCase(reservationStatus)) {
                    newStatus = SeatStatus.BOOKED;
                } else if ("PROCESSING".equalsIgnoreCase(reservationStatus)) {
                    newStatus = SeatStatus.HOLDING;
                } else if ("CANCEL".equalsIgnoreCase(reservationStatus)
                        || "EXPIRED".equalsIgnoreCase(reservationStatus)) {
                    newStatus = SeatStatus.AVAILABLE;
                } else {
                    newStatus = calculateCurrentStatus(seat, now);
                }
            } else {
                // Tính status mới dựa trên reservations từ DB
                newStatus = calculateCurrentStatus(seat, now);
            }

            if (seat.getSeatStatus() != newStatus) {
                seat.setSeatStatus(newStatus);
                seatRepository.save(seat);

                // Broadcast qua WebSocket
                broadcastSeatUpdate(seat, newStatus.name());
            }
        }
    }

    /**
     * Tính status của ghế dựa trên reservations active tại thời điểm now
     */
    private SeatStatus calculateCurrentStatus(SeatEntity seat, LocalDateTime now) {
        var activeReservation = seat.getReservation().stream()
                .filter(r -> {
                    String status = r.getStatus();
                    boolean isActiveStatus = "BOOKED".equalsIgnoreCase(status) ||
                            "PROCESSING".equalsIgnoreCase(status);
                    boolean isInTimeRange = !now.isBefore(r.getStartTime()) &&
                            now.isBefore(r.getEndTime());
                    return isActiveStatus && isInTimeRange;
                })
                .findFirst();

        if (activeReservation.isPresent()) {
            String reservStatus = activeReservation.get().getStatus();
            if ("BOOKED".equalsIgnoreCase(reservStatus)) {
                return SeatStatus.BOOKED;
            } else if ("PROCESSING".equalsIgnoreCase(reservStatus)) {
                return SeatStatus.HOLDING;
            }
        }

        return SeatStatus.AVAILABLE;
    }

    /**
     * Broadcast seat update qua WebSocket
     */
    private void broadcastSeatUpdate(SeatEntity seat, String status) {
        Map<String, Object> message = new HashMap<>();
        message.put("seatId", seat.getSeatId());
        message.put("zoneId", seat.getZone().getZoneId());
        message.put("seatCode", seat.getSeatCode());
        message.put("status", status);
        message.put("action", "STATUS_CHANGED");

        messagingTemplate.convertAndSend("/topic/seats", message);
    }

    /**
     * Force update status về AVAILABLE (khi cancel/hết giờ)
     */
    public void setAvailable(SeatEntity seat) {
        if (seat.getSeatStatus() != SeatStatus.AVAILABLE) {
            seat.setSeatStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
            broadcastSeatUpdate(seat, "AVAILABLE");
        }
    }

    /**
     * Force update status về BOOKED
     */
    public void setBooked(SeatEntity seat) {
        LocalDateTime now = LocalDateTime.now();

        // Check nếu có reservation BOOKED đang active
        var activeBooked = seat.getReservation().stream()
                .filter(r -> "BOOKED".equalsIgnoreCase(r.getStatus()) &&
                        !now.isBefore(r.getStartTime()) &&
                        now.isBefore(r.getEndTime()))
                .findFirst();

        if (activeBooked.isPresent() && seat.getSeatStatus() != SeatStatus.BOOKED) {
            seat.setSeatStatus(SeatStatus.BOOKED);
            seatRepository.save(seat);
            broadcastSeatUpdate(seat, "BOOKED");
        }
    }
}
