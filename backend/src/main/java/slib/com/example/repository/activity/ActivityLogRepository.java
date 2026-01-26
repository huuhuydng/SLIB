package slib.com.example.repository.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.activity.ActivityLogEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLogEntity, UUID> {

    List<ActivityLogEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT a FROM ActivityLogEntity a WHERE a.userId = :userId ORDER BY a.createdAt DESC LIMIT :limit")
    List<ActivityLogEntity> findByUserIdWithLimit(@Param("userId") UUID userId, @Param("limit") int limit);

    @Query("SELECT COUNT(a) FROM ActivityLogEntity a WHERE a.userId = :userId AND a.activityType = :type")
    long countByUserIdAndType(@Param("userId") UUID userId, @Param("type") String type);

    @Query("SELECT COALESCE(SUM(a.durationMinutes), 0) FROM ActivityLogEntity a WHERE a.userId = :userId AND a.activityType = 'CHECK_OUT'")
    long getTotalStudyMinutes(@Param("userId") UUID userId);

    // Delete all activity logs by user ID (for cascade delete when user is deleted)
    void deleteByUserId(UUID userId);
}
