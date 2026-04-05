package slib.com.example.service.kiosk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import slib.com.example.entity.hce.AccessLog;
import slib.com.example.repository.hce.AccessLogRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kiosk Monitor Service
 * Handles real-time monitoring of library entry/exit
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KioskMonitorService {

    private final AccessLogRepository accessLogRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String ENTRY_TOPIC = "/topic/library/entries";

    /**
     * Get today's entry statistics
     */
    public Map<String, Object> getTodayStats() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        long checkInCount = accessLogRepository.countByCheckInTimeAfter(startOfDay);
        long checkOutCount = accessLogRepository.countByCheckOutTimeAfter(startOfDay);
        long currentlyInside = accessLogRepository.countByCheckInTimeAfterAndCheckOutTimeIsNull(startOfDay);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", checkInCount);
        stats.put("totalExits", checkOutCount);
        stats.put("currentlyInside", currentlyInside);
        stats.put("lastUpdate", LocalDateTime.now());

        return stats;
    }

    /**
     * Broadcast entry/exit event to monitoring screens
     */
    public void broadcastEntryEvent(AccessLog accessLog, String action) {
        Map<String, Object> message = new HashMap<>();
        message.put("id", accessLog.getLogId().toString());
        message.put("userCode", accessLog.getUser().getUserCode());
        message.put("fullName", accessLog.getUser().getFullName());
        message.put("action", action);
        message.put("deviceId", accessLog.getDeviceId());

        if (action.equals("CHECK_IN")) {
            message.put("time", accessLog.getCheckInTime());
        } else {
            message.put("time", accessLog.getCheckOutTime());
        }

        messagingTemplate.convertAndSend(ENTRY_TOPIC, message);
        log.info("Broadcast {} event for user {}", action, accessLog.getUser().getUserCode());
    }

    /**
     * Get recent entry logs (last N records)
     */
    public List<Map<String, Object>> getRecentLogs(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<AccessLog> logs = accessLogRepository.findRecentLogs(PageRequest.of(0, safeLimit));

        return logs.stream()
                .map(log -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", log.getLogId().toString());
                    item.put("userCode", log.getUser().getUserCode());
                    item.put("fullName", log.getUser().getFullName());

                    boolean isCheckedOut = log.getCheckOutTime() != null;
                    item.put("action", isCheckedOut ? "CHECK_OUT" : "CHECK_IN");
                    item.put("deviceId", log.getDeviceId());
                    item.put("time", isCheckedOut ? log.getCheckOutTime() : log.getCheckInTime());

                    return item;
                })
                .toList();
    }
}
