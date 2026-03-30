package slib.com.example.entity.chat;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import slib.com.example.entity.users.User;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType type;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Helper method để code logic check nhanh các validate
    public boolean hasAttachment() {
        return this.attachmentUrl != null && !this.attachmentUrl.isEmpty();
    }

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    // Quan hệ với Conversation (optional - để backward compatible)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    // Loại người gửi: STUDENT, AI, LIBRARIAN
    @Column(name = "sender_type")
    private String senderType;

    // Human session ID: NULL = bot conversation, INTEGER = librarian round number
    @Column(name = "human_session_id")
    private Integer humanSessionId;
}