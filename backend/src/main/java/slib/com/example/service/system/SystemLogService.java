package slib.com.example.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import slib.com.example.entity.system.SystemLogEntity;
import slib.com.example.entity.system.SystemLogEntity.*;
import slib.com.example.repository.system.SystemLogRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
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
        Specification<SystemLogEntity> spec = buildLogSpecification(level, category, search, startDate, endDate);
        return systemLogRepository.findAll(
                spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    public byte[] exportLogsToExcel(String level, String category, String search,
                                    LocalDateTime startDate, LocalDateTime endDate) {
        List<SystemLogEntity> logs = systemLogRepository.findAll(
                buildLogSpecification(level, category, search, startDate, endDate),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Nhật ký hệ thống");
            String[] headers = {
                    "Thời gian", "Mức độ", "Loại", "Dịch vụ", "Thông điệp",
                    "Chi tiết", "Hành động", "Email người thao tác", "Nguồn", "IP"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (SystemLogEntity logEntry : logs) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, formatTimestamp(logEntry.getCreatedAt()), bodyStyle);
                writeCell(row, 1, getLevelLabel(logEntry.getLevel()), bodyStyle);
                writeCell(row, 2, getCategoryLabel(logEntry.getCategory()), bodyStyle);
                writeCell(row, 3, logEntry.getService(), bodyStyle);
                writeCell(row, 4, logEntry.getMessage(), bodyStyle);
                writeCell(row, 5, extractDetails(logEntry), bodyStyle);
                writeCell(row, 6, logEntry.getAction(), bodyStyle);
                writeCell(row, 7, logEntry.getActorEmail(), bodyStyle);
                writeCell(row, 8, getSourceLabel(logEntry.getSource()), bodyStyle);
                writeCell(row, 9, logEntry.getIpAddress(), bodyStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(currentWidth + 1024, 20000));
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể xuất file Excel nhật ký", e);
        }
    }

    public int cleanupLogsBefore(LocalDate beforeDate) {
        if (beforeDate == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày mốc để dọn nhật ký");
        }
        return systemLogRepository.deleteByCreatedAtBefore(beforeDate.atStartOfDay());
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

    private Specification<SystemLogEntity> buildLogSpecification(String level, String category, String search,
                                                                 LocalDateTime startDate, LocalDateTime endDate) {
        LogLevel logLevel = parseEnum(LogLevel.class, level);
        LogCategory logCategory = parseEnum(LogCategory.class, category);
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Specification<SystemLogEntity> spec = Specification.where(null);

        if (logLevel != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("level"), logLevel));
        }
        if (logCategory != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), logCategory));
        }
        if (searchTerm != null) {
            String keyword = "%" + searchTerm.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("message")), keyword),
                    cb.like(cb.lower(cb.coalesce(root.get("service"), "")), keyword)
            ));
        }
        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        }
        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }

        return spec;
    }

    private String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "";
        }
        return timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    private String extractDetails(SystemLogEntity logEntry) {
        if (logEntry.getDetails() == null || logEntry.getDetails().isEmpty()) {
            return "";
        }
        Object info = logEntry.getDetails().get("info");
        return info != null ? String.valueOf(info) : logEntry.getDetails().toString();
    }

    private String getLevelLabel(LogLevel level) {
        if (level == null) {
            return "";
        }
        return switch (level) {
            case ERROR -> "Lỗi";
            case WARN -> "Cảnh báo";
            case INFO -> "Thông tin";
            case DEBUG -> "Gỡ lỗi";
        };
    }

    private String getCategoryLabel(LogCategory category) {
        if (category == null) {
            return "";
        }
        return switch (category) {
            case SYSTEM_ERROR -> "Lỗi hệ thống";
            case PERFORMANCE -> "Hiệu năng";
            case BACKGROUND_JOB -> "Tác vụ nền";
            case INTEGRATION -> "Tích hợp";
            case AUDIT -> "Quản trị";
        };
    }

    private String getSourceLabel(LogSource source) {
        if (source == null) {
            return "";
        }
        return switch (source) {
            case SYSTEM -> "Hệ thống";
            case WEB -> "Web";
            case MOBILE -> "Di động";
            case HCE -> "Cổng NFC";
            case SCHEDULER -> "Tác vụ nền";
        };
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void writeCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private String resolveAction(LogCategory category, String service) {
        String prefix = category != null ? category.name() : "SYSTEM";
        if (service == null || service.isBlank()) {
            return prefix;
        }
        return prefix + ":" + service;
    }
}
