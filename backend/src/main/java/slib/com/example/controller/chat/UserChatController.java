package slib.com.example.controller.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

// 👇 Import các DTO và Entity cần thiết
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

    // 👇 [SỬA] 5. API Lấy số lượng tin chưa đọc
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        UUID myId = getCurrentUserId(userDetails);

        // ❌ LỖI CŨ: messageRepository.countUnreadMessages(myId);
        long count = chatService.getUnreadCount(myId);

        return ResponseEntity.ok(count);
    }

    // 👇 [SỬA] 6. API Đánh dấu đã đọc
    @PostMapping("/mark-read")
    public ResponseEntity<Void> markAsRead(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID myId = getCurrentUserId(userDetails);
        UUID partnerId = UUID.fromString(payload.get("senderId"));

        // 1. Cập nhật Database (is_read = true)
        chatService.markMessagesAsRead(myId, partnerId);

        // 2. 👇 GỬI THÔNG BÁO "SEEN" QUA WEBSOCKET
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