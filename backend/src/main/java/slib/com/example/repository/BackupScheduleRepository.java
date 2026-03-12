package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.system.BackupScheduleEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface BackupScheduleRepository extends JpaRepository<BackupScheduleEntity, Integer> {

    List<BackupScheduleEntity> findByIsActiveTrue();

    Optional<BackupScheduleEntity> findFirstByOrderByIdAsc();
}
