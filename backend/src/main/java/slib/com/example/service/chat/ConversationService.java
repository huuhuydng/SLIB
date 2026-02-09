package slib.com.example.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.chat.ChatMessageDTO;
import slib.com.example.dto.chat.ConversationDTO;
import slib.com.example.entity.chat.Conversation;
import slib.com.example.entity.chat.ConversationStatus;
import slib.com.example.entity.chat.Message;
import slib.com.example.entity.chat.MessageType;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;
import slib.com.example.repository.chat.ConversationRepository;
import slib.com.example.repository.chat.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

        private final ConversationRepository conversationRepository;
        private final UserRepository userRepository;
        private final MessageRepository messageRepository;
        private final SimpMessagingTemplate messagingTemplate;

        /**
         * Tạo conversation MỚI cho student mỗi lần escalate
         * QUAN TRỌNG: Không reuse conversation cũ để tránh lẫn tin nhắn
         */
        @Transactional
        public Conversation getOrCreateConversation(UUID studentId) {
                // LUÔN tạo mới conversation - mỗi lần escalate là 1 conversation riêng
                User student = userRepository.findById(studentId)
                                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

                Conversation newConv = Conversation.builder()
                                .student(student)
                                .status(ConversationStatus.AI_HANDLING)
                                .build();

                log.info("[Conversation] Created NEW conversation for student: {}", studentId);
                return conversationRepository.save(newConv);
        }

        /**
         * Chuyển trạng thái sang QUEUE_WAITING và thông báo cho Librarian
         */
        @Transactional
        public ConversationDTO escalateToHuman(UUID conversationId, String reason) {
                Conversation conv = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

                conv.setStatus(ConversationStatus.QUEUE_WAITING);
                conv.setEscalationReason(reason);
                conv.setEscalatedAt(LocalDateTime.now());

                Conversation saved = conversationRepository.save(conv);

                // Broadcast cho tất cả Librarian
                ConversationDTO dto = convertToDTO(saved);
                messagingTemplate.convertAndSend("/topic/escalate", dto);
                log.info("[Conversation] Escalated conversation {} to human. Reason: {}", conversationId, reason);

                return dto;
        }

        /**
         * Librarian tiếp nhận conversation
         */
        @Transactional
        public ConversationDTO takeOverConversation(UUID conversationId, UUID librarianId) {
                Conversation conv = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

                if (conv.getStatus() != ConversationStatus.QUEUE_WAITING &&
                                conv.getStatus() != ConversationStatus.AI_HANDLING) {
                        throw new RuntimeException(
                                        "Conversation is not available for takeover. Current status: "
                                                        + conv.getStatus());
                }

                User librarian = userRepository.findById(librarianId)
                                .orElseThrow(() -> new RuntimeException("Librarian not found: " + librarianId));

                conv.setStatus(ConversationStatus.HUMAN_CHATTING);
                conv.setLibrarian(librarian);

                Conversation saved = conversationRepository.save(conv);

                // Thông báo cho student
                messagingTemplate.convertAndSend(
                                "/topic/chat/" + conv.getStudent().getId(),
                                java.util.Map.of(
                                                "type", "LIBRARIAN_JOINED",
                                                "librarianName", librarian.getFullName(),
                                                "conversationId", conversationId));

                log.info("[Conversation] Librarian {} took over conversation {}", librarianId, conversationId);
                return convertToDTO(saved);
        }

        /**
         * Đánh dấu conversation đã hoàn thành
         */
        @Transactional
        public ConversationDTO resolveConversation(UUID conversationId) {
                Conversation conv = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

                conv.setStatus(ConversationStatus.RESOLVED);
                conv.setResolvedAt(LocalDateTime.now());

                return convertToDTO(conversationRepository.save(conv));
        }

        /**
         * Lấy danh sách conversation đang chờ xử lý
         */
        public List<ConversationDTO> getWaitingConversations() {
                return conversationRepository
                                .findByStatusOrderByCreatedAtAsc(ConversationStatus.QUEUE_WAITING)
                                .stream()
                                .map(this::convertToDTO)
                                .collect(Collectors.toList());
        }

        /**
         * Lấy danh sách conversation đang được Librarian xử lý
         */
        public List<ConversationDTO> getActiveConversations(UUID librarianId) {
                return conversationRepository
                                .findByLibrarianIdAndStatusOrderByUpdatedAtDesc(librarianId,
                                                ConversationStatus.HUMAN_CHATTING)
                                .stream()
                                .map(this::convertToDTO)
                                .collect(Collectors.toList());
        }

        /**
         * Lấy tất cả conversations (waiting + active của librarian)
         */
        public List<ConversationDTO> getAllConversationsForLibrarian(UUID librarianId) {
                List<Conversation> waiting = conversationRepository
                                .findByStatusOrderByCreatedAtAsc(ConversationStatus.QUEUE_WAITING);
                List<Conversation> active = conversationRepository
                                .findByLibrarianIdAndStatusOrderByUpdatedAtDesc(librarianId,
                                                ConversationStatus.HUMAN_CHATTING);

                // Combine and convert
                waiting.addAll(active);
                return waiting.stream()
                                .map(this::convertToDTO)
                                .collect(Collectors.toList());
        }

        /**
         * Lấy conversation theo ID
         */
        public Optional<Conversation> getConversationById(UUID conversationId) {
                return conversationRepository.findById(conversationId);
        }

        /**
         * Đếm số conversation đang chờ
         */
        public long countWaitingConversations() {
                return conversationRepository.countByStatus(ConversationStatus.QUEUE_WAITING);
        }

        /**
         * Lấy vị trí trong hàng đợi cho một conversation
         * 
         * @param conversationId ID của conversation
         * @return Vị trí trong queue (1-indexed), hoặc 0 nếu không trong queue
         */
        public int getQueuePosition(UUID conversationId) {
                List<Conversation> waitingList = conversationRepository
                                .findByStatusOrderByCreatedAtAsc(ConversationStatus.QUEUE_WAITING);

                for (int i = 0; i < waitingList.size(); i++) {
                        if (waitingList.get(i).getId().equals(conversationId)) {
                                return i + 1; // 1-indexed
                        }
                }
                return 0; // Không trong queue
        }

        /**
         * Tạo conversation mới và escalate trong 1 bước (cho mobile app)
         */
        @Transactional
        public ConversationDTO createAndEscalate(UUID studentId, String reason) {
                // Tạo hoặc lấy conversation
                Conversation conv = getOrCreateConversation(studentId);

                // Escalate và trả về DTO
                return escalateToHuman(conv.getId(), reason);
        }

        /**
         * Tạo conversation mới, lưu message history và escalate
         */
        @Transactional
        public ConversationDTO createAndEscalateWithHistory(UUID studentId, String reason,
                        List<Map<String, Object>> messageHistory) {
                // Tạo hoặc lấy conversation
                Conversation conv = getOrCreateConversation(studentId);
                User student = conv.getStudent();

                // Lưu message history vào DB nếu có
                if (messageHistory != null && !messageHistory.isEmpty()) {
                        for (Map<String, Object> msg : messageHistory) {
                                String content = (String) msg.get("content");
                                String senderType = (String) msg.get("senderType");

                                if (content == null || content.trim().isEmpty())
                                        continue;

                                Message message = Message.builder()
                                                .sender(student)
                                                .receiver(student) // Self for AI messages
                                                .content(content)
                                                .type(MessageType.TEXT)
                                                .conversation(conv)
                                                .senderType(senderType != null ? senderType : "STUDENT")
                                                .build();

                                messageRepository.save(message);

                                log.info("[Conversation] Saved message history: {} - type: {}",
                                                content.substring(0, Math.min(50, content.length())), senderType);
                        }
                }

                // Escalate và trả về DTO
                return escalateToHuman(conv.getId(), reason);
        }

        @Transactional
        public ConversationDTO cancelEscalation(UUID conversationId) {
                Conversation conv = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

                if (conv.getStatus() == ConversationStatus.QUEUE_WAITING) {
                        conv.setStatus(ConversationStatus.AI_HANDLING);
                        conv.setEscalationReason(null);
                        conv.setEscalatedAt(null);

                        Conversation saved = conversationRepository.save(conv);
                        log.info("[Conversation] Cancelled escalation for conversation {}", conversationId);
                        return convertToDTO(saved);
                }

                return convertToDTO(conv);
        }

        /**
         * Convert entity to DTO
         */
        public ConversationDTO convertToDTO(Conversation conv) {
                // Lấy tin nhắn cuối cùng
                ChatMessageDTO lastMessage = null;
                if (conv.getMessages() != null && !conv.getMessages().isEmpty()) {
                        Message lastMsg = conv.getMessages().get(conv.getMessages().size() - 1);
                        lastMessage = ChatMessageDTO.builder()
                                        .id(lastMsg.getId())
                                        .senderId(lastMsg.getSender().getId())
                                        .receiverId(lastMsg.getReceiver().getId())
                                        .content(lastMsg.getContent())
                                        .type(lastMsg.getType())
                                        .createdAt(lastMsg.getCreatedAt())
                                        .build();
                }

                return ConversationDTO.builder()
                                .id(conv.getId())
                                .studentId(conv.getStudent().getId())
                                .studentName(conv.getStudent().getFullName())
                                .studentEmail(conv.getStudent().getEmail())
                                .librarianId(conv.getLibrarian() != null ? conv.getLibrarian().getId() : null)
                                .librarianName(conv.getLibrarian() != null ? conv.getLibrarian().getFullName() : null)
                                .status(conv.getStatus())
                                .escalationReason(conv.getEscalationReason())
                                .createdAt(conv.getCreatedAt())
                                .updatedAt(conv.getUpdatedAt())
                                .escalatedAt(conv.getEscalatedAt())
                                .lastMessage(lastMessage)
                                .build();
        }

        /**
         * Lấy tất cả messages của conversation
         */
        public List<ChatMessageDTO> getConversationMessages(UUID conversationId) {
                List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
                return messages.stream()
                                .map(this::convertMessageToDTO)
                                .collect(Collectors.toList());
        }

        /**
         * Thêm message vào conversation
         */
        @Transactional
        public ChatMessageDTO addMessageToConversation(UUID conversationId, UUID senderId, String content,
                        String senderType) {
                Conversation conv = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

                User sender = userRepository.findById(senderId)
                                .orElseThrow(() -> new RuntimeException("User not found: " + senderId));

                Message message = Message.builder()
                                .sender(sender)
                                .receiver(conv.getStudent().getId().equals(senderId)
                                                ? (conv.getLibrarian() != null ? conv.getLibrarian() : sender)
                                                : conv.getStudent())
                                .content(content)
                                .type(MessageType.TEXT)
                                .conversation(conv)
                                .senderType(senderType)
                                .build();

                Message savedMessage = messageRepository.save(message);
                log.info("[Conversation] Added message to conversation {}: {} - type: {}", conversationId, content,
                                senderType);

                ChatMessageDTO dto = convertMessageToDTO(savedMessage);

                // Broadcast qua WebSocket
                messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, dto);

                return dto;
        }

        private ChatMessageDTO convertMessageToDTO(Message msg) {
                return ChatMessageDTO.builder()
                                .id(msg.getId())
                                .senderId(msg.getSender().getId())
                                .receiverId(msg.getReceiver().getId())
                                .content(msg.getContent())
                                .createdAt(msg.getCreatedAt())
                                .senderName(msg.getSender().getFullName())
                                .senderType(msg.getSenderType())
                                .build();
        }
}
