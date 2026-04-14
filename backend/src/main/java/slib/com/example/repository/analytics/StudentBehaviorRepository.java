package slib.com.example.repository.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.analytics.StudentBehaviorEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentBehaviorRepository extends JpaRepository<StudentBehaviorEntity, Integer> {

    List<StudentBehaviorEntity> findByUserId(UUID userId);

    List<StudentBehaviorEntity> findByUserIdAndBehaviorType(UUID userId, StudentBehaviorEntity.BehaviorType behaviorType);

    @Query("""
            SELECT COUNT(b) > 0
            FROM StudentBehaviorEntity b
            WHERE b.userId = :userId
              AND b.behaviorType = :behaviorType
              AND (
                    (:logMarker <> '' AND b.metadata LIKE CONCAT('%', :logMarker, '%'))
                    OR (:checkInMarker <> '' AND b.metadata LIKE CONCAT('%', :checkInMarker, '%'))
                  )
            """)
    boolean existsRecordedBehavior(
            @Param("userId") UUID userId,
            @Param("behaviorType") StudentBehaviorEntity.BehaviorType behaviorType,
            @Param("logMarker") String logMarker,
            @Param("checkInMarker") String checkInMarker);
}
