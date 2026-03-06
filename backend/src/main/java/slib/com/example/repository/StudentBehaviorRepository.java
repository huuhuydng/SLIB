package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.analytics.StudentBehaviorEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StudentBehaviorRepository extends JpaRepository<StudentBehaviorEntity, Integer> {

    List<StudentBehaviorEntity> findByUserId(UUID userId);

    List<StudentBehaviorEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<StudentBehaviorEntity> findByUserIdAndBehaviorType(UUID userId, StudentBehaviorEntity.BehaviorType behaviorType);

    @Query("SELECT sb FROM StudentBehaviorEntity sb WHERE sb.userId = :userId AND sb.createdAt >= :fromDate")
    List<StudentBehaviorEntity> findByUserIdAndDateRange(@Param("userId") UUID userId, @Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT sb.behaviorType, COUNT(sb) FROM StudentBehaviorEntity sb WHERE sb.userId = :userId GROUP BY sb.behaviorType")
    List<Object[]> countBehaviorsByType(@Param("userId") UUID userId);

    @Query("SELECT COUNT(sb) FROM StudentBehaviorEntity sb WHERE sb.userId = :userId AND sb.behaviorType = :behaviorType AND sb.createdAt >= :fromDate")
    long countBehaviorSince(@Param("userId") UUID userId, @Param("behaviorType") StudentBehaviorEntity.BehaviorType behaviorType, @Param("fromDate") LocalDateTime fromDate);

    // Aggregated queries for all students
    @Query("SELECT sb.userId, COUNT(sb), SUM(CASE WHEN sb.behaviorType = 'BOOKING_NO_SHOW' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN sb.behaviorType = 'BOOKING_CANCELLED' THEN 1 ELSE 0 END) " +
           "FROM StudentBehaviorEntity sb WHERE sb.createdAt >= :fromDate GROUP BY sb.userId")
    List<Object[]> getBehaviorStatsForAllStudents(@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT sb.userId, COUNT(sb) FROM StudentBehaviorEntity sb WHERE sb.createdAt >= :fromDate GROUP BY sb.userId ORDER BY COUNT(sb) DESC")
    List<Object[]> getMostActiveStudents(@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT sb.userId, COUNT(sb) FROM StudentBehaviorEntity sb WHERE sb.behaviorType = 'BOOKING_NO_SHOW' AND sb.createdAt >= :fromDate GROUP BY sb.userId ORDER BY COUNT(sb) DESC")
    List<Object[]> getStudentsWithMostNoShows(@Param("fromDate") LocalDateTime fromDate);
}
