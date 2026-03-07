package slib.com.example.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import slib.com.example.entity.zone_config.SeatEntity;

/**
 * Service for broadcasting seat status updates via WebSocket.
 * 
 * With the new dynamic status calculation approach, this service no longer
 * updates seat_status in the database. It only broadcasts changes to connected
 * clients so they can refresh their view.
 */
@Service
public class SeatStatusSyncService {

    private final SimpMessagingTemplate messagingTemplate;

    public SeatStatusSyncService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcast seat update via WebSocket.
     * This is called after reservation changes to notify clients to refresh.
     */
    public void broadcastSeatUpdate(SeatEntity seat, String status) {
        broadcastSeatUpdateWithTimeSlot(seat, status, null, null);
    }

    /**
     * Broadcast seat update with time slot information.
     * Clients can filter updates based on the time slot they are viewing.
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
     * @deprecated No longer updates database. Use broadcastSeatUpdate instead.
     *             This method is kept for backwards compatibility but only
     *             broadcasts.
     */
    @Deprecated
    public void updateSeatStatus(SeatEntity seat, LocalDateTime startTime, LocalDateTime endTime,
            String reservationStatus) {
        // Only broadcast - no database update
        String wsStatus = mapReservationStatusToWebSocket(reservationStatus);
        broadcastSeatUpdateWithTimeSlot(seat, wsStatus, startTime, endTime);
    }

    /**
     * Map reservation status to WebSocket broadcast status.
     */
    private String mapReservationStatusToWebSocket(String reservationStatus) {
        if (reservationStatus == null) {
            return "AVAILABLE";
        }

        String status = reservationStatus.toUpperCase();
        switch (status) {
            case "PROCESSING":
                return "HOLDING";
            case "BOOKED":
            case "CONFIRMED":
                return "BOOKED";
            case "CANCEL":
            case "CANCELLED":
            case "EXPIRED":
            case "COMPLETED":
            default:
                return "AVAILABLE";
        }
    }
}
