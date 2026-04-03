package slib.com.example.repository.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.support.SupportRequest;
import slib.com.example.entity.support.SupportRequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import java.util.UUID;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, UUID> {

    List<SupportRequest> findAllByOrderByCreatedAtDesc();

    List<SupportRequest> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    List<SupportRequest> findByStatusOrderByCreatedAtDesc(SupportRequestStatus status);

    List<SupportRequest> findByStatusInOrderByCreatedAtDesc(Collection<SupportRequestStatus> statuses);

    long countByStatus(SupportRequestStatus status);

    long countByStatusIn(Collection<SupportRequestStatus> statuses);

    long countByStatusAndCreatedAtBefore(SupportRequestStatus status, LocalDateTime threshold);

    long countByStatusAndCreatedAtBetween(SupportRequestStatus status, LocalDateTime start, LocalDateTime end);

    // Dashboard: lấy 5 yêu cầu hỗ trợ gần đây nhất
    List<SupportRequest> findTop5ByOrderByCreatedAtDesc();
}
