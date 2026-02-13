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
         * Lấy hoặc tạo session duy nhất cho student
         * QUAN TRỌNG: Mỗi user chỉ có 1 session (long-lived)
         * Các lần escalate được track bằng currentHumanSession
         */
        @Transactional
        public Conversation getOrCreateConversation(UUID studentId) {
                // Tìm session hiện có (bất kể status)
                Optional<Conversation> existingConv = conversationRepository
                                .findByStudentId(studentId)
                                .stream()
                                .findFirst();

                if (existingConv.isPresent()) {
                        log.info("[Conversation] Returning existing session for student: {}", studentId);
                        return existingConv.get();
                }

                // Tạo mới session nếu chưa có
                User student = userRepository.findById(studentId)
                                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

                Conversation newConv = Conversation.builder()
                                .student(student)
                                .status(ConversationStatus.AI_HANDLING)
                                .currentHumanSession(0)
                                .build();

                log.info("[Conversation] Created NEW session for student: {}", studentId);
                return conversationRepository.save(newConv);
        }

        /**
         * Chuyển trạng thái sang QUEUE_WAITING và thông báo cho Librarian
         * QUAN TRỌNG: Increment currentHumanSession để track lần escalate mới
         */
        @Transactional
        public ConversationDTO escalateToHuman(UUID conversationId, String reason) {
                Conversation conv = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

                // Increment human session counter (1, 2, 3, ...)
                int newHumanSession = (conv.getCurrentHumanSession() != null ? conv.getCurrentHumanSession() : 0) + 1;
                conv.setCurrentHumanSession(newHumanSession);

                conv.setStatus(ConversationStatus.QUEUE_WAITING);
                conv.setEscalationReason(reason);
                conv.setEscalatedAt(LocalDateTime.now());

                Conversation saved = conversationRepository.save(conv);

                // Gán bot messages (humanSessionId = NULL) vào session mới
                // Để thủ thư thấy context AI trước khi escalate
                int updated = messageRepository.assignBotMessagesToHumanSession(conversationId, newHumanSession);
                log.info("[Conversation] Assigned {} bot context messages to human session {}",
                                updated, newHumanSession);

                // Broadcast cho tất cả Librarian
                ConversationDTO dto = convertToDTO(saved);
                messagingTemplate.convertAndSend("/topic/escalate", dto);
                log.info("[Conversation] Escalated conversation {} to human. Human session: {}, Reason: {}",
                                conversationId, newHumanSession, reason);

                // Broadcast queue position updates to all waiting students
                broadcastQueuePositionUpdates();

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

                // Broadcast CONVERSATION_ACCEPTED để frontend cập nhật UI ngay
                messagingTemplate.convertAndSend("/topic/escalate", Map.of(
                                "type", "CONVERSATION_ACCEPTED",
                                "conversationId", conversationId.toString()));

                log.info("[Conversation] Librarian {} took over conversation {}", librarianId, conversationId);

                // Broadcast queue position updates to remaining waiting students
                broadcastQueuePositionUpdates();

                return convertToDTO(saved);
        }

        /**
         * Kết thúc phiên chat với thủ thư
         * QUAN TRỌNG: Set status về AI_HANDLING (không phải RESOLVED)
         * để user có thể tiếp tục chat với bot sau khi kết thúc
         */
        @Transactional
        public ConversationDTO resolveConversation(UUID conversationId) {
                Conversation conv = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

                // Set status về AI_HANDLING để user có thể tiếp tục chat với bot
                conv.setStatus(ConversationStatus.AI_HANDLING);
                conv.setResolvedAt(LocalDateTime.now());
                // Giữ librarian để biết ai đã xử lý lần cuối
                // Giữ currentHumanSession để lần escalate tiếp theo sẽ increment

                Conversation saved = conversationRepository.save(conv);

                log.info("[Conversation] Librarian ended chat for session {}. Human session: {}",
                                conversationId, conv.getCurrentHumanSession());

                // Broadcast CHAT_ENDED event cho student qua WebSocket
                // Student sẽ nhận được và chuyển về AI mode
                ChatMessageDTO endMessage = ChatMessageDTO.builder()
                                .id(UUID.randomUUID())
                                .content("Thủ thư đã kết thúc cuộc trò chuyện")
                                .senderType("SYSTEM")
                                .type(MessageType.SYSTEM)
                                .createdAt(LocalDateTime.now())
                                .build();

                messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, endMessage);

                // Broadcast CONVERSATION_RESOLVED để frontend xóa khỏi active list ngay
                messagingTemplate.convertAndSend("/topic/escalate", Map.of(
                                "type", "CONVERSATION_RESOLVED",
                                "conversationId", conversationId.toString()));

                log.info("[WebSocket] Sent CHAT_ENDED notification to student for conversation: {}", conversationId);
                return convertToDTO(saved);
        }

        /**
         * Lấy danh sách conversation đang chờ xử lý
         */
        public List<ConversationDTO> getWaitingConversations() {
                return conversationRepository
                                .findByStatusOrderByEscalatedAtAsc(ConversationStatus.QUEUE_WAITING)
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
         * Lấy conversation active của student (HUMAN_CHATTING hoặc QUEUE_WAITING)
         */
        public ConversationDTO getActiveConversationForStudent(UUID studentId) {
                // Check HUMAN_CHATTING trước
                var humanChatting = conversationRepository
                                .findByStudentIdAndStatus(studentId, ConversationStatus.HUMAN_CHATTING);
                if (humanChatting.isPresent()) {
                        return convertToDTO(humanChatting.get());
                }
                // Check QUEUE_WAITING
                var queueWaiting = conversationRepository
                                .findByStudentIdAndStatus(studentId, ConversationStatus.QUEUE_WAITING);
                if (queueWaiting.isPresent()) {
                        return convertToDTO(queueWaiting.get());
                }
                return null;
        }

        /**
         * Student hủy chờ queue - chuyển trạng thái sang RESOLVED
         */
        @Transactional
        public void cancelQueue(UUID conversationId, UUID studentId) {
                Conversation conv = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

                // Verify student owns this conversation
                if (!conv.getStudent().getId().equals(studentId)) {
                        throw new RuntimeException("Unauthorized: conversation does not belong to this student");
                }

                // Only cancel if currently QUEUE_WAITING
                if (conv.getStatus() != ConversationStatus.QUEUE_WAITING) {
                        throw new RuntimeException("Conversation is not in QUEUE_WAITING status");
                }

                conv.setStatus(ConversationStatus.RESOLVED);
                conversationRepository.save(conv);

                // Broadcast cho librarian biết queue đã bị hủy
                messagingTemplate.convertAndSend("/topic/escalate", Map.of(
                                "type", "QUEUE_CANCELLED",
                                "conversationId", conversationId.toString()));

                log.info("[Conversation] Student {} cancelled queue for conversation {}", studentId, conversationId);

                // Broadcast queue position updates to remaining waiting students
                broadcastQueuePositionUpdates();
        }

        /**
         * Lấy tất cả conversations (waiting + active của librarian)
         */
        public List<ConversationDTO> getAllConversationsForLibrarian(UUID librarianId) {
                List<Conversation> waiting = conversationRepository
                                .findByStatusOrderByEscalatedAtAsc(ConversationStatus.QUEUE_WAITING);
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
                                .findByStatusOrderByEscalatedAtAsc(ConversationStatus.QUEUE_WAITING);

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
         * Tạo conversation, escalate, và lưu message history
         * Messages được gán humanSessionId = currentSession (KHÔNG PHẢI NULL)
         * để tránh duplicate khi escalate nhiều lần
         */
        @Transactional
        public ConversationDTO createAndEscalateWithHistory(UUID studentId, String reason,
                        List<Map<String, Object>> messageHistory) {
                // Tạo hoặc lấy conversation
                Conversation conv = getOrCreateConversation(studentId);
                User student = conv.getStudent();

                // Increment human session counter (1, 2, 3, ...)
                int newHumanSession = (conv.getCurrentHumanSession() != null ? conv.getCurrentHumanSession() : 0) + 1;
                conv.setCurrentHumanSession(newHumanSession);
                conv.setStatus(ConversationStatus.QUEUE_WAITING);
                conv.setEscalationReason(reason);
                conv.setEscalatedAt(LocalDateTime.now());
                Conversation saved = conversationRepository.save(conv);

                // KHÔNG gọi assignBotMessagesToHumanSession ở đây
                // Vì messages AI chỉ tồn tại local trên mobile, sẽ được lưu từ messageHistory
                // bên dưới

                // Lưu message history với humanSessionId = newHumanSession
                if (messageHistory != null && !messageHistory.isEmpty()) {
                        for (Map<String, Object> msg : messageHistory) {
                                String content = (String) msg.get("content");
                                String senderType = (String) msg.get("senderType");

                                if (content == null || content.trim().isEmpty())
                                        continue;
                                if ("LIBRARIAN".equals(senderType))
                                        continue;

                                Message message = Message.builder()
                                                .sender(student)
                                                .receiver(student)
                                                .content(content)
                                                .type(MessageType.TEXT)
                                                .conversation(conv)
                                                .senderType(senderType != null ? senderType : "STUDENT")
                                                .humanSessionId(newHumanSession)
                                                .build();

                                messageRepository.save(message);
                        }
                        log.info("[Conversation] Saved {} message history with humanSession={}",
                                        messageHistory.size(), newHumanSession);
                }

                // Broadcast cho tất cả Librarian
                ConversationDTO dto = convertToDTO(saved);
                messagingTemplate.convertAndSend("/topic/escalate", dto);
                log.info("[Conversation] Escalated conversation {} to human. Human session: {}, Reason: {}",
                                conv.getId(), newHumanSession, reason);

                return dto;
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
         * Broadcast queue position updates to all waiting students via WebSocket
         * Called after queue changes (accept, cancel, new escalation)
         */
        private void broadcastQueuePositionUpdates() {
                List<Conversation> waitingList = conversationRepository
                                .findByStatusOrderByEscalatedAtAsc(ConversationStatus.QUEUE_WAITING);

                for (int i = 0; i < waitingList.size(); i++) {
                        Conversation conv = waitingList.get(i);
                        int position = i + 1;
                        messagingTemplate.convertAndSend(
                                        "/topic/chat/" + conv.getStudent().getId(),
                                        Map.of(
                                                        "type", "QUEUE_POSITION_UPDATE",
                                                        "conversationId", conv.getId().toString(),
                                                        "queuePosition", position,
                                                        "totalWaiting", waitingList.size()));
                }
                log.info("[WebSocket] Broadcast queue position updates to {} waiting students", waitingList.size());
        }

        /**
         * Gửi thông báo LIBRARIAN_JOINED cho sinh viên qua WebSocket
         * Dùng khi thủ thư bắt đầu chat từ yêu cầu hỗ trợ (không qua queue)
         */
        public void notifyStudentLibrarianJoined(UUID conversationId, UUID studentId, String librarianName) {
                messagingTemplate.convertAndSend(
                                "/topic/chat/" + studentId,
                                Map.of(
                                                "type", "LIBRARIAN_JOINED",
                                                "librarianName", librarianName,
                                                "conversationId", conversationId));

                // Broadcast CONVERSATION_ACCEPTED để frontend cập nhật UI ngay
                messagingTemplate.convertAndSend("/topic/escalate", Map.of(
                                "type", "CONVERSATION_ACCEPTED",
                                "conversationId", conversationId.toString()));

                log.info("[WebSocket] Notified student {} that librarian {} joined conversation {}",
                                studentId, librarianName, conversationId);
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
                                .currentHumanSession(conv.getCurrentHumanSession())
                                .build();
        }

        /**
         * Lấy messages của conversation với filter theo currentHumanSession
         * Chi load: bot messages (humanSessionId IS NULL) + messages của current human
         * session
         */
        public List<ChatMessageDTO> getConversationMessages(UUID conversationId) {
                Conversation conv = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

                Integer currentHumanSession = conv.getCurrentHumanSession();
                List<Message> messages;

                if (currentHumanSession != null && currentHumanSession > 0) {
                        // Filter by humanSessionId
                        messages = messageRepository.findBySessionWithHumanSessionFilter(conversationId,
                                        currentHumanSession);
                        log.info("[Conversation] Loading messages for session {} with humanSessionId={}",
                                        conversationId, currentHumanSession);
                } else {
                        // No human session yet, load all
                        messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
                }

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

                // Set humanSessionId based on senderType and conversation status
                // - LIBRARIAN messages: always get current human session ID
                // - STUDENT messages trong HUMAN_CHATTING: get current human session ID (để
                // filter đúng lần escalate)
                // - STUDENT/AI messages khi đang chat với bot: get null (bot conversation
                // context)
                Integer humanSessionId;
                if ("LIBRARIAN".equals(senderType) || "SYSTEM".equals(senderType)) {
                        humanSessionId = conv.getCurrentHumanSession();
                } else if ("STUDENT".equals(senderType) &&
                                (conv.getStatus() == ConversationStatus.HUMAN_CHATTING ||
                                                conv.getStatus() == ConversationStatus.QUEUE_WAITING)) {
                        // Student đang chat với thủ thư hoặc đang chờ -> gán humanSessionId
                        humanSessionId = conv.getCurrentHumanSession();
                } else {
                        // AI messages hoặc student chat với bot -> null
                        humanSessionId = null;
                }

                Message message = Message.builder()
                                .sender(sender)
                                .receiver(conv.getStudent().getId().equals(senderId)
                                                ? (conv.getLibrarian() != null ? conv.getLibrarian() : sender)
                                                : conv.getStudent())
                                .content(content)
                                .type(MessageType.TEXT)
                                .conversation(conv)
                                .senderType(senderType)
                                .humanSessionId(humanSessionId)
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
