package slib.com.example.repository.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.users.ImportJob;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {

    Optional<ImportJob> findByBatchId(UUID batchId);

    @Modifying
    @Query("UPDATE ImportJob j SET j.status = :status, j.updatedAt = CURRENT_TIMESTAMP WHERE j.batchId = :batchId")
    void updateStatus(@Param("batchId") UUID batchId, @Param("status") ImportJob.ImportJobStatus status);

    @Modifying
    @Query("UPDATE ImportJob j SET j.validCount = :valid, j.invalidCount = :invalid, j.updatedAt = CURRENT_TIMESTAMP WHERE j.batchId = :batchId")
    void updateValidationCounts(@Param("batchId") UUID batchId, @Param("valid") int valid,
            @Param("invalid") int invalid);

    @Modifying
    @Query("UPDATE ImportJob j SET j.importedCount = :count, j.updatedAt = CURRENT_TIMESTAMP WHERE j.batchId = :batchId")
    void updateImportedCount(@Param("batchId") UUID batchId, @Param("count") int count);

    @Modifying
    @Query("UPDATE ImportJob j SET j.avatarUploaded = :count, j.updatedAt = CURRENT_TIMESTAMP WHERE j.batchId = :batchId")
    void updateAvatarUploaded(@Param("batchId") UUID batchId, @Param("count") int count);

    @Modifying
    @Query("UPDATE ImportJob j SET j.avatarCount = :count, j.updatedAt = CURRENT_TIMESTAMP WHERE j.batchId = :batchId")
    void updateAvatarCount(@Param("batchId") UUID batchId, @Param("count") int count);
}
