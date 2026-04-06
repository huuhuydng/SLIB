package slib.com.example.repository.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.activity.PointTransactionEntity;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransactionEntity, UUID> {

    List<PointTransactionEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT p FROM PointTransactionEntity p WHERE p.userId = :userId ORDER BY p.createdAt DESC LIMIT :limit")
    List<PointTransactionEntity> findByUserIdWithLimit(@Param("userId") UUID userId, @Param("limit") int limit);

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointTransactionEntity p WHERE p.userId = :userId AND p.points > 0")
    int getTotalEarnedPoints(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointTransactionEntity p WHERE p.userId = :userId AND p.points < 0")
    int getTotalLostPoints(@Param("userId") UUID userId);

    List<PointTransactionEntity> findByUserIdAndPointsLessThanOrderByCreatedAtDesc(UUID userId, int threshold);

    Optional<PointTransactionEntity> findTopByUserIdAndPointsLessThanOrderByCreatedAtDesc(UUID userId, int threshold);

    @Query("""
            SELECT COUNT(p)
            FROM PointTransactionEntity p
            WHERE p.userId = :userId
              AND p.points < 0
              AND p.rule IS NOT NULL
              AND p.rule.ruleCode = :ruleCode
              AND p.createdAt >= :since
            """)
    long countPenaltyByUserAndRuleCodeSince(
            @Param("userId") UUID userId,
            @Param("ruleCode") String ruleCode,
            @Param("since") ZonedDateTime since);

    // Delete all point transactions by user ID (for cascade delete when user is
    // deleted)
    void deleteByUserId(UUID userId);
}
