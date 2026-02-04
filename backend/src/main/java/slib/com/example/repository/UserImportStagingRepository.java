package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.users.UserImportStaging;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserImportStagingRepository extends JpaRepository<UserImportStaging, Long> {

    List<UserImportStaging> findByBatchId(UUID batchId);

    List<UserImportStaging> findByBatchIdAndStatus(UUID batchId, UserImportStaging.StagingStatus status);

    @Query("SELECT COUNT(s) FROM UserImportStaging s WHERE s.batchId = :batchId AND s.status = :status")
    long countByBatchIdAndStatus(@Param("batchId") UUID batchId,
            @Param("status") UserImportStaging.StagingStatus status);

    @Modifying
    @Query("DELETE FROM UserImportStaging s WHERE s.batchId = :batchId")
    void deleteByBatchId(@Param("batchId") UUID batchId);

    /**
     * Mark rows as INVALID where email already exists in users table
     */
    @Modifying
    @Query(value = """
            UPDATE user_import_staging s
            SET status = 'INVALID',
                error_message = 'Email đã tồn tại: ' || s.email,
                processed_at = NOW()
            WHERE s.batch_id = :batchId
            AND s.status = 'PENDING'
            AND EXISTS (SELECT 1 FROM users u WHERE LOWER(u.email) = LOWER(s.email))
            """, nativeQuery = true)
    int markDuplicateEmails(@Param("batchId") UUID batchId);

    /**
     * Mark rows as INVALID where user_code already exists
     */
    @Modifying
    @Query(value = """
            UPDATE user_import_staging s
            SET status = 'INVALID',
                error_message = 'Mã sinh viên đã tồn tại: ' || s.user_code,
                processed_at = NOW()
            WHERE s.batch_id = :batchId
            AND s.status = 'PENDING'
            AND EXISTS (SELECT 1 FROM users u WHERE UPPER(u.user_code) = UPPER(s.user_code))
            """, nativeQuery = true)
    int markDuplicateUserCodes(@Param("batchId") UUID batchId);

    /**
     * Mark remaining PENDING rows as VALID
     */
    @Modifying
    @Query(value = """
            UPDATE user_import_staging s
            SET status = 'VALID', processed_at = NOW()
            WHERE s.batch_id = :batchId AND s.status = 'PENDING'
            """, nativeQuery = true)
    int markRemainingAsValid(@Param("batchId") UUID batchId);

    /**
     * Get rows needing avatar enrichment (VALID or IMPORTED with no avatar)
     */
    @Query(value = """
            SELECT s.* FROM user_import_staging s
            WHERE s.batch_id = :batchId
            AND s.status IN ('VALID', 'IMPORTED')
            AND (s.avt_url IS NULL OR s.avt_url = '')
            """, nativeQuery = true)
    List<UserImportStaging> findRowsNeedingAvatar(@Param("batchId") UUID batchId);
}
