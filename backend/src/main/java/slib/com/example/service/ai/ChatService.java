package slib.com.example.service.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.ai.ChatMessageEntity;
import slib.com.example.entity.ai.ChatSessionEntity;
import slib.com.example.entity.users.User;
import slib.com.example.repository.ai.ChatMessageRepository;
import slib.com.example.repository.ai.ChatSessionRepository;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing chat with AI and librarian intervention
 */
@Service
public class ChatService {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private GeminiService geminiService;

    /**
     * Send message and get AI response
     */
    @Transactional
    public ChatResponse sendMessage(User user, Long sessionId, String userMessage) {
        // Get or create session
        ChatSessionEntity session;
        if (sessionId != null) {
            session = chatSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        } else {
            session = createNewSession(user, userMessage);
        }

        // Check if session is closed
        if (session.getStatus() == ChatSessionEntity.SessionStatus.CLOSED) {
            throw new RuntimeException("Session is already closed");
        }

        // Save user message
        ChatMessageEntity userMsg = ChatMessageEntity.builder()
                .session(session)
                .role(ChatMessageEntity.MessageRole.USER)
                .content(userMessage)
                .build();
        chatMessageRepository.save(userMsg);

        // Build chat history for context
        List<Map<String, String>> chatHistory = buildChatHistory(session.getId());

        // Get AI response
        GeminiService.GeminiResponse aiResponse = geminiService.generateResponse(userMessage, chatHistory);

        // Save AI response
        ChatMessageEntity aiMsg = ChatMessageEntity.builder()
                .session(session)
                .role(ChatMessageEntity.MessageRole.AI)
                .content(aiResponse.getContent())
                .confidenceScore(aiResponse.getConfidenceScore())
                .needsReview(aiResponse.isNeedsReview())
                .build();
        chatMessageRepository.save(aiMsg);

        // Escalate session if AI is uncertain
        if (aiResponse.isNeedsReview()) {
            session.setStatus(ChatSessionEntity.SessionStatus.ESCALATED);
            chatSessionRepository.save(session);
        }

        return new ChatResponse(
                session.getId(),
                aiResponse.getContent(),
                aiResponse.isNeedsReview(),
                session.getStatus().name());
    }

    /**
     * Create new chat session
     */
    @Transactional
    public ChatSessionEntity createNewSession(User user, String firstMessage) {
        String title = firstMessage.length() > 50
                ? firstMessage.substring(0, 50) + "..."
                : firstMessage;

        ChatSessionEntity session = ChatSessionEntity.builder()
                .user(user)
                .title(title)
                .status(ChatSessionEntity.SessionStatus.ACTIVE)
                .build();

        return chatSessionRepository.save(session);
    }

    /**
     * Get user's chat sessions
     */
    public List<ChatSessionEntity> getUserSessions(UUID userId) {
        return chatSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get session with messages
     */
    public Map<String, Object> getSessionDetail(Long sessionId) {
        ChatSessionEntity session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        List<ChatMessageEntity> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        Map<String, Object> result = new HashMap<>();
        result.put("session", session);
        result.put("messages", messages);
        return result;
    }

    /**
     * Get escalated sessions for librarian
     */
    public List<ChatSessionEntity> getEscalatedSessions() {
        return chatSessionRepository.findEscalatedSessions();
    }

    /**
     * Librarian reply to message
     */
    @Transactional
    public ChatMessageEntity librarianReply(Long sessionId, String content) {
        ChatSessionEntity session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        ChatMessageEntity msg = ChatMessageEntity.builder()
                .session(session)
                .role(ChatMessageEntity.MessageRole.LIBRARIAN)
                .content(content)
                .needsReview(false)
                .build();

        return chatMessageRepository.save(msg);
    }

    /**
     * Resolve escalated session
     */
    @Transactional
    public ChatSessionEntity resolveSession(Long sessionId) {
        ChatSessionEntity session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        session.setStatus(ChatSessionEntity.SessionStatus.ACTIVE);
        return chatSessionRepository.save(session);
    }

    /**
     * Close session
     */
    @Transactional
    public ChatSessionEntity closeSession(Long sessionId) {
        ChatSessionEntity session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        session.setStatus(ChatSessionEntity.SessionStatus.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        return chatSessionRepository.save(session);
    }

    private List<Map<String, String>> buildChatHistory(Long sessionId) {
        List<ChatMessageEntity> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<Map<String, String>> history = new ArrayList<>();

        // Get last 10 messages for context
        int start = Math.max(0, messages.size() - 10);
        for (int i = start; i < messages.size(); i++) {
            ChatMessageEntity msg = messages.get(i);
            Map<String, String> entry = new HashMap<>();
            entry.put("role", msg.getRole() == ChatMessageEntity.MessageRole.USER ? "user" : "assistant");
            entry.put("content", msg.getContent());
            history.add(entry);
        }

        return history;
    }

    /**
     * Response wrapper
     */
    public static class ChatResponse {
        private final Long sessionId;
        private final String content;
        private final boolean needsLibrarian;
        private final String sessionStatus;

        public ChatResponse(Long sessionId, String content, boolean needsLibrarian, String sessionStatus) {
            this.sessionId = sessionId;
            this.content = content;
            this.needsLibrarian = needsLibrarian;
            this.sessionStatus = sessionStatus;
        }

        public Long getSessionId() {
            return sessionId;
        }

        public String getContent() {
            return content;
        }

        public boolean isNeedsLibrarian() {
            return needsLibrarian;
        }

        public String getSessionStatus() {
            return sessionStatus;
        }
    }
}
