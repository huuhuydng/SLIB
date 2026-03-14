package slib.com.example.service.users;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.users.ImportJob;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.entity.users.UserImportStaging;
import slib.com.example.repository.users.ImportJobRepository;
import slib.com.example.repository.users.UserImportStagingRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.entity.users.UserSetting;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import slib.com.example.service.auth.AuthService;

/**
 * Service for staging table operations.
 * Implements:
 * - Database-level validation (marking duplicates via SQL)
 * - Bulk insert from staging to users table
 * - Parallel BCrypt hashing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StagingImportService {

    private final UserImportStagingRepository stagingRepository;
    private final ImportJobRepository jobRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    /**
     * Step 1: Validate staging data using SQL (much faster than Java loops)
     */
    @Transactional
    public void validateStagingData(UUID batchId) {
        log.info("[Staging] Validating batch: {}", batchId);

        // Update job status
        jobRepository.updateStatus(batchId, ImportJob.ImportJobStatus.VALIDATING);

        // Mark duplicates using SQL (database-level processing)
        int duplicateEmails = stagingRepository.markDuplicateEmails(batchId);
        log.info("[Staging] Found {} duplicate emails", duplicateEmails);

        int duplicateCodes = stagingRepository.markDuplicateUserCodes(batchId);
        log.info("[Staging] Found {} duplicate user codes", duplicateCodes);

        // Mark remaining as valid
        int validCount = stagingRepository.markRemainingAsValid(batchId);
        log.info("[Staging] Marked {} rows as valid", validCount);

        // Update job counts
        long invalid = stagingRepository.countByBatchIdAndStatus(batchId, UserImportStaging.StagingStatus.INVALID);
        long valid = stagingRepository.countByBatchIdAndStatus(batchId, UserImportStaging.StagingStatus.VALID);
        jobRepository.updateValidationCounts(batchId, (int) valid, (int) invalid);

        log.info("[Staging] Validation complete: {} valid, {} invalid", valid, invalid);
    }

    /**
     * Step 2: Import valid users with PARALLEL BCrypt hashing
     */
    @Transactional
    public int importValidUsers(UUID batchId) {
        log.info("[Staging] Importing valid users for batch: {}", batchId);

        // Update job status
        jobRepository.updateStatus(batchId, ImportJob.ImportJobStatus.IMPORTING);

        // Get valid rows
        List<UserImportStaging> validRows = stagingRepository.findByBatchIdAndStatus(
                batchId, UserImportStaging.StagingStatus.VALID);

        if (validRows.isEmpty()) {
            log.warn("[Staging] No valid rows to import");
            return 0;
        }

        int totalRows = validRows.size();
        log.info("[Staging] Fast importing {} users with batch size 500...", totalRows);

        // Encode password ONCE (BCrypt is expensive - ~100ms per hash)
        String encodedPassword = authService.encodeDefaultPassword();

        // Convert ALL rows to User entities with settings (cascade will handle UserSetting)
        List<User> allUsers = validRows.stream()
                .map(row -> {
                    User user = User.builder()
                            .userCode(row.getUserCode().toUpperCase())
                            .username(row.getUserCode().toUpperCase())
                            .email(row.getEmail().toLowerCase())
                            .fullName(row.getFullName())
                            .phone(row.getPhone())
                            .dob(row.getDob())
                            .role(parseRole(row.getRole()))
                            .password(encodedPassword)
                            .passwordChanged(false)
                            .isActive(true)
                            .avtUrl(row.getAvtUrl())
                            .build();

                    // Set settings on User entity — CascadeType.ALL will persist it automatically
                    UserSetting settings = UserSetting.builder()
                            .user(user)
                            .isHceEnabled(true)
                            .isAiRecommendEnabled(true)
                            .isBookingRemindEnabled(true)
                            .themeMode("light")
                            .languageCode("vi")
                            .build();
                    user.setSettings(settings);

                    return user;
                })
                .collect(Collectors.toList());

        // Save in batches of 500 (matches Hibernate batch_size config)
        int BATCH_SIZE = 500;
        int processedCount = 0;

        for (int i = 0; i < allUsers.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, allUsers.size());
            List<User> batch = allUsers.subList(i, end);

            // Batch insert - Hibernate will cascade persist UserSettings automatically
            userRepository.saveAll(batch);

            processedCount += batch.size();

            // Update progress after each batch
            jobRepository.updateImportedCount(batchId, processedCount);

            log.info("[Staging] Progress: {}/{} ({}%)", processedCount, totalRows,
                    (processedCount * 100) / totalRows);
        }

        log.info("[Staging] Completed importing {} users", processedCount);
        return processedCount;
    }

    /**
     * Get import job status
     */
    public ImportJob getJobStatus(UUID batchId) {
        return jobRepository.findByBatchId(batchId).orElse(null);
    }

    /**
     * Get failed/invalid rows for a batch
     */
    public List<UserImportStaging> getFailedRows(UUID batchId) {
        return stagingRepository.findByBatchIdAndStatus(batchId, UserImportStaging.StagingStatus.INVALID);
    }

    /**
     * Clean up staging data after import
     */
    @Transactional
    public void cleanupStagingData(UUID batchId) {
        stagingRepository.deleteByBatchId(batchId);
        log.info("[Staging] Cleaned up staging data for batch: {}", batchId);
    }

    /**
     * Complete the job
     */
    @Transactional
    public void completeJob(UUID batchId) {
        ImportJob job = jobRepository.findByBatchId(batchId).orElse(null);
        if (job != null) {
            job.setStatus(ImportJob.ImportJobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
        }
    }

    /**
     * Mark job as failed
     */
    @Transactional
    public void failJob(UUID batchId, String errorMessage) {
        ImportJob job = jobRepository.findByBatchId(batchId).orElse(null);
        if (job != null) {
            job.setStatus(ImportJob.ImportJobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
        }
    }

    private Role parseRole(String roleStr) {
        if (roleStr == null || roleStr.isEmpty())
            return Role.STUDENT;
        try {
            return Role.valueOf(roleStr.toUpperCase());
        } catch (Exception e) {
            return Role.STUDENT;
        }
    }
}
