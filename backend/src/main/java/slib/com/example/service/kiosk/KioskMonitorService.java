package slib.com.example.service.kiosk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import slib.com.example.entity.hce.AccessLog;
import slib.com.example.entity.hce.HceDeviceEntity;
import slib.com.example.entity.kiosk.KioskConfigEntity;
import slib.com.example.repository.hce.AccessLogRepository;
import slib.com.example.repository.hce.HceDeviceRepository;
import slib.com.example.repository.kiosk.KioskConfigRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final HceDeviceRepository hceDeviceRepository;
    private final KioskConfigRepository kioskConfigRepository;
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
        message.put("deviceName", resolveDeviceDisplayName(accessLog.getDeviceId()));

        if (action.equals("CHECK_IN")) {
            message.put("time", accessLog.getCheckInTime());
        } else {
            message.put("time", accessLog.getCheckOutTime());
        }

        messagingTemplate.convertAndSend(ENTRY_TOPIC, message);
        log.info("Broadcast {} event for user {}", action, accessLog.getUser().getUserCode());
    }

    /**
     * Get recent entry/exit events in the current day.
     */
    public List<Map<String, Object>> getRecentLogs(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        int queryLimit = Math.min(safeLimit * 2, 100);
        List<AccessLog> logs = accessLogRepository.findRecentLogsFromStartOfDay(
                startOfDay,
                PageRequest.of(0, queryLimit));

        return logs.stream()
                .flatMap(log -> toTodayMonitorEvents(log, startOfDay).stream())
                .sorted(Comparator.comparing(item -> (LocalDateTime) item.get("time"), Comparator.reverseOrder()))
                .limit(safeLimit)
                .toList();
    }

    private List<Map<String, Object>> toTodayMonitorEvents(AccessLog log, LocalDateTime startOfDay) {
        List<Map<String, Object>> events = new ArrayList<>();

        if (log.getCheckInTime() != null && !log.getCheckInTime().isBefore(startOfDay)) {
            events.add(toMonitorEvent(log, "CHECK_IN", log.getCheckInTime()));
        }

        if (log.getCheckOutTime() != null && !log.getCheckOutTime().isBefore(startOfDay)) {
            events.add(toMonitorEvent(log, "CHECK_OUT", log.getCheckOutTime()));
        }

        return events;
    }

    private Map<String, Object> toMonitorEvent(AccessLog log, String action, LocalDateTime time) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", log.getLogId().toString() + "-" + action);
        item.put("userCode", log.getUser().getUserCode());
        item.put("fullName", log.getUser().getFullName());
        item.put("action", action);
        item.put("deviceId", log.getDeviceId());
        item.put("deviceName", resolveDeviceDisplayName(log.getDeviceId()));
        item.put("time", time);
        return item;
    }

    private String resolveDeviceDisplayName(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return "Cổng thư viện";
        }

        try {
            return hceDeviceRepository.findByDeviceId(deviceId)
                    .map(HceDeviceEntity::getDeviceName)
                    .filter(name -> name != null && !name.isBlank())
                    .orElseGet(() -> kioskConfigRepository.findByKioskCode(deviceId)
                            .map(KioskConfigEntity::getKioskName)
                            .filter(name -> name != null && !name.isBlank())
                            .orElse(deviceId));
        } catch (Exception e) {
            return deviceId;
        }
    }
}
