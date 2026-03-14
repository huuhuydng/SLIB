package slib.com.example.service.feedback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.service.notification.LibrarianNotificationService;
import slib.com.example.dto.feedback.FeedbackDTO;
import slib.com.example.entity.feedback.FeedbackEntity;
import slib.com.example.entity.feedback.FeedbackEntity.FeedbackStatus;
import slib.com.example.entity.users.User;
import slib.com.example.repository.feedback.FeedbackRepository;
import slib.com.example.repository.users.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final LibrarianNotificationService librarianNotificationService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Lấy tất cả phản hồi (cho thủ thư)
     */
    @Transactional(readOnly = true)
    public List<FeedbackDTO> getAll() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(FeedbackDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy phản hồi theo trạng thái
     */
    @Transactional(readOnly = true)
    public List<FeedbackDTO> getByStatus(FeedbackStatus status) {
        return feedbackRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(FeedbackDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy phản hồi của sinh viên
     */
    @Transactional(readOnly = true)
    public List<FeedbackDTO> getByStudent(UUID studentId) {
        return feedbackRepository.findByUserIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(FeedbackDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Sinh viên tạo phản hồi
     */
    @Transactional
    public FeedbackDTO create(UUID studentId, Integer rating, String content) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên"));

        FeedbackEntity feedback = FeedbackEntity.builder()
                .user(student)
                .rating(rating)
                .content(content)
                .status(FeedbackStatus.NEW)
                .build();

        FeedbackEntity saved = feedbackRepository.save(feedback);
        log.info("[Feedback] Sinh viên {} đã gửi phản hồi - rating: {}", student.getFullName(), rating);
        broadcastDashboardUpdate("FEEDBACK_UPDATE", "CREATED");
        librarianNotificationService.broadcastPendingCounts("FEEDBACK", "CREATED");
        return FeedbackDTO.fromEntity(saved);
    }

    /**
     * Thủ thư đánh dấu đã xem
     */
    @Transactional
    public FeedbackDTO markReviewed(UUID feedbackId, UUID librarianId) {
        FeedbackEntity feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phản hồi"));

        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thủ thư"));

        feedback.setStatus(FeedbackStatus.REVIEWED);
        feedback.setReviewedBy(librarian);
        feedback.setReviewedAt(LocalDateTime.now());

        FeedbackEntity saved = feedbackRepository.save(feedback);
        log.info("[Feedback] Phản hồi {} đã được xem bởi {}", feedbackId, librarian.getFullName());
        broadcastDashboardUpdate("FEEDBACK_UPDATE", "REVIEWED");
        librarianNotificationService.broadcastPendingCounts("FEEDBACK", "REVIEWED");
        return FeedbackDTO.fromEntity(saved);
    }

    /**
     * Đếm phản hồi theo trạng thái
     */
    public long countByStatus(FeedbackStatus status) {
        return feedbackRepository.countByStatus(status);
    }

    /**
     * Đếm tổng phản hồi
     */
    public long countAll() {
        return feedbackRepository.count();
    }

    /**
     * Xoá nhiều phản hồi cùng lúc
     */
    @Transactional
    public void deleteBatch(List<UUID> ids) {
        feedbackRepository.deleteAllById(ids);
        log.info("[Feedback] Deleted {} feedbacks", ids.size());
        broadcastDashboardUpdate("FEEDBACK_UPDATE", "DELETED");
        librarianNotificationService.broadcastPendingCounts("FEEDBACK", "DELETED");
    }

    private void broadcastDashboardUpdate(String type, String action) {
        try {
            messagingTemplate.convertAndSend("/topic/dashboard",
                    java.util.Map.of("type", type, "action", action, "timestamp", java.time.Instant.now().toString()));
        } catch (Exception e) {
            log.warn("Failed to broadcast dashboard update: {}", e.getMessage());
        }
    }
}
