package slib.com.example.repository.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.activity.PointTransactionEntity;

import java.util.List;
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
}
