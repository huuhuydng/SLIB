package slib.com.example.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request từ AI Service khi yêu cầu chuyển giao sang thủ thư
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EscalateRequest {
    private UUID conversationId;
    private UUID studentId;
    private String reason; // Lý do chuyển giao (optional)
}
