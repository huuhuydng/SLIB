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
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.users.UserService;
import slib.com.example.entity.users.Role;
import slib.com.example.exception.BadRequestException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/slib/chat")
@RequiredArgsConstructor
public class UserChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserChatService chatService;
    private final ConversationService conversationService;
    private final UserService userService;
    private final CloudinaryService cloudinaryService;

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

    // Typing indicator - client gửi tới /app/typing/{conversationId}
    @MessageMapping("/typing/{conversationId}")
    public void handleTypingIndicator(
            @org.springframework.messaging.handler.annotation.DestinationVariable UUID conversationId,
            @Payload Map<String, Object> payload) {
        // Broadcast trạng thái đang gõ tới tất cả subscriber của conversation
        Map<String, Object> typingEvent = Map.of(
                "type", "TYPING",
                "userId", payload.get("userId"),
                "isTyping", payload.getOrDefault("isTyping", true));
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId, typingEvent);
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

    // 8. Lấy danh sách conversation đang chờ xử lý (QUEUE_WAITING) - LIBRARIAN only
    @GetMapping("/conversations/waiting")
    public ResponseEntity<List<ConversationDTO>> getWaitingConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        requireLibrarianRole(userDetails);
        return ResponseEntity.ok(conversationService.getWaitingConversations());
    }

    // 9. Lấy danh sách conversation đang chat (HUMAN_CHATTING) của librarian - LIBRARIAN only
    @GetMapping("/conversations/active")
    public ResponseEntity<List<ConversationDTO>> getActiveConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID myId = requireLibrarianRole(userDetails);
        return ResponseEntity.ok(conversationService.getActiveConversations(myId));
    }

    // 10. Lấy tất cả conversations (waiting + active) - LIBRARIAN only
    @GetMapping("/conversations/all")
    public ResponseEntity<List<ConversationDTO>> getAllConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID myId = requireLibrarianRole(userDetails);
        return ResponseEntity.ok(conversationService.getAllConversationsForLibrarian(myId));
    }

    // 11. Librarian tiếp nhận conversation - LIBRARIAN only
    @PostMapping("/conversations/{conversationId}/take-over")
    public ResponseEntity<ConversationDTO> takeOverConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = requireLibrarianRole(userDetails);
        ConversationDTO result = conversationService.takeOverConversation(conversationId, librarianId);
        return ResponseEntity.ok(result);
    }

    // 12. Đánh dấu conversation đã hoàn thành - LIBRARIAN only
    @PostMapping("/conversations/{conversationId}/resolve")
    public ResponseEntity<ConversationDTO> resolveConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        requireLibrarianRole(userDetails);
        ConversationDTO result = conversationService.resolveConversation(conversationId);
        return ResponseEntity.ok(result);
    }

    // 13. Đếm số conversation đang chờ - LIBRARIAN only
    @GetMapping("/conversations/waiting/count")
    public ResponseEntity<Long> countWaitingConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        requireLibrarianRole(userDetails);
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

    // 15b. Sinh viên kết thúc cuộc trò chuyện với thủ thư
    @PostMapping("/conversations/{conversationId}/student-resolve")
    public ResponseEntity<ConversationDTO> studentResolve(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        ConversationDTO result = conversationService.studentResolveConversation(conversationId, userId);
        return ResponseEntity.ok(result);
    }

    // 15c. Đánh dấu đã đọc tin nhắn trong conversation
    @PostMapping("/conversations/{conversationId}/mark-read")
    public ResponseEntity<Map<String, Object>> markConversationAsRead(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        int updated = conversationService.markConversationAsRead(conversationId, userId);
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    // 15d. Gửi typing indicator qua REST (cho mobile app không dùng STOMP send)
    @PostMapping("/conversations/{conversationId}/typing")
    public ResponseEntity<Void> sendTypingIndicator(
            @PathVariable UUID conversationId,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        boolean isTyping = Boolean.TRUE.equals(payload.get("isTyping"));
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId,
                Map.of("type", "TYPING", "userId", userId.toString(), "isTyping", isTyping));
        return ResponseEntity.ok().build();
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

    // 17. Lấy messages của conversation (hỗ trợ phân trang)
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<?> getConversationMessages(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        conversationService.verifyConversationAccess(conversationId, userId);
        if (page != null) {
            // Paginated mode - cho mobile lazy loading
            org.springframework.data.domain.Page<ChatMessageDTO> messages =
                conversationService.getConversationMessagesPaginated(conversationId, page, size);
            return ResponseEntity.ok(messages);
        }
        // Non-paginated (backward compat cho frontend web)
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
        conversationService.verifyConversationAccess(conversationId, senderId);
        String content = request.get("content");
        String senderType = request.getOrDefault("senderType", "STUDENT"); // STUDENT, LIBRARIAN, AI

        ChatMessageDTO message = conversationService.addMessageToConversation(
                conversationId, senderId, content, senderType);
        return ResponseEntity.ok(message);
    }

    // 18b. Gửi tin nhắn kèm ảnh vào conversation
    @PostMapping("/conversations/{conversationId}/messages/with-image")
    public ResponseEntity<?> sendMessageWithImage(
            @PathVariable UUID conversationId,
            @RequestParam(value = "file") MultipartFile file,
            @RequestParam(value = "content", required = false, defaultValue = "") String content,
            @RequestParam(value = "senderType", required = false, defaultValue = "STUDENT") String senderType,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            UUID senderId = getCurrentUserId(userDetails);
            conversationService.verifyConversationAccess(conversationId, senderId);

            // Validate file size (5MB max)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "File quá lớn. Tối đa: 5MB"));
            }

            // Validate file type (images only)
            String contentType = resolveImageContentType(file);
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ hỗ trợ file ảnh (JPEG, PNG, GIF, WebP)"));
            }

            // Upload ảnh lên Cloudinary
            String imageUrl = cloudinaryService.uploadImageChat(file);

            // Ghép URL vào content theo format [IMAGES]
            String fullContent = content.isEmpty()
                    ? "[IMAGES]\n" + imageUrl
                    : content + "\n[IMAGES]\n" + imageUrl;

            ChatMessageDTO message = conversationService.addMessageToConversation(
                    conversationId, senderId, fullContent, senderType);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi gửi ảnh: " + e.getMessage()));
        }
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

    // 22. Mobile: Lấy hoặc tạo conversation cho AI chat (để lưu messages vào DB)
    @PostMapping("/conversations/get-or-create")
    public ResponseEntity<Map<String, Object>> getOrCreateConversation(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        slib.com.example.entity.chat.Conversation conv = conversationService.getOrCreateConversation(userId);
        return ResponseEntity.ok(Map.of(
                "conversationId", conv.getId().toString(),
                "status", conv.getStatus().toString()));
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

    private UUID requireLibrarianRole(UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Vui lòng đăng nhập để sử dụng tính năng chat!");
        }
        String email = userDetails.getUsername();
        var user = userService.getUserByEmail(email);
        if (user.getRole() != Role.LIBRARIAN && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Chỉ thủ thư mới có quyền thực hiện thao tác này");
        }
        return user.getId();
    }

    private String resolveImageContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            return contentType.toLowerCase(Locale.ROOT);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return null;
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);

        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "heic" -> "image/heic";
            case "heif" -> "image/heif";
            default -> null;
        };
    }

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/heic", "image/heif");
}
