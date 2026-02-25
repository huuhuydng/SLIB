package slib.com.example.controller.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

// Import các DTO và Entity cần thiết
import slib.com.example.dto.chat.ChatMessageDTO;
import slib.com.example.dto.chat.ChatPartnerDTO;
import slib.com.example.dto.chat.ConversationDTO;
import slib.com.example.entity.chat.Message;

import slib.com.example.service.chat.UserChatService;
import slib.com.example.service.chat.ConversationService;
import slib.com.example.service.UserService;

import java.util.List;
import java.util.Map; // Nhớ import Map
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/slib/chat")
@RequiredArgsConstructor
public class UserChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserChatService chatService;
    private final ConversationService conversationService;
    private final UserService userService;

    // ==========================================
    // PHẦN 1: WEBSOCKET
    // ==========================================
    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessageDTO chatMessageDto) {
        // Lưu DB
        ChatMessageDTO savedMsg = chatService.saveMessage(chatMessageDto);

        // Gửi cho người nhận
        messagingTemplate.convertAndSend(
                "/topic/chat/" + savedMsg.getReceiverId(),
                savedMsg);

        // Gửi lại cho người gửi (để đồng bộ realtime trên các tab khác của họ)
        messagingTemplate.convertAndSend(
                "/topic/chat/" + savedMsg.getSenderId(),
                savedMsg);
    }

    // ==========================================
    // PHẦN 2: REST API
    // ==========================================

    // 1. Lấy lịch sử tin nhắn
    @GetMapping("/history/{otherUserId}")
    public ResponseEntity<org.springframework.data.domain.Page<ChatMessageDTO>> getChatHistory(
            @PathVariable UUID otherUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID currentUserId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(chatService.getChatHistory(currentUserId, otherUserId, page, size));
    }

    // 2. Lấy danh sách người đã chat (Sidebar)
    @GetMapping("/conversations")
    public ResponseEntity<List<ChatPartnerDTO>> getConversations(@AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(chatService.getConversations(currentUserId));
    }

    // 3. API TÌM KIẾM
    @GetMapping("/search")
    public ResponseEntity<List<ChatMessageDTO>> searchInConversation(
            @RequestParam UUID partnerId,
            @RequestParam String keyword,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID myId = getCurrentUserId(userDetails);

        // Gọi Service
        List<Message> messages = chatService.searchConversation(myId, partnerId, keyword);

        // Convert sang DTO để trả về
        List<ChatMessageDTO> dtos = messages.stream().map(msg -> {
            ChatMessageDTO dto = new ChatMessageDTO();
            dto.setId(msg.getId());
            dto.setSenderId(msg.getSender().getId());
            dto.setReceiverId(msg.getReceiver().getId());
            dto.setContent(msg.getContent());
            dto.setAttachmentUrl(msg.getAttachmentUrl());
            dto.setType(msg.getType());
            dto.setCreatedAt(msg.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // 4. API TÌM TRANG (JUMP TO MESSAGE)
    @GetMapping("/find-page")
    public ResponseEntity<Integer> findMessagePage(
            @RequestParam UUID partnerId,
            @RequestParam UUID messageId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID myId = getCurrentUserId(userDetails);
        int pageIndex = chatService.getPageNumberOfMessage(myId, partnerId, messageId);

        return ResponseEntity.ok(pageIndex);
    }

    // [FIX] 5. API Lấy số lượng tin chưa đọc
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        UUID myId = getCurrentUserId(userDetails);

        // BUG FIX: messageRepository.countUnreadMessages(myId);
        long count = chatService.getUnreadCount(myId);

        return ResponseEntity.ok(count);
    }

    // [FIX] 6. API Đánh dấu đã đọc
    @PostMapping("/mark-read")
    public ResponseEntity<Void> markAsRead(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID myId = getCurrentUserId(userDetails);
        UUID partnerId = UUID.fromString(payload.get("senderId"));

        // 1. Cập nhật Database (is_read = true)
        chatService.markMessagesAsRead(myId, partnerId);

        // 2. GỬI THÔNG BÁO "SEEN" QUA WEBSOCKET
        messagingTemplate.convertAndSend(
                "/topic/chat/seen/" + partnerId,
                Map.of("partnerId", myId));

        return ResponseEntity.ok().build();
    }

    // 7. API LẤY KHO LƯU TRỮ (MEDIA & FILES)
    @GetMapping("/media/{partnerId}")
    public ResponseEntity<List<ChatMessageDTO>> getMedia(
            @PathVariable UUID partnerId,
            @RequestParam String type, // 'IMAGE' hoặc 'FILE'
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID myId = getCurrentUserId(userDetails);

        // Gọi Service đã có của bạn
        List<ChatMessageDTO> mediaList = chatService.getConversationMedia(myId, partnerId, type);

        return ResponseEntity.ok(mediaList);
    }

    // ==========================================
    // CONVERSATION MANAGEMENT (AI-to-Human Escalation)
    // ==========================================

    // 8. Lấy danh sách conversation đang chờ xử lý (QUEUE_WAITING)
    @GetMapping("/conversations/waiting")
    public ResponseEntity<List<ConversationDTO>> getWaitingConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        // Chỉ Librarian mới có quyền xem
        getCurrentUserId(userDetails);
        return ResponseEntity.ok(conversationService.getWaitingConversations());
    }

    // 9. Lấy danh sách conversation đang chat (HUMAN_CHATTING) của librarian
    @GetMapping("/conversations/active")
    public ResponseEntity<List<ConversationDTO>> getActiveConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID myId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(conversationService.getActiveConversations(myId));
    }

    // 10. Lấy tất cả conversations (waiting + active)
    @GetMapping("/conversations/all")
    public ResponseEntity<List<ConversationDTO>> getAllConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID myId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(conversationService.getAllConversationsForLibrarian(myId));
    }

    // 11. Librarian tiếp nhận conversation
    @PostMapping("/conversations/{conversationId}/take-over")
    public ResponseEntity<ConversationDTO> takeOverConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = getCurrentUserId(userDetails);
        ConversationDTO result = conversationService.takeOverConversation(conversationId, librarianId);
        return ResponseEntity.ok(result);
    }

    // 12. Đánh dấu conversation đã hoàn thành
    @PostMapping("/conversations/{conversationId}/resolve")
    public ResponseEntity<ConversationDTO> resolveConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        getCurrentUserId(userDetails);
        ConversationDTO result = conversationService.resolveConversation(conversationId);
        return ResponseEntity.ok(result);
    }

    // 13. Đếm số conversation đang chờ
    @GetMapping("/conversations/waiting/count")
    public ResponseEntity<Long> countWaitingConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        getCurrentUserId(userDetails);
        return ResponseEntity.ok(conversationService.countWaitingConversations());
    }

    // 14. Lấy vị trí trong hàng đợi cho một conversation
    @GetMapping("/conversations/{conversationId}/queue-position")
    public ResponseEntity<Map<String, Object>> getQueuePosition(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        getCurrentUserId(userDetails);
        int position = conversationService.getQueuePosition(conversationId);
        long totalWaiting = conversationService.countWaitingConversations();
        return ResponseEntity.ok(Map.of(
                "position", position,
                "totalWaiting", totalWaiting,
                "conversationId", conversationId));
    }

    // 15. Hủy chờ trong queue - quay lại AI handling
    @PostMapping("/conversations/{conversationId}/cancel-escalation")
    public ResponseEntity<ConversationDTO> cancelEscalation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        getCurrentUserId(userDetails);
        ConversationDTO result = conversationService.cancelEscalation(conversationId);
        return ResponseEntity.ok(result);
    }

    // 16. User yêu cầu gặp thủ thư (tạo conversation mới và escalate)
    @PostMapping("/conversations/request-librarian")
    public ResponseEntity<Map<String, Object>> requestLibrarian(
            @RequestBody(required = false) Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        String reason = request != null ? (String) request.get("reason") : "User yêu cầu gặp thủ thư";

        // Lấy AI session ID để backend có thể đọc chat history từ MongoDB
        String aiSessionId = request != null ? (String) request.get("aiSessionId") : null;

        // Tạo conversation mới và escalate với aiSessionId
        ConversationDTO conversation = conversationService.createAndEscalateWithHistory(
                userId, reason, null, aiSessionId);
        int queuePosition = conversationService.getQueuePosition(conversation.getId());
        long totalWaiting = conversationService.countWaitingConversations();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "conversationId", conversation.getId(),
                "queuePosition", queuePosition,
                "totalWaiting", totalWaiting));
    }

    // 17. Lấy tất cả messages của conversation
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getConversationMessages(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        getCurrentUserId(userDetails);
        List<ChatMessageDTO> messages = conversationService.getConversationMessages(conversationId);
        return ResponseEntity.ok(messages);
    }

    // 18. Gửi tin nhắn mới vào conversation
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @PathVariable UUID conversationId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID senderId = getCurrentUserId(userDetails);
        String content = request.get("content");
        String senderType = request.getOrDefault("senderType", "STUDENT"); // STUDENT, LIBRARIAN, AI

        ChatMessageDTO message = conversationService.addMessageToConversation(
                conversationId, senderId, content, senderType);
        return ResponseEntity.ok(message);
    }

    // 19. Lay conversation status (cho mobile polling)
    @GetMapping("/conversations/{conversationId}/status")
    public ResponseEntity<Map<String, Object>> getConversationStatus(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        getCurrentUserId(userDetails);
        ConversationDTO conv = conversationService.getConversationById(conversationId)
                .map(c -> conversationService.convertToDTO(c))
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        int queuePosition = conversationService.getQueuePosition(conversationId);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", conv.getId());
        response.put("status", conv.getStatus());
        response.put("librarianName", conv.getLibrarianName() != null ? conv.getLibrarianName() : "");
        response.put("queuePosition", queuePosition);
        return ResponseEntity.ok(response);
    }

    // 20. Student check active conversation (HUMAN_CHATTING hoặc QUEUE_WAITING)
    @GetMapping("/conversations/my-active")
    public ResponseEntity<Map<String, Object>> getMyActiveConversation(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID studentId = getCurrentUserId(userDetails);
        var activeConv = conversationService.getActiveConversationForStudent(studentId);
        if (activeConv != null) {
            return ResponseEntity.ok(Map.of(
                    "hasActive", true,
                    "conversationId", activeConv.getId().toString(),
                    "status", activeConv.getStatus().toString(),
                    "librarianName", activeConv.getLibrarianName() != null ? activeConv.getLibrarianName() : ""));
        }
        return ResponseEntity.ok(Map.of("hasActive", false));
    }

    // 21. Student hủy chờ queue (cancel queue)
    @PostMapping("/conversations/{conversationId}/cancel-queue")
    public ResponseEntity<Map<String, Object>> cancelQueue(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID studentId = getCurrentUserId(userDetails);
        conversationService.cancelQueue(conversationId, studentId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã hủy chờ"));
    }

    // ==========================================
    // HELPER
    // ==========================================
    private UUID getCurrentUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Vui lòng đăng nhập để sử dụng tính năng chat!");
        }
        String email = userDetails.getUsername();
        return userService.getUserByEmail(email).getId();
    }
}