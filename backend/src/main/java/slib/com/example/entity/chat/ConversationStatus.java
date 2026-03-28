package slib.com.example.entity.chat;

/**
 * Trạng thái của cuộc hội thoại trong quy trình AI-to-Human Escalation
 */
public enum ConversationStatus {
    AI_HANDLING,
    QUEUE_WAITING, 
    HUMAN_CHATTING, 
    RESOLVED 
}
