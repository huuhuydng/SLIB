package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.system.BackupHistoryEntity;
import slib.com.example.entity.system.BackupHistoryEntity.BackupStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BackupHistoryRepository extends JpaRepository<BackupHistoryEntity, UUID> {

    List<BackupHistoryEntity> findAllByOrderByStartedAtDesc();

    List<BackupHistoryEntity> findByStatusOrderByStartedAtDesc(BackupStatus status);

    /** Find backups older than a given date for cleanup */
    List<BackupHistoryEntity> findByStartedAtBeforeAndStatus(LocalDateTime before, BackupStatus status);
}
