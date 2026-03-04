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
@RequestMapping("/slib/appeals")
@RequiredArgsConstructor
public class ViolationAppealController {

    private final ViolationService violationService;

    /**
     * Student: Create an appeal for a violation
     */
    @PostMapping
    public ResponseEntity<ViolationAppealResponse> createAppeal(
            @Valid @RequestBody CreateAppealRequest request,
            @AuthenticationPrincipal User student) {
        ViolationAppealResponse response = violationService.createAppeal(request, student);
        return ResponseEntity.ok(response);
    }

    /**
     * Staff: Get all appeals
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<ViolationAppealResponse>> getAllAppeals() {
        List<ViolationAppealResponse> appeals = violationService.getAllAppeals();
        return ResponseEntity.ok(appeals);
    }

    /**
     * Staff: Get all pending appeals
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<ViolationAppealResponse>> getPendingAppeals() {
        List<ViolationAppealResponse> appeals = violationService.getPendingAppeals();
        return ResponseEntity.ok(appeals);
    }

    /**
     * Student: Get my appeals
     */
    @GetMapping("/my-appeals")
    public ResponseEntity<List<ViolationAppealResponse>> getMyAppeals(@AuthenticationPrincipal User user) {
        List<ViolationAppealResponse> appeals = violationService.getAppealsByStudent(user.getId());
        return ResponseEntity.ok(appeals);
    }

    /**
     * Get appeals for a specific student
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<ViolationAppealResponse>> getAppealsByStudent(@PathVariable UUID studentId) {
        List<ViolationAppealResponse> appeals = violationService.getAppealsByStudent(studentId);
        return ResponseEntity.ok(appeals);
    }

    /**
     * Staff: Review an appeal (approve or reject)
     */
    @PutMapping("/{appealId}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ViolationAppealResponse> reviewAppeal(
            @PathVariable UUID appealId,
            @Valid @RequestBody ReviewAppealRequest request,
            @AuthenticationPrincipal User reviewer) {
        ViolationAppealResponse response = violationService.reviewAppeal(appealId, request, reviewer);
        return ResponseEntity.ok(response);
    }

    /**
     * Get appeals for a specific violation
     */
    @GetMapping("/violation/{violationId}")
    public ResponseEntity<List<ViolationAppealResponse>> getAppealsByViolation(@PathVariable UUID violationId) {
        List<ViolationAppealResponse> appeals = violationService.getAppealsByViolation(violationId);
        return ResponseEntity.ok(appeals);
    }
}
