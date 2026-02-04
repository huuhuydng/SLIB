package slib.com.example.entity.chat;

/**
 * Trạng thái của cuộc hội thoại trong quy trình AI-to-Human Escalation
 */
public enum ConversationStatus {
    AI_HANDLING, // AI đang xử lý tự động
    QUEUE_WAITING, // Đang chờ thủ thư tiếp nhận
    HUMAN_CHATTING, // Thủ thư đang chat trực tiếp
    RESOLVED // Cuộc hội thoại đã kết thúc
}
