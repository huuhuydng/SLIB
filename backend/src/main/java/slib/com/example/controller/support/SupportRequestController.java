package slib.com.example.controller.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.dto.support.SupportRequestDTO;
import slib.com.example.entity.support.SupportRequestStatus;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.support.SupportRequestService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/slib/support-requests")
@RequiredArgsConstructor
public class SupportRequestController {

    private final SupportRequestService supportRequestService;
    private final UserRepository userRepository;

    /**
     * Helper: lấy userId từ authentication
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
     * POST /slib/support-requests
     * Sinh viên tạo yêu cầu hỗ trợ (multipart form: description + images)
     */
    @PostMapping
    public ResponseEntity<SupportRequestDTO> create(
            @RequestParam("description") String description,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID studentId = getCurrentUserId(userDetails);
        SupportRequestDTO result = supportRequestService.create(studentId, description, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * GET /slib/support-requests
     * Thủ thư xem tất cả yêu cầu
     */
    @GetMapping
    public ResponseEntity<List<SupportRequestDTO>> getAll(
            @RequestParam(value = "status", required = false) String status) {
        if (status != null && !status.isEmpty()) {
            try {
                SupportRequestStatus requestStatus = SupportRequestStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(supportRequestService.getByStatus(requestStatus));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(supportRequestService.getAll());
    }

    /**
     * GET /slib/support-requests/my
     * Sinh viên xem yêu cầu của mình
     */
    @GetMapping("/my")
    public ResponseEntity<List<SupportRequestDTO>> getMyRequests(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID studentId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(supportRequestService.getByStudent(studentId));
    }

    /**
     * PUT /slib/support-requests/{id}/status
     * Thủ thư cập nhật trạng thái
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<SupportRequestDTO> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = getCurrentUserId(userDetails);
        String statusStr = body.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            SupportRequestStatus status = SupportRequestStatus.valueOf(statusStr.toUpperCase());
            return ResponseEntity.ok(supportRequestService.updateStatus(id, status, librarianId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /slib/support-requests/{id}/respond
     * Thủ thư phản hồi yêu cầu
     */
    @PutMapping("/{id}/respond")
    public ResponseEntity<SupportRequestDTO> respond(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = getCurrentUserId(userDetails);
        String response = body.get("response");
        if (response == null || response.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(supportRequestService.respond(id, response, librarianId));
    }

    /**
     * GET /slib/support-requests/count
     * Đếm số yêu cầu theo trạng thái
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount() {
        return ResponseEntity.ok(Map.of(
                "pending", supportRequestService.countByStatus(SupportRequestStatus.PENDING),
                "inProgress", supportRequestService.countByStatus(SupportRequestStatus.IN_PROGRESS),
                "resolved", supportRequestService.countByStatus(SupportRequestStatus.RESOLVED),
                "rejected", supportRequestService.countByStatus(SupportRequestStatus.REJECTED)));
    }

    /**
     * POST /slib/support-requests/{id}/chat
     * Thủ thư mở chat với sinh viên từ yêu cầu hỗ trợ
     */
    @PostMapping("/{id}/chat")
    public ResponseEntity<?> startChat(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            UUID librarianId = getCurrentUserId(userDetails);
            UUID conversationId = supportRequestService.startChatForRequest(id, librarianId);
            return ResponseEntity.ok(Map.of("conversationId", conversationId));
        } catch (Exception e) {
            log.error("[SupportRequest] Error starting chat for request {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
