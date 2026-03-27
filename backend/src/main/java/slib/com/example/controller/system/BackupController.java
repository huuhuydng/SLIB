package slib.com.example.controller.system;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.entity.system.BackupHistoryEntity;
import slib.com.example.entity.system.BackupScheduleEntity;
import slib.com.example.repository.system.BackupScheduleRepository;
import slib.com.example.service.system.BackupService;

import java.io.File;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Backup Controller
 * Manual backup, backup history, backup schedule CRUD.
 */
@RestController
@RequestMapping("/slib/system/backup")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;
    private final BackupScheduleRepository backupScheduleRepository;

    // =========================================
    // === MANUAL BACKUP ===
    // =========================================

    /**
     * POST /slib/system/backup — Trigger manual backup
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> triggerBackup() {
        try {
            BackupHistoryEntity result = backupService.performBackup();
            Map<String, Object> response = new HashMap<>();
            response.put("id", result.getId());
            response.put("status", result.getStatus().name());
            response.put("filePath", result.getFilePath());
            response.put("fileSize", result.getFileSizeBytes());
            response.put("startedAt", result.getStartedAt());
            response.put("completedAt", result.getCompletedAt());
            response.put("message", "Sao lưu thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
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
            map.put("filePath", h.getFilePath());
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
    public ResponseEntity<Map<String, Object>> updateSchedule(@RequestBody Map<String, Object> request) {
        BackupScheduleEntity schedule = backupScheduleRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> BackupScheduleEntity.builder()
                        .scheduleName("Daily Backup")
                        .cronExpression("03:00")
                        .backupType(BackupScheduleEntity.BackupType.FULL)
                        .retainDays(30)
                        .isActive(false)
                        .build());

        if (request.containsKey("time")) {
            schedule.setCronExpression(request.get("time").toString());
        }
        if (request.containsKey("retainDays")) {
            schedule.setRetainDays(Integer.parseInt(request.get("retainDays").toString()));
        }
        if (request.containsKey("isActive")) {
            schedule.setIsActive(Boolean.parseBoolean(request.get("isActive").toString()));
        }

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
}
