package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.feedback.SeatViolationReportEntity;
import slib.com.example.entity.feedback.SeatViolationReportEntity.ReportStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeatViolationReportRepository extends JpaRepository<SeatViolationReportEntity, UUID> {

    List<SeatViolationReportEntity> findByReporter_IdOrderByCreatedAtDesc(UUID reporterId);

    List<SeatViolationReportEntity> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<SeatViolationReportEntity> findAllByOrderByCreatedAtDesc();

    long countByStatus(ReportStatus status);
}
