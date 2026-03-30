package slib.com.example.repository.system;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.system.SystemLogEntity;
import slib.com.example.entity.system.SystemLogEntity.LogCategory;
import slib.com.example.entity.system.SystemLogEntity.LogLevel;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLogEntity, UUID> {

    /**
     * Combined filter query with pagination.
     * All params are optional (null = skip filter).
     * Default sort: createdAt DESC.
     */
    @Query("SELECT s FROM SystemLogEntity s WHERE " +
            "(:level IS NULL OR s.level = :level) AND " +
            "(:category IS NULL OR s.category = :category) AND " +
            "(:search IS NULL OR LOWER(s.message) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "  OR LOWER(s.service) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:startDate IS NULL OR s.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR s.createdAt <= :endDate) " +
            "ORDER BY s.createdAt DESC")
    Page<SystemLogEntity> findLogs(
            @Param("level") LogLevel level,
            @Param("category") LogCategory category,
            @Param("search") String search,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    long countByLevel(LogLevel level);

    @Query("SELECT COUNT(s) FROM SystemLogEntity s WHERE s.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(s) FROM SystemLogEntity s WHERE s.level = :level AND s.createdAt >= :since")
    long countByLevelSince(@Param("level") LogLevel level, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(s) FROM SystemLogEntity s WHERE s.level = :level " +
            "AND (:startDate IS NULL OR s.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR s.createdAt <= :endDate)")
    long countByLevelBetween(@Param("level") LogLevel level,
                             @Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    /** Retention: delete logs older than cutoff date */
    @Modifying
    @Transactional
    @Query("DELETE FROM SystemLogEntity s WHERE s.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
