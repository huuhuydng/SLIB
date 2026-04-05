package slib.com.example.controller.system;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.system.SystemLogCleanupRequest;
import slib.com.example.entity.system.SystemLogEntity;
import slib.com.example.service.system.SystemLogService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * System Log Controller
 * Admin-only API to query operational logs.
 */
@RestController
@RequestMapping("/slib/system/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemLogController {

    private final SystemLogService systemLogService;

    /**
     * GET /slib/system/logs
     * Query logs with filters, search, date range, and pagination.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<SystemLogEntity> logs = systemLogService.getLogs(level, category, search,
                startDate, endDate, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("content", logs.getContent());
        response.put("totalElements", logs.getTotalElements());
        response.put("totalPages", logs.getTotalPages());
        response.put("page", logs.getNumber());
        response.put("size", logs.getSize());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /slib/system/logs/stats
     * Log count statistics. Optional startDate/endDate for custom range.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(systemLogService.getStats(startDate, endDate));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        String fileName = "nhat-ky-he-thong-"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(systemLogService.exportLogsToExcel(level, category, search, startDate, endDate));
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupLogs(@Valid @RequestBody SystemLogCleanupRequest request) {
        int deleted = systemLogService.cleanupLogsBefore(request.getBeforeDate());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("deletedCount", deleted);
        response.put("beforeDate", request.getBeforeDate());
        response.put("message", "Đã dọn " + deleted + " nhật ký trước ngày " + request.getBeforeDate());

        return ResponseEntity.ok(response);
    }
}
