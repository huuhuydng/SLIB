package slib.com.example.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request từ AI Service khi gửi tin nhắn trả lời cho student
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AIReplyRequest {
    private UUID conversationId;
    private UUID studentId;
    private String content;
    private String attachmentUrl;
    private String messageType; // TEXT, IMAGE, FILE
}
