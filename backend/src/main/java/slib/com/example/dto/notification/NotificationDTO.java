package slib.com.example.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private UUID id;
    private UUID userId;
    private String title;
    private String content;
    private String notificationType;
    private String category;
    private String categoryLabel;
    private String referenceType;
    private UUID referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
