package slib.com.example.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.time.LocalDateTime; // 👈 Nhớ import cái này

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatPartnerDTO {
    private UUID id;
    private String fullName;
    private String email; 
    
    // 👇 THÊM 2 TRƯỜNG NÀY ĐỂ FIX LỖI INFERENCE VÀ HIỂN THỊ THÔNG BÁO
    private long unreadCount; 
    private LocalDateTime latestMessageTime; 
}