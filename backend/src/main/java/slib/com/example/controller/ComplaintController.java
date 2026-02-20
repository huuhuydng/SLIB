package slib.com.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.complaint.ComplaintDTO;
import slib.com.example.entity.complaint.ComplaintEntity.ComplaintStatus;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.ComplaintService;

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
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID studentId = getCurrentUserId(userDetails);
        String subject = body.get("subject");
        String content = body.get("content");
        String evidenceUrl = body.get("evidenceUrl");
        String txnId = body.get("pointTransactionId");

        if (subject == null || subject.isBlank() || content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        UUID pointTransactionId = txnId != null ? UUID.fromString(txnId) : null;
        ComplaintDTO result = complaintService.create(studentId, subject, content, evidenceUrl,
                pointTransactionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * PUT /slib/complaints/{id}/accept
     * Thủ thư chấp nhận khiếu nại
     */
    @PutMapping("/{id}/accept")
    public ResponseEntity<ComplaintDTO> accept(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = getCurrentUserId(userDetails);
        String note = body != null ? body.get("note") : null;
        return ResponseEntity.ok(complaintService.accept(id, librarianId, note));
    }

    /**
     * PUT /slib/complaints/{id}/deny
     * Thủ thư từ chối khiếu nại
     */
    @PutMapping("/{id}/deny")
    public ResponseEntity<ComplaintDTO> deny(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = getCurrentUserId(userDetails);
        String note = body != null ? body.get("note") : null;
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
}
