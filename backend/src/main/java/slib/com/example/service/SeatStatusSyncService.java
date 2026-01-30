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
     * Cập nhật seat status dựa trên reservation status và thời gian thực
     * 
     * LOGIC CHÍNH:
     * - PROCESSING → seat = HOLDING (ngay lập tức, luôn luôn)
     * - BOOKED/CONFIRMED + đang trong khung giờ → seat = BOOKED
     * - BOOKED/CONFIRMED + chưa đến giờ → seat = AVAILABLE
     * - CANCEL/EXPIRED → seat = AVAILABLE
     */
    public void updateSeatStatus(SeatEntity seat, LocalDateTime startTime, LocalDateTime endTime,
            String reservationStatus) {
        LocalDateTime now = LocalDateTime.now();
        boolean isInTimeSlot = !now.isBefore(startTime) && now.isBefore(endTime);

        SeatStatus newStatus = calculateSeatStatus(reservationStatus, isInTimeSlot);

        if (newStatus != null && seat.getSeatStatus() != newStatus) {
            seat.setSeatStatus(newStatus);
            seatRepository.save(seat);
            broadcastSeatUpdate(seat, newStatus.name());
        }
    }

    /**
     * Tính seat status dựa trên reservation status và thời gian
     */
    private SeatStatus calculateSeatStatus(String reservationStatus, boolean isInTimeSlot) {
        if (reservationStatus == null) {
            return null;
        }

        String status = reservationStatus.toUpperCase();

        switch (status) {
            case "PROCESSING":
                // User đang chọn ghế → luôn HOLDING
                return SeatStatus.HOLDING;

            case "BOOKED":
            case "CONFIRMED":
                // Đã đặt → BOOKED nếu đang trong khung giờ, AVAILABLE nếu chưa đến
                return isInTimeSlot ? SeatStatus.BOOKED : SeatStatus.AVAILABLE;

            case "CANCEL":
            case "CANCELLED":
            case "EXPIRED":
                // Hủy hoặc hết hạn → AVAILABLE
                return SeatStatus.AVAILABLE;

            default:
                return null;
        }
    }

    /**
     * Tính status của ghế dựa trên tất cả reservations active tại thời điểm now
     * Dùng cho recalculate khi không biết reservation cụ thể
     */
    private SeatStatus calculateCurrentStatus(SeatEntity seat, LocalDateTime now) {
        // Tìm reservation đang active (PROCESSING, BOOKED, hoặc CONFIRMED)
        var activeReservation = seat.getReservation().stream()
                .filter(r -> {
                    String status = r.getStatus().toUpperCase();

                    // PROCESSING: luôn active (không cần kiểm tra time)
                    if ("PROCESSING".equals(status)) {
                        return true;
                    }

                    // BOOKED/CONFIRMED: chỉ active nếu đang trong khung giờ
                    if ("BOOKED".equals(status) || "CONFIRMED".equals(status)) {
                        return !now.isBefore(r.getStartTime()) && now.isBefore(r.getEndTime());
                    }

                    return false;
                })
                .findFirst();

        if (activeReservation.isPresent()) {
            String status = activeReservation.get().getStatus().toUpperCase();
            if ("PROCESSING".equals(status)) {
                return SeatStatus.HOLDING;
            } else {
                return SeatStatus.BOOKED;
            }
        }

        return SeatStatus.AVAILABLE;
    }

    /**
     * Broadcast seat update qua WebSocket
     */
    private void broadcastSeatUpdate(SeatEntity seat, String status) {
        broadcastSeatUpdateWithTimeSlot(seat, status, null, null);
    }

    /**
     * Broadcast seat update với thông tin time slot
     * Dùng cho booking future time slots để clients có thể filter
     */
    public void broadcastSeatUpdateWithTimeSlot(SeatEntity seat, String status,
            LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> message = new HashMap<>();
        message.put("seatId", seat.getSeatId());
        message.put("zoneId", seat.getZone().getZoneId());
        message.put("seatCode", seat.getSeatCode());
        message.put("status", status);
        message.put("action", "STATUS_CHANGED");

        // Include time slot info for filtering on clients
        if (startTime != null && endTime != null) {
            message.put("date", startTime.toLocalDate().toString());
            message.put("startTime", String.format("%02d:%02d", startTime.getHour(), startTime.getMinute()));
            message.put("endTime", String.format("%02d:%02d", endTime.getHour(), endTime.getMinute()));
        }

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

        // Check nếu có reservation BOOKED hoặc CONFIRMED đang active
        var activeBooked = seat.getReservation().stream()
                .filter(r -> ("BOOKED".equalsIgnoreCase(r.getStatus()) || "CONFIRMED".equalsIgnoreCase(r.getStatus()))
                        &&
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
