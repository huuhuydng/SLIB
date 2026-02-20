package slib.com.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.feedback.FeedbackDTO;
import slib.com.example.entity.feedback.FeedbackEntity.FeedbackStatus;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.FeedbackService;

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
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID studentId = getCurrentUserId(userDetails);
        Integer rating = (Integer) body.get("rating");
        String content = (String) body.get("content");

        if (rating == null || rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().build();
        }

        FeedbackDTO result = feedbackService.create(studentId, rating, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
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
}
