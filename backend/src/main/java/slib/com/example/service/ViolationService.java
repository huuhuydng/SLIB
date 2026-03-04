package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.reputation.*;
import slib.com.example.entity.reputation.ViolationAppeal;
import slib.com.example.entity.reputation.ViolationRecord;
import slib.com.example.entity.users.StudentProfile;
import slib.com.example.entity.users.User;
import slib.com.example.repository.StudentProfileRepository;
import slib.com.example.repository.UserRepository;
import slib.com.example.repository.ViolationAppealRepository;
import slib.com.example.repository.ViolationRecordRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ViolationService {

    private final ViolationRecordRepository violationRecordRepository;
    private final ViolationAppealRepository violationAppealRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;

    /**
     * Create a new violation record for a student
     */
    @Transactional
    public ViolationRecordResponse createViolation(CreateViolationRequest request, User createdBy) {
        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        ViolationRecord violation = ViolationRecord.builder()
                .student(student)
                .createdBy(createdBy)
                .violationReason(request.getViolationReason())
                .penaltyPoints(request.getPenaltyPoints())
                .status(ViolationRecord.ViolationStatus.ACTIVE)
                .notes(request.getNotes())
                .build();

        violation = violationRecordRepository.save(violation);

        // Update student profile
        StudentProfile profile = studentProfileRepository.findById(student.getId())
                .orElseThrow(() -> new RuntimeException("Student profile not found"));
        
        profile.setReputationScore(Math.max(0, profile.getReputationScore() - request.getPenaltyPoints()));
        profile.setViolationCount(profile.getViolationCount() + 1);
        studentProfileRepository.save(profile);

        return mapToViolationResponse(violation);
    }

    /**
     * Get all violations for a student
     */
    public List<ViolationRecordResponse> getViolationsByStudent(UUID studentId) {
        List<ViolationRecord> violations = violationRecordRepository.findByStudent_IdOrderByCreatedAtDesc(studentId);
        
        return violations.stream()
                .map(this::mapToViolationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a violation record by ID
     */
    public ViolationRecordResponse getViolationById(UUID violationId) {
        ViolationRecord violation = violationRecordRepository.findById(violationId)
                .orElseThrow(() -> new RuntimeException("Violation not found"));
        
        return mapToViolationResponse(violation);
    }

    /**
     * Create an appeal for a violation
     */
    @Transactional
    public ViolationAppealResponse createAppeal(CreateAppealRequest request, User student) {
        ViolationRecord violation = violationRecordRepository.findById(request.getViolationId())
                .orElseThrow(() -> new RuntimeException("Violation not found"));

        // Check if violation belongs to student
        if (!violation.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("You can only appeal your own violations");
        }

        // Check if violation already has a pending appeal
        if (violationAppealRepository.existsByViolationAndStatus(violation, ViolationAppeal.AppealStatus.PENDING)) {
            throw new RuntimeException("This violation already has a pending appeal");
        }

        // Check if violation is active
        if (violation.getStatus() != ViolationRecord.ViolationStatus.ACTIVE) {
            throw new RuntimeException("You can only appeal active violations");
        }

        ViolationAppeal appeal = ViolationAppeal.builder()
                .violation(violation)
                .student(student)
                .appealReason(request.getAppealReason())
                .status(ViolationAppeal.AppealStatus.PENDING)
                .build();

        appeal = violationAppealRepository.save(appeal);

        // Update violation status
        violation.setStatus(ViolationRecord.ViolationStatus.APPEALED);
        violationRecordRepository.save(violation);

        return mapToAppealResponse(appeal);
    }

    /**
     * Get all appeals (for staff)
     */
    public List<ViolationAppealResponse> getAllAppeals() {
        List<ViolationAppeal> appeals = violationAppealRepository.findAll();
        return appeals.stream()
                .map(this::mapToAppealResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all pending appeals (for staff)
     */
    public List<ViolationAppealResponse> getPendingAppeals() {
        List<ViolationAppeal> appeals = violationAppealRepository.findByStatusOrderByCreatedAtAsc(ViolationAppeal.AppealStatus.PENDING);
        return appeals.stream()
                .map(this::mapToAppealResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all appeals by student
     */
    public List<ViolationAppealResponse> getAppealsByStudent(UUID studentId) {
        List<ViolationAppeal> appeals = violationAppealRepository.findByStudent_IdOrderByCreatedAtDesc(studentId);
        return appeals.stream()
                .map(this::mapToAppealResponse)
                .collect(Collectors.toList());
    }

    /**
     * Review an appeal (for staff)
     */
    @Transactional
    public ViolationAppealResponse reviewAppeal(UUID appealId, ReviewAppealRequest request, User reviewer) {
        ViolationAppeal appeal = violationAppealRepository.findById(appealId)
                .orElseThrow(() -> new RuntimeException("Appeal not found"));

        if (appeal.getStatus() != ViolationAppeal.AppealStatus.PENDING) {
            throw new RuntimeException("This appeal has already been reviewed");
        }

        ViolationRecord violation = appeal.getViolation();
        StudentProfile profile = studentProfileRepository.findById(appeal.getStudent().getId())
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        if (request.getApproved()) {
            // Approve appeal - restore points and cancel violation
            appeal.setStatus(ViolationAppeal.AppealStatus.APPROVED);
            violation.setStatus(ViolationRecord.ViolationStatus.CANCELLED);
            
            // Restore reputation score
            profile.setReputationScore(Math.min(100, profile.getReputationScore() + violation.getPenaltyPoints()));
            profile.setViolationCount(Math.max(0, profile.getViolationCount() - 1));
        } else {
            // Reject appeal
            appeal.setStatus(ViolationAppeal.AppealStatus.REJECTED);
            violation.setStatus(ViolationRecord.ViolationStatus.DISMISSED);
        }

        appeal.setReviewedBy(reviewer);
        appeal.setReviewNotes(request.getReviewNotes());
        appeal.setReviewedAt(LocalDateTime.now());

        appeal = violationAppealRepository.save(appeal);
        violationRecordRepository.save(violation);
        studentProfileRepository.save(profile);

        return mapToAppealResponse(appeal);
    }

    /**
     * Get appeals by student ID for a specific violation
     */
    public List<ViolationAppealResponse> getAppealsByViolation(UUID violationId) {
        List<ViolationAppeal> appeals = violationAppealRepository.findByViolation_IdOrderByCreatedAtDesc(violationId);
        return appeals.stream()
                .map(this::mapToAppealResponse)
                .collect(Collectors.toList());
    }

    // Helper methods to map entities to DTOs
    private ViolationRecordResponse mapToViolationResponse(ViolationRecord violation) {
        ViolationAppealResponse appealResponse = null;
        
        // Get the latest appeal for this violation if exists
        List<ViolationAppeal> appeals = violationAppealRepository.findByViolation_IdOrderByCreatedAtDesc(violation.getId());
        if (!appeals.isEmpty()) {
            appealResponse = mapToAppealResponse(appeals.get(0));
        }

        return ViolationRecordResponse.builder()
                .id(violation.getId())
                .studentId(violation.getStudent().getId())
                .studentName(violation.getStudent().getFullName())
                .studentCode(violation.getStudent().getUserCode())
                .violationReason(violation.getViolationReason())
                .penaltyPoints(violation.getPenaltyPoints())
                .status(violation.getStatus().name())
                .notes(violation.getNotes())
                .createdByName(violation.getCreatedBy() != null ? violation.getCreatedBy().getFullName() : null)
                .createdAt(violation.getCreatedAt())
                .updatedAt(violation.getUpdatedAt())
                .appeal(appealResponse)
                .build();
    }

    private ViolationAppealResponse mapToAppealResponse(ViolationAppeal appeal) {
        ViolationRecord violation = appeal.getViolation();
        
        return ViolationAppealResponse.builder()
                .id(appeal.getId())
                .violationId(violation.getId())
                .studentId(appeal.getStudent().getId())
                .studentName(appeal.getStudent().getFullName())
                .studentCode(appeal.getStudent().getUserCode())
                .appealReason(appeal.getAppealReason())
                .status(appeal.getStatus().name())
                .reviewedByName(appeal.getReviewedBy() != null ? appeal.getReviewedBy().getFullName() : null)
                .reviewNotes(appeal.getReviewNotes())
                .reviewedAt(appeal.getReviewedAt())
                .createdAt(appeal.getCreatedAt())
                .updatedAt(appeal.getUpdatedAt())
                .violationReason(violation.getViolationReason())
                .penaltyPoints(violation.getPenaltyPoints())
                .violationDate(violation.getCreatedAt())
                .build();
    }
}
