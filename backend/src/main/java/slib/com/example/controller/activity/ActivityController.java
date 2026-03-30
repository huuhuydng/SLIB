package slib.com.example.controller.activity;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slib.com.example.entity.activity.ActivityLogEntity;
import slib.com.example.entity.activity.PointTransactionEntity;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.activity.ActivityService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/slib/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final UserRepository userRepository;

    private UUID resolveAuthorizedUserId(UUID requestedUserId, UserDetails userDetails) {
        if (userDetails == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }

        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.LIBRARIAN) {
            return requestedUserId;
        }
        if (!currentUser.getId().equals(requestedUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền truy cập dữ liệu hoạt động của người khác.");
        }
        return currentUser.getId();
    }

    /**
     * Get activity history with summary stats for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getActivityHistory(@PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);
            List<ActivityLogEntity> activities = activityService.getActivitiesByUser(resolvedUserId);
            double totalHours = activityService.getTotalStudyHours(resolvedUserId);
            long totalVisits = activityService.getTotalVisits(resolvedUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("activities", activities);
            response.put("totalStudyHours", totalHours);
            response.put("totalVisits", totalVisits);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get point transactions for a user
     */
    @GetMapping("/points/{userId}")
    public ResponseEntity<?> getPointTransactions(@PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);
            List<PointTransactionEntity> transactions = activityService.getPointTransactionsByUser(resolvedUserId);
            int totalEarned = activityService.getTotalEarnedPoints(resolvedUserId);
            int totalLost = activityService.getTotalLostPoints(resolvedUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("transactions", transactions);
            response.put("totalEarned", totalEarned);
            response.put("totalLost", totalLost);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get combined activity history (both tabs data)
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getFullActivityHistory(@PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);
            // Activities
            List<ActivityLogEntity> activities = activityService.getActivitiesByUser(resolvedUserId);
            double totalHours = activityService.getTotalStudyHours(resolvedUserId);
            long totalVisits = activityService.getTotalVisits(resolvedUserId);

            // Points
            List<PointTransactionEntity> pointTransactions = activityService.getPointTransactionsByUser(resolvedUserId);
            int totalEarned = activityService.getTotalEarnedPoints(resolvedUserId);
            int totalLost = activityService.getTotalLostPoints(resolvedUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("activities", activities);
            response.put("totalStudyHours", totalHours);
            response.put("totalVisits", totalVisits);
            response.put("pointTransactions", pointTransactions);
            response.put("totalPointsEarned", totalEarned);
            response.put("totalPointsLost", totalLost);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get penalty point transactions for a user (auto penalties: no-show, late check-in, late check-out, violations)
     */
    @GetMapping("/penalties/{userId}")
    public ResponseEntity<?> getPenalties(@PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);
            return ResponseEntity.ok(activityService.getPenaltyTransactions(resolvedUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Seed sample data for testing (call this once to populate sample data)
     */
    @PostMapping("/seed/{userId}")
    public ResponseEntity<?> seedSampleData(@PathVariable UUID userId) {
        try {
            activityService.seedSampleData(userId);
            return ResponseEntity.ok(Map.of("message", "Sample data seeded successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
