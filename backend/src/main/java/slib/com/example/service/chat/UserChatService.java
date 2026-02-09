package slib.com.example.service.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import slib.com.example.dto.chat.ChatMessageDTO;
import slib.com.example.dto.chat.ChatPartnerDTO; // Nhớ import cái này
import slib.com.example.entity.chat.Message;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;
import slib.com.example.repository.chat.MessageRepository;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.chat.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    // ========================================================================
    // 1. GỬI TIN NHẮN
    // ========================================================================
    public ChatMessageDTO saveMessage(ChatMessageDTO chatMessageDto) {
        // Tìm người gửi, nhận
        User sender = userRepository.findById(chatMessageDto.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findById(chatMessageDto.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        // Xác định senderType - ưu tiên từ DTO, fallback dựa trên role
        String senderType = chatMessageDto.getSenderType();
        if (senderType == null || senderType.isEmpty()) {
            // Fallback: xác định dựa trên role của sender
            senderType = sender.getRole() != null &&
                    sender.getRole().name().contains("LIBRARIAN") ? "LIBRARIAN" : "STUDENT";
        }

        // Tạo Entity
        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(chatMessageDto.getContent())
                .attachmentUrl(chatMessageDto.getAttachmentUrl())
                .type(chatMessageDto.getType())
                .senderType(senderType) // Quan trọng: lưu senderType để mobile biết
                .build();

        // Validate
        if (message.getContent() == null && !message.hasAttachment()) {
            throw new RuntimeException("Nội dung tin nhắn không được để trống!");
        }

        // Lưu và trả về
        Message savedMessage = messageRepository.save(message);
        return convertEntityToDto(savedMessage);
    }

    // ========================================================================
    // 2. LẤY LỊCH SỬ TIN NHẮN
    // ========================================================================
    public Page<ChatMessageDTO> getChatHistory(UUID currentUserId, UUID otherUserId, int page, int size) {
        if (!userRepository.existsById(currentUserId)) {
            throw new RuntimeException("User not found: " + currentUserId);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository.findConversation(
                currentUserId,
                otherUserId,
                pageable);
        return messages.map(this::convertEntityToDto);
    }

    // ========================================================================
    // 3. LẤY DANH SÁCH NGƯỜI ĐÃ CHAT (SỬA LẠI THEO YÊU CẦU)
    // ========================================================================
    public List<ChatPartnerDTO> getConversations(UUID currentUserId) {
        // B1: Lấy danh sách ID các đối tác đã từng chat
        List<UUID> partnerIds = messageRepository.findConversationPartners(currentUserId);

        if (partnerIds == null || partnerIds.isEmpty()) {
            return new ArrayList<>();
        }

        // B2: Chuyển đổi sang danh sách DTO và xử lý logic bổ trợ
        return partnerIds.stream()
                .map(partnerId -> {
                    User partner = userRepository.findById(partnerId).orElse(null);
                    if (partner == null)
                        return null;

                    // Lấy số tin chưa đọc và thời gian tin cuối từ Repository
                    long unreadFromPartner = messageRepository.countUnreadFromPartner(currentUserId, partnerId);
                    java.time.LocalDateTime latestTime = messageRepository.findLatestMessageTime(currentUserId,
                            partnerId);

                    // Ép kiểu tường minh cho Builder để tránh lỗi Inference
                    return ChatPartnerDTO.builder()
                            .id(partner.getId())
                            .fullName(partner.getFullName())
                            .email(partner.getEmail())
                            .unreadCount(unreadFromPartner)
                            .latestMessageTime(latestTime)
                            .build();
                })
                .filter(java.util.Objects::nonNull) // Loại bỏ các kết quả null
                .sorted((a, b) -> {
                    // Sắp xếp giảm dần theo thời gian (mới nhất lên đầu)
                    if (a.getLatestMessageTime() == null)
                        return 1;
                    if (b.getLatestMessageTime() == null)
                        return -1;
                    return b.getLatestMessageTime().compareTo(a.getLatestMessageTime());
                })
                .collect(Collectors.toList());
    }

    public List<Message> searchConversation(UUID myId, UUID partnerId, String keyword) {
        return messageRepository.searchMessages(myId, partnerId, keyword);
    }

    public int getPageNumberOfMessage(UUID currentUserId, UUID partnerId, UUID messageId) {
        // 1. Đếm số tin nhắn mới hơn nó
        long newerCount = messageRepository.countMessagesNewerThan(currentUserId, partnerId, messageId);

        // 2. Tính số trang (mặc định size = 20)
        int pageSize = 20;
        return (int) (newerCount / pageSize);
    }

    public long getUnreadCount(UUID myId) {
        return messageRepository.countUnreadMessages(myId);
    }

    // 👇 2. Hàm đánh dấu đã đọc (Cần @Transactional vì là lệnh UPDATE)
    @Transactional
    public void markMessagesAsRead(UUID myId, UUID partnerId) {
        messageRepository.markAllAsRead(myId, partnerId);
    }

    // 4. LẤY KHO LƯU TRỮ (MEDIA & FILES)
    public List<ChatMessageDTO> getConversationMedia(UUID myId, UUID partnerId, String typeStr) {
        // 1. Chuyển đổi String gửi từ FE (IMAGE/FILE) thành Enum
        MessageType mType;
        try {
            mType = MessageType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RuntimeException("Loại media không hợp lệ: " + typeStr);
        }

        // 2. Truyền Enum vào hàm Repository
        List<Message> mediaMessages = messageRepository.findAllMediaByType(myId, partnerId, mType);

        return mediaMessages.stream()
                .map(this::convertEntityToDto)
                .collect(Collectors.toList());
    }

    // Helper convert
    private ChatMessageDTO convertEntityToDto(Message message) {
        return ChatMessageDTO.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .receiverId(message.getReceiver().getId())
                .content(message.getContent())
                .attachmentUrl(message.getAttachmentUrl())
                .type(message.getType())
                .createdAt(message.getCreatedAt())
                .isRead(message.isRead())
                .senderName(message.getSender().getFullName())
                .senderType(message.getSenderType()) // Quan trọng: để mobile biết tin từ ai
                .build();
    }
}