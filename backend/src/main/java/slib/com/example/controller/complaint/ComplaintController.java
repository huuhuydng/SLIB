package slib.com.example.controller.complaint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.common.UuidBatchRequest;
import slib.com.example.dto.complaint.ComplaintDTO;
import slib.com.example.dto.complaint.ComplaintResolutionRequest;
import slib.com.example.dto.complaint.CreateComplaintRequest;
import slib.com.example.entity.complaint.ComplaintEntity.ComplaintStatus;
import slib.com.example.entity.users.User;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.complaint.ComplaintService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/slib/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserRepository userRepository;

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
     * GET /slib/complaints
     * Thủ thư xem tất cả khiếu nại
     */
    @GetMapping
    public ResponseEntity<List<ComplaintDTO>> getAll(
            @RequestParam(value = "status", required = false) String status) {
        if (status != null && !status.isEmpty()) {
            try {
                ComplaintStatus complaintStatus = ComplaintStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(complaintService.getByStatus(complaintStatus));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(complaintService.getAll());
    }

    /**
     * GET /slib/complaints/my
     * Sinh viên xem khiếu nại của mình
     */
    @GetMapping("/my")
    public ResponseEntity<List<ComplaintDTO>> getMyComplaints(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID studentId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(complaintService.getByStudent(studentId));
    }

    /**
     * POST /slib/complaints
     * Sinh viên tạo khiếu nại
     */
    @PostMapping
    public ResponseEntity<ComplaintDTO> create(
            @Valid @RequestBody CreateComplaintRequest body,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID studentId = getCurrentUserId(userDetails);
        ComplaintDTO result = complaintService.create(studentId, body.getSubject(), body.getContent(), body.getEvidenceUrl(),
                body.getPointTransactionId(), body.getViolationReportId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * PUT /slib/complaints/{id}/accept
     * Thủ thư chấp nhận khiếu nại
     */
    @PutMapping("/{id}/accept")
    public ResponseEntity<ComplaintDTO> accept(
            @PathVariable UUID id,
            @RequestBody(required = false) @Valid ComplaintResolutionRequest body,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = getCurrentUserId(userDetails);
        String note = body != null ? body.getNote() : null;
        return ResponseEntity.ok(complaintService.accept(id, librarianId, note));
    }

    /**
     * PUT /slib/complaints/{id}/deny
     * Thủ thư từ chối khiếu nại
     */
    @PutMapping("/{id}/deny")
    public ResponseEntity<ComplaintDTO> deny(
            @PathVariable UUID id,
            @RequestBody(required = false) @Valid ComplaintResolutionRequest body,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = getCurrentUserId(userDetails);
        String note = body != null ? body.getNote() : null;
        return ResponseEntity.ok(complaintService.deny(id, librarianId, note));
    }

    /**
     * GET /slib/complaints/count
     * Đếm khiếu nại theo trạng thái
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount() {
        return ResponseEntity.ok(Map.of(
                "pending", complaintService.countByStatus(ComplaintStatus.PENDING),
                "accepted", complaintService.countByStatus(ComplaintStatus.ACCEPTED),
                "denied", complaintService.countByStatus(ComplaintStatus.DENIED)));
    }

    /**
     * DELETE /slib/complaints/batch
     * Thủ thư xoá nhiều khiếu nại cùng lúc
     */
    @DeleteMapping("/batch")
    public ResponseEntity<?> deleteBatch(@Valid @RequestBody UuidBatchRequest body) {
        try {
            List<UUID> uuids = body.getIds();
            complaintService.deleteBatch(uuids);
            return ResponseEntity.ok(Map.of("deleted", uuids.size()));
        } catch (Exception e) {
            log.error("[Complaint] Error deleting batch: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
