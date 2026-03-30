package slib.com.example.service.zone_config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.zone_config.SeatRepository;

/**
 * Service for holding seats during the booking process.
 * 
 * Note: With the new dynamic status calculation, seat holding is now managed
 * through reservations with PROCESSING status rather than directly on the seat
 * entity.
 * This service now delegates to the reservation-based approach.
 */
@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Hold a seat for a specific time slot.
     * Instead of setting seat_status directly, this now checks for existing
     * reservations.
     */
    public Map<String, Object> holdSeat(Integer seatId, UUID userId, LocalDateTime startTime, LocalDateTime endTime) {
        SeatEntity seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        // Check if seat is administratively disabled
        if (seat.getIsActive() == null || !seat.getIsActive()) {
            throw new RuntimeException("Ghế này đang bị tạm khóa");
        }

        // Check for overlapping reservations
        var overlapping = reservationRepository.findOverlappingReservations(seatId, startTime, endTime);
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Ghế đã được đặt hoặc đang giữ bởi người khác trong khung giờ này");
        }

        // Broadcast via WebSocket
        broadcastSeatUpdateWithTimeSlot(seat, "HOLDING", startTime, endTime);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("seatId", seatId);
        result.put("status", "HOLDING");
        result.put("message", "Ghế đang được giữ, vui lòng hoàn tất đặt chỗ");
        return result;
    }

    /**
     * Check if a seat is available for a specific time slot.
     */
    public boolean isSeatAvailable(Integer seatId, LocalDateTime startTime, LocalDateTime endTime) {
        SeatEntity seat = seatRepository.findById(seatId).orElse(null);
        if (seat == null || seat.getIsActive() == null || !seat.getIsActive()) {
            return false;
        }

        var overlapping = reservationRepository.findOverlappingReservations(seatId, startTime, endTime);
        return overlapping.isEmpty();
    }

    /**
     * Release a held seat (cancel PROCESSING reservation).
     */
    public Map<String, Object> releaseSeat(Integer seatId, UUID userId, LocalDateTime startTime,
            LocalDateTime endTime) {
        SeatEntity seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        // Broadcast via WebSocket
        broadcastSeatUpdateWithTimeSlot(seat, "AVAILABLE", startTime, endTime);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("seatId", seatId);
        result.put("status", "AVAILABLE");
        return result;
    }

    private void broadcastSeatUpdateWithTimeSlot(SeatEntity seat, String status,
            LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> message = new HashMap<>();
        message.put("seatId", seat.getSeatId());
        message.put("zoneId", seat.getZone().getZoneId());
        message.put("seatCode", seat.getSeatCode());
        message.put("status", status);
        message.put("action", "STATUS_CHANGED");

        if (startTime != null && endTime != null) {
            message.put("date", startTime.toLocalDate().toString());
            message.put("startTime", String.format("%02d:%02d", startTime.getHour(), startTime.getMinute()));
            message.put("endTime", String.format("%02d:%02d", endTime.getHour(), endTime.getMinute()));
        }

        messagingTemplate.convertAndSend("/topic/seats", message);
    }
}
