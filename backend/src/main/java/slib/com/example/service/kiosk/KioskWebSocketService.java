package slib.com.example.service.kiosk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Kiosk WebSocket Service
 * Handles real-time communication with kiosk devices
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KioskWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send session update to specific kiosk
     */
    public void sendSessionUpdate(String kioskCode, KioskQrDTO.KioskSessionUpdateMessage message) {
        String topic = "/topic/kiosk/" + kioskCode + "/session-updated";
        messagingTemplate.convertAndSend(topic, message);
        log.info("Sent session update to kiosk {}: {}", kioskCode, message.getAction());
    }

    /**
     * Send QR refresh event to kiosk
     */
    public void sendQrRefresh(String kioskCode, String newQrPayload, java.time.LocalDateTime expiresAt) {
        String topic = "/topic/kiosk/" + kioskCode + "/qr-refresh";

        Map<String, Object> message = new HashMap<>();
        message.put("qrPayload", newQrPayload);
        message.put("expiresAt", expiresAt);

        messagingTemplate.convertAndSend(topic, message);
        log.info("Sent QR refresh to kiosk {}", kioskCode);
    }

    /**
     * Send logout event to kiosk
     */
    public void sendLogout(String kioskCode, String reason) {
        String topic = "/topic/kiosk/" + kioskCode + "/logout";

        Map<String, Object> message = new HashMap<>();
        message.put("reason", reason);
        message.put("timestamp", java.time.LocalDateTime.now());

        messagingTemplate.convertAndSend(topic, message);
        log.info("Sent logout to kiosk {}: {}", kioskCode, reason);
    }

    /**
     * Broadcast to all monitoring screens
     */
    public void broadcastToMonitors(Map<String, Object> message) {
        messagingTemplate.convertAndSend("/topic/library/entries", message);
    }
}
