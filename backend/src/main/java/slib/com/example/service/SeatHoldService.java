package slib.com.example.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.SeatStatus;
import slib.com.example.repository.SeatRepository;

@Service
public class SeatHoldService {

    private final SeatRepository seatRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public SeatHoldService(SeatRepository seatRepository, SimpMessagingTemplate messagingTemplate) {
        this.seatRepository = seatRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Hold a seat for 5 minutes
     */
    public Map<String, Object> holdSeat(Integer seatId, UUID userId) {
        SeatEntity seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        // Check if seat is available
        if (seat.getSeatStatus() != SeatStatus.AVAILABLE) {
            throw new RuntimeException("Ghế đã được đặt hoặc đang giữ bởi người khác");
        }

        // Hold the seat
        seat.setSeatStatus(SeatStatus.HOLDING);
        seat.setHeldByUser(userId);
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5));
        seatRepository.save(seat);

        // Broadcast via WebSocket
        broadcastSeatUpdate(seat, "HOLDING");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("seatId", seatId);
        result.put("status", "HOLDING");
        result.put("expiresAt", seat.getHoldExpiresAt().toString());
        return result;
    }

    /**
     * Release a held seat
     */
    public Map<String, Object> releaseSeat(Integer seatId, UUID userId) {
        SeatEntity seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        // Check if user is the one holding
        if (seat.getHeldByUser() == null || !seat.getHeldByUser().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy giữ ghế này");
        }

        // Release the seat
        seat.setSeatStatus(SeatStatus.AVAILABLE);
        seat.setHeldByUser(null);
        seat.setHoldExpiresAt(null);
        seatRepository.save(seat);

        // Broadcast via WebSocket
        broadcastSeatUpdate(seat, "AVAILABLE");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("seatId", seatId);
        result.put("status", "AVAILABLE");
        return result;
    }

    private void broadcastSeatUpdate(SeatEntity seat, String status) {
        Map<String, Object> message = new HashMap<>();
        message.put("seatId", seat.getSeatId());
        message.put("zoneId", seat.getZone().getZoneId());
        message.put("seatCode", seat.getSeatCode());
        message.put("status", status);
        message.put("action", "STATUS_CHANGED");

        messagingTemplate.convertAndSend("/topic/seats", message);
    }
}
