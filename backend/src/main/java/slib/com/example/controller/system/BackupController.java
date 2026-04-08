package slib.com.example.controller.system;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import slib.com.example.entity.system.BackupHistoryEntity;
import slib.com.example.entity.system.BackupScheduleEntity;
import slib.com.example.dto.system.BackupScheduleUpdateRequest;
import slib.com.example.repository.system.BackupScheduleRepository;
import slib.com.example.service.system.BackupService;
import slib.com.example.service.system.SystemLogService;

import java.io.File;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Backup Controller
 * Manual backup, backup history, backup schedule CRUD.
 */
@RestController
@RequestMapping("/slib/system/backup")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class BackupController {

    private final BackupService backupService;
    private final BackupScheduleRepository backupScheduleRepository;
    private final SystemLogService systemLogService;

    // =========================================
    // === MANUAL BACKUP ===
    // =========================================

    /**
     * POST /slib/system/backup — Trigger manual backup
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> triggerBackup(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            BackupHistoryEntity result = backupService.performBackup();
            systemLogService.logAudit(
                    "BackupController",
                    "Kích hoạt sao lưu thủ công: " + result.getId(),
                    null,
                    userDetails != null ? userDetails.getUsername() : null);
            Map<String, Object> response = new HashMap<>();
            response.put("id", result.getId());
            response.put("status", result.getStatus().name());
            response.put("fileSize", result.getFileSizeBytes());
            response.put("startedAt", result.getStartedAt());
            response.put("completedAt", result.getCompletedAt());
            response.put("message", "Sao lưu thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            systemLogService.logError("BackupController", "Sao lưu thủ công thất bại", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "FAILED",
                    "message", "Sao lưu thất bại: " + e.getMessage()
            ));
        }
    }

    // =========================================
    // === BACKUP HISTORY ===
    // =========================================

    /**
     * GET /slib/system/backup/history
     */
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory() {
        List<BackupHistoryEntity> history = backupService.getHistory();
        List<Map<String, Object>> result = history.stream().map(h -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", h.getId());
            map.put("status", h.getStatus().name());
            map.put("fileName", new File(h.getFilePath()).getName());
            map.put("fileSize", h.getFileSizeBytes());
            map.put("fileSizeFormatted", formatFileSize(h.getFileSizeBytes()));
            map.put("startedAt", h.getStartedAt());
            map.put("completedAt", h.getCompletedAt());
            map.put("errorMessage", h.getErrorMessage());
            // Calculate duration
            if (h.getStartedAt() != null && h.getCompletedAt() != null) {
                long seconds = java.time.Duration.between(h.getStartedAt(), h.getCompletedAt()).getSeconds();
                map.put("duration", seconds + " giây");
            }
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /slib/system/backup/download/{id}
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadBackup(@PathVariable UUID id) {
        try {
            File file = backupService.getBackupFile(id);
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getName() + "\"")
                    .contentLength(file.length())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /slib/system/backup/restore/{id}
     * Restore PostgreSQL database from an existing backup file.
     */
    @PostMapping("/restore/{id}")
    public ResponseEntity<Map<String, Object>> restoreBackup(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            backupService.restoreBackup(id);
            systemLogService.logAudit(
                    "BackupController",
                    "Khôi phục dữ liệu PostgreSQL từ backup: " + id,
                    null,
                    userDetails != null ? userDetails.getUsername() : null);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Khôi phục dữ liệu PostgreSQL thành công. Bạn nên kiểm tra lại hệ thống ngay sau khi phục hồi."
            ));
        } catch (Exception e) {
            systemLogService.logError("BackupController", "Khôi phục dữ liệu thất bại", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage() != null ? e.getMessage() : "Khôi phục dữ liệu thất bại"
            ));
        }
    }

    // =========================================
    // === BACKUP SCHEDULE ===
    // =========================================

    /**
     * GET /slib/system/backup/schedule
     */
    @GetMapping("/schedule")
    public ResponseEntity<Map<String, Object>> getSchedule() {
        Optional<BackupScheduleEntity> scheduleOpt = backupScheduleRepository.findFirstByOrderByIdAsc();

        if (scheduleOpt.isEmpty()) {
            // Create default schedule if none exists
            BackupScheduleEntity defaultSchedule = BackupScheduleEntity.builder()
                    .scheduleName("Daily Backup")
                    .cronExpression("03:00")
                    .backupType(BackupScheduleEntity.BackupType.FULL)
                    .retainDays(30)
                    .isActive(false)
                    .nextBackupAt(LocalDateTime.now().plusDays(1).withHour(3).withMinute(0).withSecond(0))
                    .build();
            backupScheduleRepository.save(defaultSchedule);
            return ResponseEntity.ok(scheduleToMap(defaultSchedule));
        }

        return ResponseEntity.ok(scheduleToMap(scheduleOpt.get()));
    }

    /**
     * PUT /slib/system/backup/schedule
     * Update backup schedule config.
     * Body: { "time": "03:00", "retainDays": 30, "isActive": true }
     */
    @PutMapping("/schedule")
    public ResponseEntity<Map<String, Object>> updateSchedule(
            @Valid @RequestBody BackupScheduleUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        BackupScheduleEntity schedule = backupScheduleRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> BackupScheduleEntity.builder()
                        .scheduleName("Daily Backup")
                        .cronExpression("03:00")
                        .backupType(BackupScheduleEntity.BackupType.FULL)
                        .retainDays(30)
                        .isActive(false)
                        .build());

        try {
            LocalTime backupTime = parseBackupTime(request.getTime());
            schedule.setCronExpression(backupTime.toString());
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Thời gian sao lưu không hợp lệ. Định dạng đúng là HH:mm"
            ));
        }
        schedule.setRetainDays(request.getRetainDays());
        schedule.setIsActive(request.getIsActive());

        // Calculate next backup time
        try {
            LocalTime backupTime = LocalTime.parse(schedule.getCronExpression());
            LocalDateTime nextBackup = LocalDateTime.now().with(backupTime);
            if (nextBackup.isBefore(LocalDateTime.now())) {
                nextBackup = nextBackup.plusDays(1);
            }
            schedule.setNextBackupAt(nextBackup);
        } catch (Exception ignored) {
            schedule.setNextBackupAt(LocalDateTime.now().plusDays(1).withHour(3).withMinute(0));
        }

        backupScheduleRepository.save(schedule);
        systemLogService.logAudit(
                "BackupController",
                "Cập nhật lịch sao lưu: time=%s, retainDays=%s, active=%s".formatted(
                        schedule.getCronExpression(),
                        schedule.getRetainDays(),
                        schedule.getIsActive()),
                null,
                userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.ok(scheduleToMap(schedule));
    }

    // =========================================
    // === HELPERS ===
    // =========================================

    private Map<String, Object> scheduleToMap(BackupScheduleEntity s) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("scheduleName", s.getScheduleName());
        map.put("time", s.getCronExpression());
        map.put("backupType", s.getBackupType().name());
        map.put("retainDays", s.getRetainDays());
        map.put("isActive", s.getIsActive());
        map.put("lastBackupAt", s.getLastBackupAt());
        map.put("nextBackupAt", s.getNextBackupAt());
        return map;
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private LocalTime parseBackupTime(String time) {
        return LocalTime.parse(time);
    }
}
