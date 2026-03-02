package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import slib.com.example.entity.complaint.ComplaintEntity.ComplaintStatus;
import slib.com.example.entity.feedback.FeedbackEntity.FeedbackStatus;
import slib.com.example.entity.feedback.SeatViolationReportEntity.ReportStatus;
import slib.com.example.entity.support.SupportRequestStatus;
import slib.com.example.repository.ComplaintRepository;
import slib.com.example.repository.FeedbackRepository;
import slib.com.example.repository.SeatViolationReportRepository;
import slib.com.example.repository.chat.ConversationRepository;
import slib.com.example.repository.support.SupportRequestRepository;
import slib.com.example.entity.chat.ConversationStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service tổng hợp pending counts cho thủ thư
 * và broadcast qua WebSocket khi có thay đổi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LibrarianNotificationService {

    private final SupportRequestRepository supportRequestRepository;
    private final ComplaintRepository complaintRepository;
    private final FeedbackRepository feedbackRepository;
    private final ConversationRepository conversationRepository;
    private final SeatViolationReportRepository violationReportRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final slib.com.example.repository.chat.MessageRepository messageRepository;

    /**
     * Lấy tổng hợp pending counts
     */
    public Map<String, Object> getPendingCounts() {
        long supportRequests = supportRequestRepository.countByStatus(SupportRequestStatus.PENDING);
        long complaints = complaintRepository.countByStatus(ComplaintStatus.PENDING);
        long feedbacks = feedbackRepository.countByStatus(FeedbackStatus.NEW);
        long chats = conversationRepository.countByStatus(ConversationStatus.QUEUE_WAITING);
        long violations = violationReportRepository.countByStatus(ReportStatus.PENDING);

        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("supportRequests", supportRequests);
        counts.put("complaints", complaints);
        counts.put("feedbacks", feedbacks);
        counts.put("chats", chats);
        counts.put("violations", violations);
        counts.put("total", supportRequests + complaints + feedbacks + chats + violations);
        return counts;
    }

    /**
     * Đếm tin nhắn chưa đọc từ student cho một librarian cụ thể
     */
    public long getUnreadChatCount(java.util.UUID librarianId) {
        return messageRepository.countUnreadStudentMessagesForLibrarian(librarianId);
    }

    /**
     * Broadcast pending counts mới nhất tới tất cả thủ thư qua WebSocket
     * Gọi sau khi có thay đổi (tạo mới, xử lý, v.v.)
     */
    public void broadcastPendingCounts(String source, String action) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "PENDING_COUNTS_UPDATE");
            payload.put("source", source);
            payload.put("action", action);
            payload.put("counts", getPendingCounts());
            payload.put("timestamp", Instant.now().toString());

            messagingTemplate.convertAndSend("/topic/librarian-notifications", payload);
            log.debug("[LibrarianNotification] Broadcast pending counts - source: {}, action: {}", source, action);
        } catch (Exception e) {
            log.warn("[LibrarianNotification] Failed to broadcast: {}", e.getMessage());
        }
    }
}
