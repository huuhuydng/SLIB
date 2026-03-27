package slib.com.example.repository.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.feedback.SeatStatusReportEntity;
import slib.com.example.entity.feedback.SeatStatusReportEntity.ReportStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeatStatusReportRepository extends JpaRepository<SeatStatusReportEntity, UUID> {
    List<SeatStatusReportEntity> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    List<SeatStatusReportEntity> findAllByOrderByCreatedAtDesc();

    List<SeatStatusReportEntity> findByStatusOrderByCreatedAtDesc(ReportStatus status);
}
