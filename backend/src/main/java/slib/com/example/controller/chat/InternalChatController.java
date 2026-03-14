package slib.com.example.controller.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.chat.AIReplyRequest;
import slib.com.example.dto.chat.ChatMessageDTO;
import slib.com.example.dto.chat.ConversationDTO;
import slib.com.example.dto.chat.EscalateRequest;
import slib.com.example.entity.chat.Conversation;
import slib.com.example.entity.chat.Message;
import slib.com.example.entity.chat.MessageType;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.repository.chat.MessageRepository;
import slib.com.example.service.chat.ConversationService;

import java.util.Map;
import java.util.UUID;

/**
 * Internal API cho AI Service gọi ngược về Backend
 * Bảo vệ bằng API Key trong header X-API-Key
 */
@RestController
@RequestMapping("/internal/chat")
@RequiredArgsConstructor
@Slf4j
public class InternalChatController {

    private final ConversationService conversationService;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${slib.internal.api-key:default-internal-key}")
    private String internalApiKey;

    /**
     * AI Service gửi tin nhắn trả lời cho student
     * POST /internal/chat/reply
     */
    @PostMapping("/reply")
    public ResponseEntity<?> aiReply(
            @RequestBody AIReplyRequest request,
            @RequestHeader("X-API-Key") String apiKey) {

        // Validate API Key
        if (!validateApiKey(apiKey)) {
            log.warn("[InternalChat] Invalid API Key for /internal/chat/reply");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid API Key"));
        }

        try {
            // Tìm hoặc tạo BOT user
            User botUser = getOrCreateBotUser();

            // Tìm student
            User student = userRepository.findById(request.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Student not found: " + request.getStudentId()));

            // Lấy conversation
            Conversation conversation = null;
            if (request.getConversationId() != null) {
                conversation = conversationService.getConversationById(request.getConversationId())
                        .orElse(null);
            }

            // Tạo tin nhắn từ BOT
            Message message = Message.builder()
                    .sender(botUser)
                    .receiver(student)
                    .content(request.getContent())
                    .attachmentUrl(request.getAttachmentUrl())
                    .type(parseMessageType(request.getMessageType()))
                    .conversation(conversation)
                    .senderType("AI")
                    .build();

            Message savedMessage = messageRepository.save(message);

            // Tạo DTO để gửi qua WebSocket
            ChatMessageDTO dto = ChatMessageDTO.builder()
                    .id(savedMessage.getId())
                    .senderId(botUser.getId())
                    .receiverId(student.getId())
                    .content(savedMessage.getContent())
                    .attachmentUrl(savedMessage.getAttachmentUrl())
                    .type(savedMessage.getType())
                    .createdAt(savedMessage.getCreatedAt())
                    .senderName("AI Assistant")
                    .build();

            // Gửi WebSocket cho student
            messagingTemplate.convertAndSend("/topic/chat/" + student.getId(), dto);
            log.info("[InternalChat] AI replied to student {}: {}", student.getId(), request.getContent());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "messageId", savedMessage.getId()));

        } catch (Exception e) {
            log.error("Error processing AI reply", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * AI Service yêu cầu chuyển giao sang thủ thư
     * POST /internal/chat/escalate
     */
    @PostMapping("/escalate")
    public ResponseEntity<?> escalate(
            @RequestBody EscalateRequest request,
            @RequestHeader("X-API-Key") String apiKey) {

        // Validate API Key
        if (!validateApiKey(apiKey)) {
            log.warn("[InternalChat] Invalid API Key for /internal/chat/escalate");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid API Key"));
        }

        try {
            UUID conversationId = request.getConversationId();

            // Nếu không có conversationId, tìm theo studentId
            if (conversationId == null && request.getStudentId() != null) {
                Conversation conv = conversationService.getOrCreateConversation(request.getStudentId());
                conversationId = conv.getId();
            }

            if (conversationId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "conversationId or studentId is required"));
            }

            // Escalate conversation
            ConversationDTO dto = conversationService.escalateToHuman(conversationId, request.getReason());

            log.info("[InternalChat] AI escalated conversation {} to human. Reason: {}",
                    conversationId, request.getReason());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "conversation", dto));

        } catch (Exception e) {
            log.error("Error processing escalation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Health check cho AI Service
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "internal-chat-api"));
    }

    // ==================== HELPER METHODS ====================

    private boolean validateApiKey(String apiKey) {
        if (apiKey == null || internalApiKey == null) return false;
        return java.security.MessageDigest.isEqual(
                apiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                internalApiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private User getOrCreateBotUser() {
        // Tìm BOT user với email đặc biệt
        String botEmail = "ai-bot@slib.system";
        return userRepository.findByEmail(botEmail)
                .orElseGet(() -> {
                    // Tạo BOT user nếu chưa có
                    User bot = User.builder()
                            .userCode("BOT-001")
                            .fullName("AI Assistant")
                            .email(botEmail)
                            .role(Role.STUDENT) // Dùng role STUDENT để đơn giản
                            .isActive(true)
                            .build();
                    return userRepository.save(bot);
                });
    }

    private MessageType parseMessageType(String type) {
        if (type == null || type.isEmpty()) {
            return MessageType.TEXT;
        }
        try {
            return MessageType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MessageType.TEXT;
        }
    }
}
