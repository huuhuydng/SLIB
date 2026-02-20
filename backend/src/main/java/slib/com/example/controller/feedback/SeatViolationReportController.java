package slib.com.example.controller.feedback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.dto.feedback.CreateViolationReportRequest;
import slib.com.example.dto.feedback.ViolationReportResponse;
import slib.com.example.entity.feedback.SeatViolationReportEntity.ReportStatus;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.SeatViolationReportService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/slib/violation-reports")
@RequiredArgsConstructor
public class SeatViolationReportController {

    private final SeatViolationReportService violationReportService;
    private final UserRepository userRepository;

    /**
     * Helper: lay userId tu authentication
     */
    private UUID getCurrentUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    /**
     * POST /slib/violation-reports
     * Sinh vien tao bao cao vi pham (multipart: seatId, violationType, description,
     * images)
     */
    @PostMapping
    public ResponseEntity<ViolationReportResponse> create(
            @RequestParam("seatId") Integer seatId,
            @RequestParam("violationType") String violationType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID reporterId = getCurrentUserId(userDetails);

        CreateViolationReportRequest request = new CreateViolationReportRequest();
        request.setSeatId(seatId);
        request.setViolationType(violationType);
        request.setDescription(description);

        ViolationReportResponse result = violationReportService.createReport(reporterId, request, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * GET /slib/violation-reports/my
     * Sinh vien xem bao cao cua minh
     */
    @GetMapping("/my")
    public ResponseEntity<List<ViolationReportResponse>> getMyReports(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID reporterId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(violationReportService.getMyReports(reporterId));
    }

    /**
     * GET /slib/violation-reports
     * Thu thu xem tat ca bao cao (filter theo ?status=PENDING)
     */
    @GetMapping
    public ResponseEntity<List<ViolationReportResponse>> getAll(
            @RequestParam(value = "status", required = false) String status) {
        if (status != null && !status.isEmpty()) {
            try {
                ReportStatus reportStatus = ReportStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(violationReportService.getByStatus(reportStatus));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(violationReportService.getAll());
    }

    /**
     * PUT /slib/violation-reports/{id}/verify
     * Thu thu xac minh bao cao + tru diem
     */
    @PutMapping("/{id}/verify")
    public ResponseEntity<ViolationReportResponse> verify(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(violationReportService.verifyReport(id, librarianId));
    }

    /**
     * PUT /slib/violation-reports/{id}/reject
     * Thu thu tu choi bao cao
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<ViolationReportResponse> reject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(violationReportService.rejectReport(id, librarianId));
    }

    /**
     * GET /slib/violation-reports/count
     * Dem so bao cao theo trang thai
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount() {
        return ResponseEntity.ok(Map.of(
                "pending", violationReportService.countByStatus(ReportStatus.PENDING),
                "verified", violationReportService.countByStatus(ReportStatus.VERIFIED),
                "resolved", violationReportService.countByStatus(ReportStatus.RESOLVED),
                "rejected", violationReportService.countByStatus(ReportStatus.REJECTED)));
    }
}
