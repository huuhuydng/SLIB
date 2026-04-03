package slib.com.example.repository.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.support.SupportRequest;
import slib.com.example.entity.support.SupportRequestStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, UUID> {

    List<SupportRequest> findAllByOrderByCreatedAtDesc();

    List<SupportRequest> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    List<SupportRequest> findByStatusOrderByCreatedAtDesc(SupportRequestStatus status);

    long countByStatus(SupportRequestStatus status);
}
