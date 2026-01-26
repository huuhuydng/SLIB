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
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;
import slib.com.example.repository.chat.ConversationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

        private final ConversationRepository conversationRepository;
        private final UserRepository userRepository;
        private final SimpMessagingTemplate messagingTemplate;

        /**
         * Lấy hoặc tạo conversation cho student
         * Nếu có conversation đang active (không phải RESOLVED), trả về nó
         * Nếu không, tạo mới với trạng thái AI_HANDLING
         */
        @Transactional
        public Conversation getOrCreateConversation(UUID studentId) {
                // Tìm conversation đang active
                Optional<Conversation> existingConv = conversationRepository
                                .findByStudentIdAndStatusNot(studentId, ConversationStatus.RESOLVED);

                if (existingConv.isPresent()) {
                        return existingConv.get();
                }

                // Tạo mới conversation
                User student = userRepository.findById(studentId)
                                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

                Conversation newConv = Conversation.builder()
                                .student(student)
                                .status(ConversationStatus.AI_HANDLING)
                                .build();

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
                log.info("🔔 Escalated conversation {} to human. Reason: {}", conversationId, reason);

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

                log.info("✅ Librarian {} took over conversation {}", librarianId, conversationId);
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
         * Convert entity to DTO
         */
        private ConversationDTO convertToDTO(Conversation conv) {
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
}
