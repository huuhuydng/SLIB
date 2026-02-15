package slib.com.example.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import slib.com.example.entity.chat.MessageType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {

    private UUID id;
    private UUID senderId;
    private UUID receiverId;
    private String content;
    private String attachmentUrl;
    private MessageType type;
    private LocalDateTime createdAt;
    private Boolean isRead; // Trạng thái đã đọc hay chưa

    private String senderName;
    private String senderType; // STUDENT, AI, LIBRARIAN

    public String getThumbnailUrl() {
        if (this.attachmentUrl == null || this.attachmentUrl.isEmpty()) {
            return null;
        }
        // Web màn hình to hơn điện thoại, ta resize khoảng 500px là đẹp
        // c_limit: Giới hạn chiều rộng, giữ nguyên tỉ lệ ảnh
        return this.attachmentUrl.replace("/upload/", "/upload/w_500,c_limit,q_auto/");
    }
}