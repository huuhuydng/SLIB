package slib.com.example.repository.zone_config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.zone_config.LayoutScheduleEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LayoutScheduleRepository extends JpaRepository<LayoutScheduleEntity, Long> {

    Optional<LayoutScheduleEntity> findFirstByStatusOrderByScheduledForAsc(String status);

    List<LayoutScheduleEntity> findByStatusOrderByScheduledForAsc(String status);

    List<LayoutScheduleEntity> findByStatusAndScheduledForLessThanEqualOrderByScheduledForAsc(
            String status,
            LocalDateTime scheduledFor
    );
}
