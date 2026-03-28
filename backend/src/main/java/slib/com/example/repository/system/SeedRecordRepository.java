package slib.com.example.repository.system;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.system.SeedRecordEntity;

import java.util.List;

@Repository
public interface SeedRecordRepository extends JpaRepository<SeedRecordEntity, Long> {

    List<SeedRecordEntity> findAllByOrderByIdDesc();

    boolean existsByEntityTypeAndEntityId(String entityType, String entityId);
}
