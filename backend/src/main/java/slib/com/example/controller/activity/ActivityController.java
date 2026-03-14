package slib.com.example.controller.activity;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.entity.activity.ActivityLogEntity;
import slib.com.example.entity.activity.PointTransactionEntity;
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

    /**
     * Get activity history with summary stats for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getActivityHistory(@PathVariable UUID userId) {
        try {
            List<ActivityLogEntity> activities = activityService.getActivitiesByUser(userId);
            double totalHours = activityService.getTotalStudyHours(userId);
            long totalVisits = activityService.getTotalVisits(userId);

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
    public ResponseEntity<?> getPointTransactions(@PathVariable UUID userId) {
        try {
            List<PointTransactionEntity> transactions = activityService.getPointTransactionsByUser(userId);
            int totalEarned = activityService.getTotalEarnedPoints(userId);
            int totalLost = activityService.getTotalLostPoints(userId);

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
    public ResponseEntity<?> getFullActivityHistory(@PathVariable UUID userId) {
        try {
            // Activities
            List<ActivityLogEntity> activities = activityService.getActivitiesByUser(userId);
            double totalHours = activityService.getTotalStudyHours(userId);
            long totalVisits = activityService.getTotalVisits(userId);

            // Points
            List<PointTransactionEntity> pointTransactions = activityService.getPointTransactionsByUser(userId);
            int totalEarned = activityService.getTotalEarnedPoints(userId);
            int totalLost = activityService.getTotalLostPoints(userId);

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
