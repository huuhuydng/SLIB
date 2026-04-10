package slib.com.example.controller.users;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.users.ManualReputationAdjustmentRequest;
import slib.com.example.dto.users.StudentProfileResponse;
import slib.com.example.entity.users.User;
import slib.com.example.service.users.StudentProfileService;

import java.util.UUID;

@RestController
@RequestMapping("/slib/student-profile")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    /**
     * Get current user's student profile
     * Mobile app will use this endpoint
     */
    @GetMapping("/me")
    public ResponseEntity<StudentProfileResponse> getMyProfile(@AuthenticationPrincipal User user) {
        StudentProfileResponse profile = studentProfileService.getOrCreateProfile(user);
        return ResponseEntity.ok(profile);
    }

    /**
     * Get student profile by user ID (for staff/admin)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<StudentProfileResponse> getProfileByUserId(@PathVariable UUID userId) {
        return studentProfileService.getProfileByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Add study hours to current user's profile
     */
    @PostMapping("/add-hours")
    public ResponseEntity<StudentProfileResponse> addStudyHours(
            @AuthenticationPrincipal User user,
            @RequestParam double hours) {
        return studentProfileService.addStudyHours(user.getId(), hours)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Staff: Update reputation score for a student
     */
    @PutMapping("/{userId}/reputation")
    public ResponseEntity<StudentProfileResponse> updateReputation(
            @PathVariable UUID userId,
            @RequestParam int score) {
        return studentProfileService.updateReputationScore(userId, score)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Admin: Manual reputation adjustment with mandatory reason and audit trail
     */
    @PatchMapping("/{userId}/reputation/manual-adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> manualAdjustReputation(
            @PathVariable UUID userId,
            @Valid @RequestBody ManualReputationAdjustmentRequest request,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("error", "Token không hợp lệ hoặc hết hạn"));
        }

        try {
            return studentProfileService.applyManualReputationAdjustment(
                            userId,
                            request.points(),
                            request.reason(),
                            currentUser)
                    .<ResponseEntity<?>>map(profile -> ResponseEntity.ok(java.util.Map.of(
                            "message", "Đã điều chỉnh điểm uy tín",
                            "profile", profile)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Staff: Add violation to a student
     */
    @PostMapping("/{userId}/violation")
    public ResponseEntity<StudentProfileResponse> addViolation(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "10") int penaltyPoints) {
        return studentProfileService.addViolation(userId, penaltyPoints)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update current user's basic info (fullName, phone, dob)
     */
    @PutMapping("/me")
    public ResponseEntity<StudentProfileResponse> updateMyProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody slib.com.example.dto.users.UpdateProfileRequest request) {
        return studentProfileService.updateUserInfo(user.getId(), request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Upload avatar for current user
     */
    @PostMapping("/me/avatar")
    public ResponseEntity<StudentProfileResponse> uploadAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            return studentProfileService.updateAvatar(user.getId(), file)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
