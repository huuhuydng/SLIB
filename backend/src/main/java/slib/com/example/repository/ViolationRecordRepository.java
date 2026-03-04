package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.reputation.ViolationRecord;
import slib.com.example.entity.users.User;

import java.util.List;
import java.util.UUID;

@Repository
public interface ViolationRecordRepository extends JpaRepository<ViolationRecord, UUID> {
    
    /**
     * Find all violation records for a student
     */
    List<ViolationRecord> findByStudentOrderByCreatedAtDesc(User student);
    
    /**
     * Find all violation records by student ID
     */
    List<ViolationRecord> findByStudent_IdOrderByCreatedAtDesc(UUID studentId);
    
    /**
     * Find all active violations for a student
     */
    List<ViolationRecord> findByStudentAndStatus(User student, ViolationRecord.ViolationStatus status);
    
    /**
     * Count violations by student
     */
    long countByStudent(User student);
}
