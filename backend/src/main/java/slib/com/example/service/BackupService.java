package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import slib.com.example.entity.system.BackupHistoryEntity;
import slib.com.example.entity.system.BackupHistoryEntity.BackupStatus;
import slib.com.example.entity.system.SystemLogEntity.LogLevel;
import slib.com.example.repository.BackupHistoryRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Backup Service
 * Handles manual PostgreSQL backup via pg_dump and backup history management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private final BackupHistoryRepository backupHistoryRepository;
    private final SystemLogService systemLogService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    private static final String BACKUP_DIR = "/tmp/slib-backups";

    /**
     * Trigger a manual database backup using pg_dump.
     */
    public BackupHistoryEntity performBackup() {
        // Ensure backup directory exists
        Path backupPath = Paths.get(BACKUP_DIR);
        try {
            Files.createDirectories(backupPath);
        } catch (IOException e) {
            log.error("[Backup] Cannot create backup directory: {}", e.getMessage());
            systemLogService.logError("BackupService", "Cannot create backup directory", e.getMessage());
            throw new RuntimeException("Cannot create backup directory: " + e.getMessage());
        }

        // Parse database connection info
        String dbHost = "localhost";
        String dbPort = "5434";
        String dbName = "slib";
        try {
            // jdbc:postgresql://localhost:5434/slib?...
            String url = datasourceUrl.replace("jdbc:postgresql://", "");
            String hostPortDb = url.split("\\?")[0]; // localhost:5434/slib
            String[] parts = hostPortDb.split("/");
            dbName = parts[1];
            String[] hostPort = parts[0].split(":");
            dbHost = hostPort[0];
            dbPort = hostPort.length > 1 ? hostPort[1] : "5432";
        } catch (Exception e) {
            log.warn("[Backup] Could not parse datasource URL, using defaults: {}", e.getMessage());
        }

        // Build filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "slib_backup_" + timestamp + ".sql";
        String filePath = BACKUP_DIR + "/" + fileName;

        // Create history entry
        BackupHistoryEntity history = BackupHistoryEntity.builder()
                .filePath(filePath)
                .status(BackupStatus.FAILED) // Default to failed, update on success
                .startedAt(LocalDateTime.now())
                .build();

        try {
            // Run pg_dump
            ProcessBuilder pb = new ProcessBuilder(
                    "pg_dump",
                    "-h", dbHost,
                    "-p", dbPort,
                    "-U", datasourceUsername,
                    "-d", dbName,
                    "-F", "c",        // custom format (compressed)
                    "-f", filePath
            );
            pb.environment().put("PGPASSWORD", datasourcePassword);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                history.setStatus(BackupStatus.FAILED);
                history.setErrorMessage("pg_dump exit code: " + exitCode + ". Output: " + output);
                history.setCompletedAt(LocalDateTime.now());
                backupHistoryRepository.save(history);
                systemLogService.logJobEvent(LogLevel.ERROR, "BackupService",
                        "Manual backup failed: exit code " + exitCode);
                throw new RuntimeException("Backup failed: " + output);
            }

            // Success
            File backupFile = new File(filePath);
            history.setStatus(BackupStatus.SUCCESS);
            history.setFileSizeBytes(backupFile.length());
            history.setCompletedAt(LocalDateTime.now());
            backupHistoryRepository.save(history);

            systemLogService.logJobEvent(LogLevel.INFO, "BackupService",
                    "Manual backup completed: " + fileName + " (" + formatFileSize(backupFile.length()) + ")");

            log.info("[Backup] Completed: {} ({})", fileName, formatFileSize(backupFile.length()));
            return history;

        } catch (IOException | InterruptedException e) {
            history.setErrorMessage(e.getMessage());
            history.setCompletedAt(LocalDateTime.now());
            backupHistoryRepository.save(history);
            systemLogService.logError("BackupService", "Backup process error", e.getMessage());
            throw new RuntimeException("Backup failed: " + e.getMessage());
        }
    }

    /**
     * Get backup history ordered by newest first.
     */
    public List<BackupHistoryEntity> getHistory() {
        return backupHistoryRepository.findAllByOrderByStartedAtDesc();
    }

    /**
     * Get backup file for download.
     */
    public File getBackupFile(UUID backupId) {
        BackupHistoryEntity history = backupHistoryRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("Backup not found"));

        File file = new File(history.getFilePath());
        if (!file.exists()) {
            throw new RuntimeException("Backup file not found on disk");
        }
        return file;
    }

    /**
     * Cleanup old backup files beyond retention period.
     */
    public void cleanupOldBackups(int retainDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retainDays);
        List<BackupHistoryEntity> oldBackups = backupHistoryRepository
                .findByStartedAtBeforeAndStatus(cutoff, BackupStatus.SUCCESS);

        for (BackupHistoryEntity backup : oldBackups) {
            try {
                File file = new File(backup.getFilePath());
                if (file.exists()) {
                    file.delete();
                }
                backupHistoryRepository.delete(backup);
            } catch (Exception e) {
                log.warn("[Backup] Failed to cleanup old backup {}: {}", backup.getId(), e.getMessage());
            }
        }

        if (!oldBackups.isEmpty()) {
            systemLogService.logJobEvent(LogLevel.INFO, "BackupService",
                    "Cleaned up " + oldBackups.size() + " old backups (retain " + retainDays + " days)");
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
