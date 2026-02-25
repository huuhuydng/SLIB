package slib.com.example.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import slib.com.example.service.SeedDataService;

import java.util.Map;

/**
 * Controller tạo/xoá dữ liệu mẫu cho hệ thống SLIB.
 * 
 * Sử dụng qua Postman:
 * 
 * 1. Tạo tất cả dữ liệu mẫu (bao gồm dashboard):
 * POST http://localhost:8080/slib/seed/all?bookings=15&violations=8&supports=8
 * 
 * 2. Tạo riêng từng loại:
 * POST http://localhost:8080/slib/seed/bookings?count=15
 * POST http://localhost:8080/slib/seed/access-logs?count=50
 * POST http://localhost:8080/slib/seed/violations?count=8
 * POST http://localhost:8080/slib/seed/support-requests?count=8
 * POST http://localhost:8080/slib/seed/complaints?count=5
 * POST http://localhost:8080/slib/seed/feedbacks?count=8
 * 
 * 3. Xoá dữ liệu seed:
 * DELETE http://localhost:8080/slib/seed/clear
 * 
 * 4. Xoá tất cả bookings:
 * DELETE http://localhost:8080/slib/seed/bookings
 */
@RestController
@RequestMapping("/slib/seed")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class SeedDataController {

    private final SeedDataService seedDataService;

    /**
     * Tạo tất cả dữ liệu mẫu cùng lúc (bao gồm dashboard data)
     * POST /slib/seed/all?bookings=15&violations=8&supports=8
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> seedAll(
            @RequestParam(defaultValue = "15") int bookings,
            @RequestParam(defaultValue = "8") int violations,
            @RequestParam(defaultValue = "8") int supports) {
        return ResponseEntity.ok(seedDataService.seedAll(bookings, violations, supports));
    }

    /**
     * Tạo dữ liệu mẫu booking (trải 7 ngày)
     * POST /slib/seed/bookings?count=15
     */
    @PostMapping("/bookings")
    public ResponseEntity<Map<String, Object>> seedBookings(
            @RequestParam(defaultValue = "15") int count) {
        return ResponseEntity.ok(seedDataService.seedBookings(count));
    }

    /**
     * Tạo dữ liệu mẫu access logs (check-in/out trải 7 ngày)
     * POST /slib/seed/access-logs?count=50
     */
    @PostMapping("/access-logs")
    public ResponseEntity<Map<String, Object>> seedAccessLogs(
            @RequestParam(defaultValue = "50") int count) {
        return ResponseEntity.ok(seedDataService.seedAccessLogs(count));
    }

    /**
     * Tạo dữ liệu mẫu vi phạm
     * POST /slib/seed/violations?count=8
     */
    @PostMapping("/violations")
    public ResponseEntity<Map<String, Object>> seedViolations(
            @RequestParam(defaultValue = "8") int count) {
        return ResponseEntity.ok(seedDataService.seedViolations(count));
    }

    /**
     * Tạo dữ liệu mẫu yêu cầu hỗ trợ
     * POST /slib/seed/support-requests?count=8
     */
    @PostMapping("/support-requests")
    public ResponseEntity<Map<String, Object>> seedSupportRequests(
            @RequestParam(defaultValue = "8") int count) {
        return ResponseEntity.ok(seedDataService.seedSupportRequests(count));
    }

    /**
     * Tạo dữ liệu mẫu khiếu nại
     * POST /slib/seed/complaints?count=5
     */
    @PostMapping("/complaints")
    public ResponseEntity<Map<String, Object>> seedComplaints(
            @RequestParam(defaultValue = "5") int count) {
        return ResponseEntity.ok(seedDataService.seedComplaints(count));
    }

    /**
     * Tạo dữ liệu mẫu phản hồi
     * POST /slib/seed/feedbacks?count=8
     */
    @PostMapping("/feedbacks")
    public ResponseEntity<Map<String, Object>> seedFeedbacks(
            @RequestParam(defaultValue = "8") int count) {
        return ResponseEntity.ok(seedDataService.seedFeedbacks(count));
    }

    /**
     * Xoá dữ liệu seed (chỉ xoá dữ liệu có marker [SEED])
     * DELETE /slib/seed/clear
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearSeedData() {
        return ResponseEntity.ok(seedDataService.clearSeedData());
    }

    /**
     * Xoá tất cả bookings
     * DELETE /slib/seed/bookings
     */
    @DeleteMapping("/bookings")
    public ResponseEntity<Map<String, Object>> clearAllBookings() {
        return ResponseEntity.ok(seedDataService.clearAllBookings());
    }

    /**
     * Tạo dữ liệu test tính năng báo cáo vi phạm
     * POST /slib/seed/violation-test?userCode=SL000001&neighbors=4&sameZone=true
     * 
     * Tạo booking CONFIRMED cho user chính + nhiều user khác (cùng zone hoặc khác
     * zone tuỳ sameZone)
     */
    @PostMapping("/violation-test")
    public ResponseEntity<Map<String, Object>> seedViolationTestData(
            @RequestParam String userCode,
            @RequestParam(defaultValue = "4") int neighbors,
            @RequestParam(defaultValue = "true") boolean sameZone) {
        return ResponseEntity.ok(seedDataService.seedViolationTestData(userCode, neighbors, sameZone));
    }

    /**
     * Tạo dữ liệu test tính năng nhắc nhở lịch đặt chỗ (sau 15 phút)
     * POST /slib/seed/reminder-test?userCode=SL000001
     */
    @PostMapping("/reminder-test")
    public ResponseEntity<Map<String, Object>> seedReminderTestData(
            @RequestParam String userCode) {
        return ResponseEntity.ok(seedDataService.seedReminderTestData(userCode));
    }
}
