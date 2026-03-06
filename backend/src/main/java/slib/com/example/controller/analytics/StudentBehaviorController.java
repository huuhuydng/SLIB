package slib.com.example.controller.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.analytics.BehaviorSummaryDTO;
import slib.com.example.dto.analytics.StudentBehaviorAnalyticsDTO;
import slib.com.example.service.StudentBehaviorService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/slib/analytics")
@RequiredArgsConstructor
public class StudentBehaviorController {

    private final StudentBehaviorService behaviorService;

    /**
     * Lấy analytics của một sinh viên (mặc định 30 ngày)
     */
    @GetMapping("/student/{userId}")
    public ResponseEntity<StudentBehaviorAnalyticsDTO> getStudentAnalytics(@PathVariable UUID userId) {
        try {
            StudentBehaviorAnalyticsDTO analytics = behaviorService.getStudentAnalytics(userId);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy analytics với số ngày tùy chỉnh
     */
    @GetMapping("/student/{userId}/days/{days}")
    public ResponseEntity<StudentBehaviorAnalyticsDTO> getStudentAnalyticsWithDays(
            @PathVariable UUID userId,
            @PathVariable int days) {
        try {
            if (days < 1 || days > 365) {
                return ResponseEntity.badRequest().build();
            }
            StudentBehaviorAnalyticsDTO analytics = behaviorService.getStudentAnalytics(userId, days);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy tổng hợp behavior của tất cả sinh viên (cho AI)
     * AI service sẽ gọi endpoint này để lấy dữ liệu phân tích
     */
    @GetMapping("/behavior-summary")
    public ResponseEntity<BehaviorSummaryDTO> getBehaviorSummary(
            @RequestParam(defaultValue = "30") int days) {
        try {
            if (days < 1 || days > 365) {
                return ResponseEntity.badRequest().build();
            }
            BehaviorSummaryDTO summary = behaviorService.getBehaviorSummary(days);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy danh sách sinh viên có vấn đề hành vi (no-show cao)
     */
    @GetMapping("/behavior-issues")
    public ResponseEntity<List<StudentBehaviorAnalyticsDTO>> getStudentsWithBehaviorIssues(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "0.3") double minNoShowRate) {
        try {
            if (days < 1 || days > 365 || minNoShowRate < 0 || minNoShowRate > 1) {
                return ResponseEntity.badRequest().build();
            }
            List<StudentBehaviorAnalyticsDTO> issues = behaviorService.getStudentsWithBehaviorIssues(days, minNoShowRate);
            return ResponseEntity.ok(issues);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
