package slib.com.example.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import slib.com.example.entity.system.BackupHistoryEntity;
import slib.com.example.entity.system.BackupHistoryEntity.BackupStatus;
import slib.com.example.entity.system.SystemLogEntity.LogLevel;
import slib.com.example.repository.system.BackupHistoryRepository;

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

    @Value("${backup.pg-dump-path:pg_dump}")
    private String pgDumpPath;

    @Value("${backup.pg-restore-path:pg_restore}")
    private String pgRestorePath;

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
            log.error("[Backup] Không thể tạo thư mục sao lưu: {}", e.getMessage());
            systemLogService.logError("BackupService", "Không thể tạo thư mục sao lưu", e.getMessage());
            throw new RuntimeException("Không thể tạo thư mục sao lưu: " + e.getMessage());
        }

        DatabaseConnectionInfo connectionInfo = parseDatabaseConnectionInfo();

        // Build filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "slib_backup_" + timestamp + ".dump";
        String filePath = BACKUP_DIR + "/" + fileName;

        // Create history entry
        BackupHistoryEntity history = BackupHistoryEntity.builder()
                .filePath(filePath)
                .status(BackupStatus.FAILED) // Default to failed, update on success
                .startedAt(LocalDateTime.now())
                .build();

        try {
            ensurePgDumpAvailable();

            // Run pg_dump
            ProcessBuilder pb = new ProcessBuilder(
                    pgDumpPath,
                    "-h", connectionInfo.host(),
                    "-p", connectionInfo.port(),
                    "-U", datasourceUsername,
                    "-d", connectionInfo.database(),
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
                        "Sao lưu thủ công thất bại: mã thoát " + exitCode);
                throw new RuntimeException("Sao lưu thất bại: " + output);
            }

            // Success
            File backupFile = new File(filePath);
            history.setStatus(BackupStatus.SUCCESS);
            history.setFileSizeBytes(backupFile.length());
            history.setCompletedAt(LocalDateTime.now());
            backupHistoryRepository.save(history);

            systemLogService.logJobEvent(LogLevel.INFO, "BackupService",
                    "Sao lưu thủ công hoàn tất: " + fileName + " (" + formatFileSize(backupFile.length()) + ")");

            log.info("[Backup] Completed: {} ({})", fileName, formatFileSize(backupFile.length()));
            return history;

        } catch (IOException | InterruptedException e) {
            String normalizedMessage = normalizeBackupErrorMessage(e);
            history.setErrorMessage(e.getMessage());
            history.setCompletedAt(LocalDateTime.now());
            backupHistoryRepository.save(history);
            systemLogService.logError("BackupService", "Tiến trình sao lưu gặp lỗi", normalizedMessage);
            throw new RuntimeException(normalizedMessage);
        }
    }

    /**
     * Restore PostgreSQL database from an existing successful backup.
     */
    public void restoreBackup(UUID backupId) {
        BackupHistoryEntity history = backupHistoryRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản sao lưu"));

        if (history.getStatus() != BackupStatus.SUCCESS) {
            throw new RuntimeException("Chỉ có thể khôi phục từ bản sao lưu thành công");
        }

        File backupFile = new File(history.getFilePath());
        if (!backupFile.exists()) {
            throw new RuntimeException("Không tìm thấy file sao lưu trên máy chủ");
        }

        DatabaseConnectionInfo connectionInfo = parseDatabaseConnectionInfo();

        try {
            ensurePgRestoreAvailable();

            ProcessBuilder pb = new ProcessBuilder(
                    pgRestorePath,
                    "-h", connectionInfo.host(),
                    "-p", connectionInfo.port(),
                    "-U", datasourceUsername,
                    "-d", connectionInfo.database(),
                    "--clean",
                    "--if-exists",
                    "--no-owner",
                    "--no-privileges",
                    "--single-transaction",
                    "--exit-on-error",
                    backupFile.getAbsolutePath()
            );
            pb.environment().put("PGPASSWORD", datasourcePassword);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                systemLogService.logError("BackupService", "Khôi phục dữ liệu thất bại", output);
                throw new RuntimeException("Khôi phục thất bại: " + output);
            }

            systemLogService.logJobEvent(LogLevel.WARN, "BackupService",
                    "Khôi phục dữ liệu PostgreSQL thành công từ bản sao lưu: " + backupFile.getName());
            log.warn("[Backup] Restore completed from {}", backupFile.getName());

        } catch (IOException | InterruptedException e) {
            String normalizedMessage = normalizeRestoreErrorMessage(e);
            systemLogService.logError("BackupService", "Khôi phục dữ liệu thất bại", normalizedMessage);
            throw new RuntimeException(normalizedMessage);
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
                log.warn("[Backup] Không thể dọn bản sao lưu cũ {}: {}", backup.getId(), e.getMessage());
            }
        }

        if (!oldBackups.isEmpty()) {
            systemLogService.logJobEvent(LogLevel.INFO, "BackupService",
                    "Đã dọn " + oldBackups.size() + " bản sao lưu cũ (giữ lại " + retainDays + " ngày)");
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private void ensurePgDumpAvailable() throws IOException, InterruptedException {
        Process process = new ProcessBuilder(pgDumpPath, "--version")
                .redirectErrorStream(true)
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Không thể chạy công cụ pg_dump tại đường dẫn: " + pgDumpPath);
        }
    }

    private void ensurePgRestoreAvailable() throws IOException, InterruptedException {
        Process process = new ProcessBuilder(pgRestorePath, "--version")
                .redirectErrorStream(true)
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Không thể chạy công cụ pg_restore tại đường dẫn: " + pgRestorePath);
        }
    }

    private DatabaseConnectionInfo parseDatabaseConnectionInfo() {
        String dbHost = "localhost";
        String dbPort = "5434";
        String dbName = "slib";
        try {
            String url = datasourceUrl.replace("jdbc:postgresql://", "");
            String hostPortDb = url.split("\\?")[0];
            String[] parts = hostPortDb.split("/");
            dbName = parts[1];
            String[] hostPort = parts[0].split(":");
            dbHost = hostPort[0];
            dbPort = hostPort.length > 1 ? hostPort[1] : "5432";
        } catch (Exception e) {
            log.warn("[Backup] Không thể phân tích datasource URL, dùng cấu hình mặc định: {}", e.getMessage());
        }
        return new DatabaseConnectionInfo(dbHost, dbPort, dbName);
    }

    private String normalizeBackupErrorMessage(Exception e) {
        String rawMessage = e.getMessage() != null ? e.getMessage() : "Lỗi không xác định";
        if (rawMessage.contains("Cannot run program") || rawMessage.contains("No such file or directory")) {
            return "Không tìm thấy công cụ sao lưu PostgreSQL (pg_dump). Vui lòng cài postgresql-client hoặc cấu hình backup.pg-dump-path đúng trên máy chủ.";
        }
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return "Tiến trình sao lưu bị gián đoạn ngoài ý muốn.";
        }
        return "Sao lưu thất bại: " + rawMessage;
    }

    private String normalizeRestoreErrorMessage(Exception e) {
        String rawMessage = e.getMessage() != null ? e.getMessage() : "Lỗi không xác định";
        if (rawMessage.contains("Cannot run program") || rawMessage.contains("No such file or directory")) {
            return "Không tìm thấy công cụ khôi phục PostgreSQL (pg_restore). Vui lòng cài postgresql-client hoặc cấu hình backup.pg-restore-path đúng trên máy chủ.";
        }
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return "Tiến trình khôi phục bị gián đoạn ngoài ý muốn.";
        }
        return "Khôi phục dữ liệu thất bại: " + rawMessage;
    }

    private record DatabaseConnectionInfo(String host, String port, String database) {
    }
}
