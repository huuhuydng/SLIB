package slib.com.example.controller.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.dto.common.UuidBatchRequest;
import slib.com.example.dto.support.SupportRequestDTO;
import slib.com.example.dto.support.SupportRequestRespondRequest;
import slib.com.example.dto.support.SupportRequestStatusUpdateRequest;
import slib.com.example.entity.support.SupportRequestStatus;
import slib.com.example.entity.users.User;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.support.SupportRequestService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/slib/support-requests")
@RequiredArgsConstructor
@Validated
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
            @RequestParam("description")
            @NotBlank(message = "Nội dung hỗ trợ không được để trống")
            @Size(max = 2000, message = "Nội dung hỗ trợ không được vượt quá 2000 ký tự") String description,
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
            @Valid @RequestBody SupportRequestStatusUpdateRequest body,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(supportRequestService.updateStatus(id, body.getStatus(), librarianId));
    }

    /**
     * PUT /slib/support-requests/{id}/respond
     * Thủ thư phản hồi yêu cầu
     */
    @PutMapping("/{id}/respond")
    public ResponseEntity<SupportRequestDTO> respond(
            @PathVariable UUID id,
            @Valid @RequestBody SupportRequestRespondRequest body,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(supportRequestService.respond(id, body.getResponse(), librarianId));
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

    /**
     * DELETE /slib/support-requests/batch
     * Thủ thư xoá nhiều yêu cầu hỗ trợ cùng lúc
     */
    @DeleteMapping("/batch")
    public ResponseEntity<?> deleteBatch(@Valid @RequestBody UuidBatchRequest body) {
        try {
            List<UUID> uuids = body.getIds();
            supportRequestService.deleteBatch(uuids);
            return ResponseEntity.ok(Map.of("deleted", uuids.size()));
        } catch (Exception e) {
            log.error("[SupportRequest] Error deleting batch: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
