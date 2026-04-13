package slib.com.example.service.feedback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.chat.Conversation;
import slib.com.example.service.notification.LibrarianNotificationService;
import slib.com.example.dto.feedback.FeedbackDTO;
import slib.com.example.entity.feedback.FeedbackEntity;
import slib.com.example.entity.feedback.FeedbackEntity.FeedbackStatus;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.users.User;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.exception.BadRequestException;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.chat.ConversationRepository;
import slib.com.example.repository.feedback.FeedbackRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.notification.PushNotificationService;
import slib.com.example.util.ContentValidationUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final ReservationRepository reservationRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final LibrarianNotificationService librarianNotificationService;
    private final PushNotificationService pushNotificationService;
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
        return create(studentId, rating, content, null, null, null);
    }

    /**
     * Sinh viên tạo phản hồi
     */
    @Transactional
    public FeedbackDTO create(UUID studentId, Integer rating, String content, String category, String conversationId, UUID reservationId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên"));
        validateFeedbackContext(studentId, conversationId, reservationId);
        String normalizedContent = ContentValidationUtil.normalizeOptionalText(content, "Nội dung phản hồi", 2000);
        String normalizedCategory = ContentValidationUtil.normalizeOptionalFeedbackCategory(category);

        String resolvedCategory = normalizedCategory;
        if (resolvedCategory == null || resolvedCategory.isBlank()) {
            if (conversationId != null && !conversationId.isBlank()) {
                resolvedCategory = "MESSAGE";
            } else if (reservationId != null) {
                resolvedCategory = "GENERAL";
            }
        }

        FeedbackEntity feedback = FeedbackEntity.builder()
                .user(student)
                .rating(rating)
                .content(normalizedContent)
                .category(resolvedCategory)
                .conversationId(conversationId)
                .reservationId(reservationId)
                .status(FeedbackStatus.NEW)
                .build();

        FeedbackEntity saved = feedbackRepository.save(feedback);
        log.info("[Feedback] Sinh viên {} đã gửi phản hồi - rating: {}", student.getFullName(), rating);

        boolean isMessageFeedback = "MESSAGE".equalsIgnoreCase(resolvedCategory);
        String staffTitle = isMessageFeedback
                ? "Đánh giá mới cho phiên hỗ trợ"
                : "Phản hồi mới từ sinh viên";
        String staffBody = isMessageFeedback
                ? student.getFullName() + " vừa gửi đánh giá mới cho cuộc trò chuyện hỗ trợ."
                : student.getFullName() + " vừa gửi phản hồi mới sau khi sử dụng chỗ ngồi.";

        pushNotificationService.sendToStaff(
                staffTitle,
                staffBody,
                NotificationType.SYSTEM,
                saved.getId(),
                "FEEDBACK",
                resolvedCategory);
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

        if (feedback.getStatus() != FeedbackStatus.NEW) {
            return FeedbackDTO.fromEntity(feedback);
        }

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
     * Thủ thư đánh dấu nhiều phản hồi là đã xem
     */
    @Transactional
    public int markReviewedBatch(List<UUID> feedbackIds, UUID librarianId) {
        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thủ thư"));

        List<FeedbackEntity> feedbacks = feedbackRepository.findAllById(feedbackIds);
        LocalDateTime reviewedAt = LocalDateTime.now();

        List<FeedbackEntity> reviewableFeedbacks = feedbacks.stream()
                .filter(feedback -> feedback.getStatus() == FeedbackStatus.NEW)
                .peek(feedback -> {
                    feedback.setStatus(FeedbackStatus.REVIEWED);
                    feedback.setReviewedBy(librarian);
                    feedback.setReviewedAt(reviewedAt);
                })
                .collect(Collectors.toList());

        if (reviewableFeedbacks.isEmpty()) {
            return 0;
        }

        feedbackRepository.saveAll(reviewableFeedbacks);
        log.info("[Feedback] {} phản hồi đã được xem bởi {}", reviewableFeedbacks.size(), librarian.getFullName());
        broadcastDashboardUpdate("FEEDBACK_UPDATE", "REVIEWED");
        librarianNotificationService.broadcastPendingCounts("FEEDBACK", "REVIEWED");
        return reviewableFeedbacks.size();
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

    @Transactional(readOnly = true)
    public Map<String, Object> checkPendingFeedback(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        // Cho phép hiện feedback ngay sau khi sinh viên rời ghế sớm:
        // actualEndTime sẽ được ưu tiên nếu có, ngược lại dùng endTime kế hoạch.
        List<ReservationEntity> eligibleReservations = reservationRepository
                .findCompletedReservationsEligibleForFeedback(
                        userId,
                        now,
                        List.of("COMPLETED"));

        if (eligibleReservations.isEmpty()) {
            return Map.of("hasPending", false);
        }

        ReservationEntity reservation = eligibleReservations.stream()
                .filter(item -> !feedbackRepository.existsByReservationId(item.getReservationId()))
                .findFirst()
                .orElse(null);

        if (reservation == null) {
            return Map.of("hasPending", false);
        }

        Map<String, Object> result = new HashMap<>();
        LocalDateTime effectiveEndTime = reservation.getActualEndTime() != null
                ? reservation.getActualEndTime()
                : reservation.getEndTime();
        result.put("hasPending", true);
        result.put("reservationId", reservation.getReservationId().toString());
        result.put("zoneName", reservation.getSeat().getZone().getZoneName());
        result.put("seatCode", reservation.getSeat().getSeatCode());
        result.put("confirmedAt", reservation.getConfirmedAt() != null ? reservation.getConfirmedAt().toString() : null);
        result.put("endedAt", effectiveEndTime.toString());
        return result;
    }

    private void broadcastDashboardUpdate(String type, String action) {
        try {
            messagingTemplate.convertAndSend("/topic/dashboard",
                    java.util.Map.of("type", type, "action", action, "timestamp", java.time.Instant.now().toString()));
        } catch (Exception e) {
            log.warn("Failed to broadcast dashboard update: {}", e.getMessage());
        }
    }

    private void validateFeedbackContext(UUID studentId, String conversationId, UUID reservationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            UUID convId;
            try {
                convId = UUID.fromString(conversationId);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("conversationId không hợp lệ.");
            }

            Conversation conversation = conversationRepository.findById(convId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cuộc trò chuyện"));
            if (conversation.getStudent() == null || !studentId.equals(conversation.getStudent().getId())) {
                throw new BadRequestException("Bạn không thể gửi phản hồi cho cuộc trò chuyện không thuộc về mình.");
            }
        }

        if (reservationId != null) {
            ReservationEntity reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lượt đặt chỗ"));
            if (reservation.getUser() == null || !studentId.equals(reservation.getUser().getId())) {
                throw new BadRequestException("Bạn không thể gửi phản hồi cho lượt đặt chỗ không thuộc về mình.");
            }

            LocalDateTime now = LocalDateTime.now();
            String status = reservation.getStatus() != null ? reservation.getStatus().toUpperCase() : "";
            LocalDateTime effectiveEndTime = reservation.getActualEndTime() != null
                    ? reservation.getActualEndTime()
                    : reservation.getEndTime();
            if (!List.of("CONFIRMED", "COMPLETED").contains(status) || effectiveEndTime == null
                    || effectiveEndTime.isAfter(now)) {
                throw new BadRequestException("Chỉ có thể gửi phản hồi cho lượt đặt chỗ đã sử dụng và đã kết thúc.");
            }
        }
    }
}
