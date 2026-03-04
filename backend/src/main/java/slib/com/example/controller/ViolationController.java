package slib.com.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.reputation.*;
import slib.com.example.entity.users.User;
import slib.com.example.service.ViolationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/slib/violations")
@RequiredArgsConstructor
public class ViolationController {

    private final ViolationService violationService;

    /**
     * Staff: Create a new violation record for a student
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ViolationRecordResponse> createViolation(
            @Valid @RequestBody CreateViolationRequest request,
            @AuthenticationPrincipal User createdBy) {
        ViolationRecordResponse response = violationService.createViolation(request, createdBy);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all violations for a specific student
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<ViolationRecordResponse>> getViolationsByStudent(@PathVariable UUID studentId) {
        List<ViolationRecordResponse> violations = violationService.getViolationsByStudent(studentId);
        return ResponseEntity.ok(violations);
    }

    /**
     * Student: Get my violations
     */
    @GetMapping("/my-violations")
    public ResponseEntity<List<ViolationRecordResponse>> getMyViolations(@AuthenticationPrincipal User user) {
        List<ViolationRecordResponse> violations = violationService.getViolationsByStudent(user.getId());
        return ResponseEntity.ok(violations);
    }

    /**
     * Get a specific violation by ID
     */
    @GetMapping("/{violationId}")
    public ResponseEntity<ViolationRecordResponse> getViolationById(@PathVariable UUID violationId) {
        ViolationRecordResponse violation = violationService.getViolationById(violationId);
        return ResponseEntity.ok(violation);
    }
}
