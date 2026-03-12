package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import slib.com.example.entity.system.BackupScheduleEntity;
import slib.com.example.entity.system.SystemLogEntity.LogLevel;
import slib.com.example.repository.BackupScheduleRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Backup Scheduler Service
 * Runs periodically to check if any scheduled backup needs to execute.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackupSchedulerService {

    private final BackupScheduleRepository backupScheduleRepository;
    private final BackupService backupService;
    private final SystemLogService systemLogService;

    /**
     * Check every minute if a scheduled backup needs to run.
     * Compares current time with nextBackupAt of active schedules.
     */
    @Scheduled(fixedRate = 60000) // every 1 minute
    public void checkAndRunScheduledBackups() {
        List<BackupScheduleEntity> activeSchedules = backupScheduleRepository.findByIsActiveTrue();

        for (BackupScheduleEntity schedule : activeSchedules) {
            if (schedule.getNextBackupAt() != null &&
                LocalDateTime.now().isAfter(schedule.getNextBackupAt())) {
                try {
                    log.info("[BackupScheduler] Running scheduled backup: {}", schedule.getScheduleName());
                    systemLogService.logJobEvent(LogLevel.INFO, "BackupScheduler",
                            "Starting scheduled backup: " + schedule.getScheduleName());

                    backupService.performBackup();

                    // Update schedule times
                    schedule.setLastBackupAt(LocalDateTime.now());
                    schedule.setNextBackupAt(calculateNextBackupTime(schedule));
                    backupScheduleRepository.save(schedule);

                    // Cleanup old backups
                    backupService.cleanupOldBackups(schedule.getRetainDays());

                    systemLogService.logJobEvent(LogLevel.INFO, "BackupScheduler",
                            "Scheduled backup completed: " + schedule.getScheduleName());

                } catch (Exception e) {
                    log.error("[BackupScheduler] Scheduled backup failed: {}", e.getMessage());
                    systemLogService.logJobEvent(LogLevel.ERROR, "BackupScheduler",
                            "Scheduled backup failed: " + e.getMessage());

                    // Still update nextBackupAt to avoid infinite retries
                    schedule.setNextBackupAt(calculateNextBackupTime(schedule));
                    backupScheduleRepository.save(schedule);
                }
            }
        }
    }

    /**
     * Calculate next backup time from cron expression.
     * Simple parsing: expects "HH:mm" format from cronExpression for daily backups.
     */
    private LocalDateTime calculateNextBackupTime(BackupScheduleEntity schedule) {
        try {
            // Parse time from cron expression (stored as "HH:mm" for simplicity)
            String cronExpr = schedule.getCronExpression();
            LocalTime backupTime;

            if (cronExpr.contains(":")) {
                // Format: "03:00"
                backupTime = LocalTime.parse(cronExpr);
            } else {
                // Default to 3:00 AM
                backupTime = LocalTime.of(3, 0);
            }

            // Next backup is tomorrow at the specified time
            return LocalDateTime.now().plusDays(1).with(backupTime);
        } catch (Exception e) {
            // Default: next day at 3 AM
            return LocalDateTime.now().plusDays(1).withHour(3).withMinute(0).withSecond(0);
        }
    }
}
