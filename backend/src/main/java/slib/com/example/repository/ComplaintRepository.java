package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.complaint.ComplaintEntity;
import slib.com.example.entity.complaint.ComplaintEntity.ComplaintStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComplaintRepository extends JpaRepository<ComplaintEntity, UUID> {

    List<ComplaintEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<ComplaintEntity> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);

    List<ComplaintEntity> findAllByOrderByCreatedAtDesc();

    long countByStatus(ComplaintStatus status);

    // Dashboard: lấy 5 khiếu nại gần đây nhất
    List<ComplaintEntity> findTop5ByOrderByCreatedAtDesc();

    // Statistic: tổng complaints trong range
    long countByCreatedAtAfter(LocalDateTime startDate);
}
