package slib.com.example.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import slib.com.example.entity.system.SystemLogEntity;
import slib.com.example.entity.system.SystemLogEntity.*;
import slib.com.example.repository.system.SystemLogRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * System Log Service
 * Provides methods to log operational events and query logs.
 * All log writes are async to avoid impacting main request flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;

    /** Log retention: keep logs for 90 days by default */
    private static final int LOG_RETENTION_DAYS = 90;

    // =========================================
    // === WRITE METHODS (Async) ===
    // =========================================

    @Async
    public void logEvent(LogLevel level, LogCategory category, String service,
                         String message, String details, UUID userId,
                         LogSource source, UUID referenceId) {
        try {
            SystemLogEntity logEntity = SystemLogEntity.builder()
                    .level(level)
                    .category(category)
                    .service(service)
                    .message(message)
                    .action(resolveAction(category, service))
                    .source(source != null ? source : LogSource.SYSTEM)
                    .referenceId(referenceId)
                    .build();

            if (details != null && !details.isEmpty()) {
                Map<String, Object> detailsMap = new HashMap<>();
                detailsMap.put("info", details);
                logEntity.setDetails(detailsMap);
            }

            systemLogRepository.save(logEntity);
        } catch (Exception e) {
            log.error("[SystemLog] Failed to persist log: {}", e.getMessage());
        }
    }

    // --- Convenience methods (only log critical points) ---

    /** System error (exception, crash) */
    @Async
    public void logError(String service, String message, String details) {
        logEvent(LogLevel.ERROR, LogCategory.SYSTEM_ERROR, service, message, details,
                null, LogSource.SYSTEM, null);
    }

    /** Integration error (Firebase, Cloudinary, AI, Email) */
    @Async
    public void logIntegrationError(String service, String message, String details) {
        logEvent(LogLevel.ERROR, LogCategory.INTEGRATION, service, message, details,
                null, LogSource.SYSTEM, null);
    }

    /** Warning (performance, threshold breach) */
    @Async
    public void logWarning(String service, String message, String details) {
        logEvent(LogLevel.WARN, LogCategory.PERFORMANCE, service, message, details,
                null, LogSource.SYSTEM, null);
    }

    /** Background job event (start/finish/fail only) */
    @Async
    public void logJobEvent(LogLevel level, String service, String message) {
        logEvent(level, LogCategory.BACKGROUND_JOB, service, message, null,
                null, LogSource.SCHEDULER, null);
    }

    /** Admin audit event */
    @Async
    public void logAudit(String service, String message, UUID userId, String actorEmail) {
        try {
            SystemLogEntity logEntity = SystemLogEntity.builder()
                    .level(LogLevel.INFO)
                    .category(LogCategory.AUDIT)
                    .service(service)
                    .message(message)
                    .action(resolveAction(LogCategory.AUDIT, service))
                    .source(LogSource.WEB)
                    .actorEmail(actorEmail)
                    .build();
            systemLogRepository.save(logEntity);
        } catch (Exception e) {
            log.error("[SystemLog] Failed to persist audit log: {}", e.getMessage());
        }
    }

    // =========================================
    // === READ METHODS ===
    // =========================================

    public Page<SystemLogEntity> getLogs(String level, String category, String search,
                                         LocalDateTime startDate, LocalDateTime endDate,
                                         int page, int size) {
        LogLevel logLevel = parseEnum(LogLevel.class, level);
        LogCategory logCategory = parseEnum(LogCategory.class, category);
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        return systemLogRepository.findLogs(logLevel, logCategory, searchTerm,
                startDate, endDate, PageRequest.of(page, size));
    }

    public Map<String, Object> getStats(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> stats = new HashMap<>();

        if (startDate == null && endDate == null) {
            // Default: last 24h stats
            LocalDateTime last24h = LocalDateTime.now().minusHours(24);
            stats.put("totalErrors", systemLogRepository.countByLevel(LogLevel.ERROR));
            stats.put("totalWarnings", systemLogRepository.countByLevel(LogLevel.WARN));
            stats.put("totalInfo", systemLogRepository.countByLevel(LogLevel.INFO));
            stats.put("errorsLast24h", systemLogRepository.countByLevelSince(LogLevel.ERROR, last24h));
            stats.put("warningsLast24h", systemLogRepository.countByLevelSince(LogLevel.WARN, last24h));
            stats.put("totalLast24h", systemLogRepository.countSince(last24h));
        } else {
            // Custom date range
            stats.put("totalErrors", systemLogRepository.countByLevelBetween(LogLevel.ERROR, startDate, endDate));
            stats.put("totalWarnings", systemLogRepository.countByLevelBetween(LogLevel.WARN, startDate, endDate));
            stats.put("totalInfo", systemLogRepository.countByLevelBetween(LogLevel.INFO, startDate, endDate));
        }

        return stats;
    }

    // =========================================
    // === LOG RETENTION (cleanup old logs) ===
    // =========================================

    /** Run daily at 4:00 AM — delete logs older than LOG_RETENTION_DAYS */
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(LOG_RETENTION_DAYS);
        try {
            int deleted = systemLogRepository.deleteByCreatedAtBefore(cutoff);
            if (deleted > 0) {
                log.info("[SystemLog] Retention cleanup: deleted {} logs older than {} days", deleted, LOG_RETENTION_DAYS);
            }
        } catch (Exception e) {
            log.error("[SystemLog] Retention cleanup failed: {}", e.getMessage());
        }
    }

    // =========================================
    // === HELPERS ===
    // =========================================

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Enum.valueOf(enumType, value.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveAction(LogCategory category, String service) {
        String prefix = category != null ? category.name() : "SYSTEM";
        if (service == null || service.isBlank()) {
            return prefix;
        }
        return prefix + ":" + service;
    }
}
