package slib.com.example.controller.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import slib.com.example.entity.ai.ChatMessageEntity;
import slib.com.example.entity.ai.ChatSessionEntity;
import slib.com.example.entity.users.User;
import slib.com.example.service.ai.ChatService;

import java.util.List;
import java.util.Map;

/**
 * Chat endpoints for students and librarians
 */
@RestController
@RequestMapping("/slib/ai")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Value("${ai.service.url:http://slib-ai-service:8001}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // ========================================
    // PUBLIC PROXY ENDPOINT (No Auth Required)
    // ========================================

    /**
     * Proxy request to AI Service - for mobile app without auth
     */
    @PostMapping("/proxy-chat")
    public ResponseEntity<?> proxyChat(@RequestBody Map<String, Object> request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    aiServiceUrl + "/api/ai/chat",
                    HttpMethod.POST,
                    entity,
                    String.class);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "success", false,
                            "reply", "Không thể kết nối đến AI Service: " + e.getMessage()));
        }
    }

    // ========================================
    // STUDENT (Mobile App) ENDPOINTS
    // ========================================

    /**
     * Send message to AI
     */
    @PostMapping("/chat/message")
    public ResponseEntity<?> sendMessage(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> request) {
        try {
            Long sessionId = request.get("sessionId") != null
                    ? Long.valueOf(request.get("sessionId").toString())
                    : null;
            String message = (String) request.get("message");

            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Tin nhắn không được để trống"));
            }

            ChatService.ChatResponse response = chatService.sendMessage(user, sessionId, message);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sessionId", response.getSessionId(),
                    "reply", response.getContent(),
                    "needsLibrarian", response.isNeedsLibrarian(),
                    "sessionStatus", response.getSessionStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi gửi tin nhắn: " + e.getMessage()));
        }
    }

    /**
     * Get user's chat sessions
     */
    @GetMapping("/chat/sessions")
    public ResponseEntity<List<ChatSessionEntity>> getUserSessions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getUserSessions(user.getId()));
    }

    /**
     * Get session detail with messages
     */
    @GetMapping("/chat/session/{sessionId}")
    public ResponseEntity<?> getSessionDetail(@PathVariable Long sessionId) {
        try {
            return ResponseEntity.ok(chatService.getSessionDetail(sessionId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    /**
     * Close chat session
     */
    @PostMapping("/chat/session/{sessionId}/close")
    public ResponseEntity<?> closeSession(@PathVariable Long sessionId) {
        try {
            ChatSessionEntity session = chatService.closeSession(sessionId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã đóng phiên chat",
                    "session", session));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    // ========================================
    // LIBRARIAN (Admin Portal) ENDPOINTS
    // ========================================

    /**
     * Get escalated sessions (needs librarian)
     */
    @GetMapping("/admin/escalated")
    public ResponseEntity<List<ChatSessionEntity>> getEscalatedSessions() {
        return ResponseEntity.ok(chatService.getEscalatedSessions());
    }

    /**
     * Librarian reply to a session
     */
    @PostMapping("/admin/reply/{sessionId}")
    public ResponseEntity<?> librarianReply(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Nội dung phản hồi không được để trống"));
            }

            ChatMessageEntity msg = chatService.librarianReply(sessionId, content);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã gửi phản hồi",
                    "data", msg));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi gửi phản hồi: " + e.getMessage()));
        }
    }

    /**
     * Resolve escalated session (mark as handled)
     */
    @PutMapping("/admin/resolve/{sessionId}")
    public ResponseEntity<?> resolveSession(@PathVariable Long sessionId) {
        try {
            ChatSessionEntity session = chatService.resolveSession(sessionId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã xử lý xong phiên chat",
                    "session", session));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Get session detail for librarian review
     */
    @GetMapping("/admin/session/{sessionId}")
    public ResponseEntity<?> getSessionForAdmin(@PathVariable Long sessionId) {
        try {
            return ResponseEntity.ok(chatService.getSessionDetail(sessionId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }
}
