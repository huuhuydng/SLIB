package slib.com.example.dto.ai;

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
public class ChatSessionDTO {
    private Long id;
    private UUID userId;
    private String userName;
    private String status;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}
