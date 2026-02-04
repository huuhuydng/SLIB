package slib.com.example.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import slib.com.example.entity.chat.ConversationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationDTO {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private UUID librarianId;
    private String librarianName;
    private ConversationStatus status;
    private String escalationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime escalatedAt;
    private ChatMessageDTO lastMessage;
    private long unreadCount;
}
