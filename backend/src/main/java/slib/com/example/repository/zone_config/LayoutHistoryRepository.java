package slib.com.example.repository.zone_config;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.zone_config.LayoutHistoryEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface LayoutHistoryRepository extends JpaRepository<LayoutHistoryEntity, Long> {

    List<LayoutHistoryEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT MAX(h.publishedVersion) FROM LayoutHistoryEntity h WHERE h.publishedVersion IS NOT NULL")
    Optional<Long> findLatestPublishedVersion();
}
