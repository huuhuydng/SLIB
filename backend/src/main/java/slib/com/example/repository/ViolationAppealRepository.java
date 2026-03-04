package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.reputation.ViolationAppeal;
import slib.com.example.entity.reputation.ViolationRecord;
import slib.com.example.entity.users.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ViolationAppealRepository extends JpaRepository<ViolationAppeal, UUID> {
    
    /**
     * Find appeal by violation record
     */
    Optional<ViolationAppeal> findByViolation(ViolationRecord violation);
    
    /**
     * Find all appeals by student
     */
    List<ViolationAppeal> findByStudentOrderByCreatedAtDesc(User student);
    
    /**
     * Find all appeals by student ID
     */
    List<ViolationAppeal> findByStudent_IdOrderByCreatedAtDesc(UUID studentId);
    
    /**
     * Find all pending appeals
     */
    List<ViolationAppeal> findByStatusOrderByCreatedAtAsc(ViolationAppeal.AppealStatus status);
    
    /**
     * Find all appeals by violation ID
     */
    List<ViolationAppeal> findByViolation_IdOrderByCreatedAtDesc(UUID violationId);
    
    /**
     * Check if violation already has a pending appeal
     */
    boolean existsByViolationAndStatus(ViolationRecord violation, ViolationAppeal.AppealStatus status);
}
