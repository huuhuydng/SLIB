package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.feedback.SeatViolationReportEntity;
import slib.com.example.entity.feedback.SeatViolationReportEntity.ReportStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SeatViolationReportRepository extends JpaRepository<SeatViolationReportEntity, UUID> {

    List<SeatViolationReportEntity> findByReporter_IdOrderByCreatedAtDesc(UUID reporterId);

    List<SeatViolationReportEntity> findByViolator_IdOrderByCreatedAtDesc(UUID violatorId);

    List<SeatViolationReportEntity> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<SeatViolationReportEntity> findAllByOrderByCreatedAtDesc();

    long countByStatus(ReportStatus status);

    // Dashboard: count violations in a date range
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Dashboard: lấy 5 vi phạm gần đây nhất
    List<SeatViolationReportEntity> findTop5ByOrderByCreatedAtDesc();

    // Statistic: phân bổ vi phạm theo loại trong range
    @Query(value = "SELECT violation_type, COUNT(*) as cnt FROM seat_violation_reports " +
            "WHERE created_at >= :startDate GROUP BY violation_type ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> countByViolationTypeAfter(@Param("startDate") LocalDateTime startDate);

    // Statistic: tổng vi phạm trong range
    long countByCreatedAtAfter(LocalDateTime startDate);
}
