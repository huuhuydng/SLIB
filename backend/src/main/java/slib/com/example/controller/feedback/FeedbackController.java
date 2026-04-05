package slib.com.example.controller.feedback;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.common.UuidBatchRequest;
import slib.com.example.dto.feedback.CreateFeedbackRequest;
import slib.com.example.dto.feedback.FeedbackDTO;
import slib.com.example.entity.feedback.FeedbackEntity.FeedbackStatus;
import slib.com.example.entity.users.User;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.feedback.FeedbackService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/slib/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
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
     * GET /slib/feedbacks
     * Thủ thư xem tất cả phản hồi
     */
    @GetMapping
    public ResponseEntity<List<FeedbackDTO>> getAll(
            @RequestParam(value = "status", required = false) String status) {
        if (status != null && !status.isEmpty()) {
            try {
                FeedbackStatus feedbackStatus = FeedbackStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(feedbackService.getByStatus(feedbackStatus));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(feedbackService.getAll());
    }

    /**
     * GET /slib/feedbacks/my
     * Sinh viên xem phản hồi của mình
     */
    @GetMapping("/my")
    public ResponseEntity<List<FeedbackDTO>> getMyFeedbacks(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID studentId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(feedbackService.getByStudent(studentId));
    }

    /**
     * POST /slib/feedbacks
     * Sinh viên gửi phản hồi
     */
    @PostMapping
    public ResponseEntity<FeedbackDTO> create(
            @Valid @RequestBody CreateFeedbackRequest body,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID studentId = getCurrentUserId(userDetails);
        FeedbackDTO result = feedbackService.create(
                studentId,
                body.getRating(),
                body.getContent(),
                body.getCategory(),
                body.getConversationId() != null ? body.getConversationId().toString() : null,
                body.getReservationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * GET /slib/feedbacks/check-pending
     * Kiểm tra xem sinh viên có reservation đã xác nhận ghế và đã hết giờ chưa gửi phản hồi
     */
    @GetMapping("/check-pending")
    public ResponseEntity<Map<String, Object>> checkPending(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID studentId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(feedbackService.checkPendingFeedback(studentId));
    }

    /**
     * PUT /slib/feedbacks/{id}/review
     * Thủ thư đánh dấu đã xem
     */
    @PutMapping("/{id}/review")
    public ResponseEntity<FeedbackDTO> markReviewed(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(feedbackService.markReviewed(id, librarianId));
    }

    /**
     * PUT /slib/feedbacks/batch/review
     * Thủ thư đánh dấu nhiều phản hồi là đã xem
     */
    @PutMapping("/batch/review")
    public ResponseEntity<Map<String, Integer>> markReviewedBatch(
            @Valid @RequestBody UuidBatchRequest body,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = getCurrentUserId(userDetails);
        int reviewed = feedbackService.markReviewedBatch(body.getIds(), librarianId);
        return ResponseEntity.ok(Map.of("reviewed", reviewed));
    }

    /**
     * GET /slib/feedbacks/count
     * Đếm phản hồi
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount() {
        return ResponseEntity.ok(Map.of(
                "total", feedbackService.countAll(),
                "new", feedbackService.countByStatus(FeedbackStatus.NEW),
                "reviewed", feedbackService.countByStatus(FeedbackStatus.REVIEWED),
                "acted", feedbackService.countByStatus(FeedbackStatus.ACTED)));
    }

    /**
     * DELETE /slib/feedbacks/batch
     * Thủ thư xoá nhiều phản hồi cùng lúc
     */
    @DeleteMapping("/batch")
    public ResponseEntity<?> deleteBatch(@Valid @RequestBody UuidBatchRequest body) {
        try {
            List<UUID> uuids = body.getIds();
            feedbackService.deleteBatch(uuids);
            return ResponseEntity.ok(Map.of("deleted", uuids.size()));
        } catch (Exception e) {
            log.error("[Feedback] Error deleting batch: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
