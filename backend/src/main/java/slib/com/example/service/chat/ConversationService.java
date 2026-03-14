package slib.com.example.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
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
import slib.com.example.repository.users.UserRepository;
import slib.com.example.repository.chat.ConversationRepository;
import slib.com.example.repository.chat.MessageRepository;
import slib.com.example.service.notification.LibrarianNotificationService;
import slib.com.example.service.notification.PushNotificationService;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

        private final ConversationRepository conversationRepository;
        private final UserRepository userRepository;
        private final MessageRepository messageRepository;
        private final SimpMessagingTemplate messagingTemplate;
        private final @Lazy LibrarianNotificationService librarianNotificationService;
        private final @Lazy PushNotificationService pushNotificationService;

        @Value("${app.ai-service.url:http://127.0.0.1:8001}")
        private String aiServiceUrl;

        private final RestTemplate restTemplate = new RestTemplate();

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
                // QUAN TRỌNG: Nếu đã từng resolve, chỉ gán messages MỚI (sau lần resolve gần
                // nhất)
                // để tránh gán lại messages cũ từ session trước
                int updated;
                if (conv.getResolvedAt() != null) {
                        updated = messageRepository.assignRecentBotMessagesToHumanSession(
                                        conversationId, newHumanSession, conv.getResolvedAt());
                } else {
                        updated = messageRepository.assignBotMessagesToHumanSession(
                                        conversationId, newHumanSession);
                }
                log.info("[Conversation] Assigned {} bot context messages to human session {}",
                                updated, newHumanSession);

                // Broadcast cho tất cả Librarian
                ConversationDTO dto = convertToDTO(saved);
                messagingTemplate.convertAndSend("/topic/escalate", dto);
                log.info("[Conversation] Escalated conversation {} to human. Human session: {}, Reason: {}",
                                conversationId, newHumanSession, reason);

                // Broadcast queue position updates to all waiting students
                broadcastQueuePositionUpdates();

                librarianNotificationService.broadcastPendingCounts("CHAT", "ESCALATED");
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

                librarianNotificationService.broadcastPendingCounts("CHAT", "TAKEN_OVER");
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
                librarianNotificationService.broadcastPendingCounts("CHAT", "RESOLVED");
                return convertToDTO(saved);
        }

        /**
         * Sinh viên kết thúc cuộc trò chuyện với thủ thư
         * Tương tự resolveConversation nhưng từ phía sinh viên
         */
        @Transactional
        public ConversationDTO studentResolveConversation(UUID conversationId, UUID studentId) {
                Conversation conv = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

                // Verify student owns this conversation
                if (!conv.getStudent().getId().equals(studentId)) {
                        throw new RuntimeException("Unauthorized: conversation does not belong to this student");
                }

                // Only resolve if currently HUMAN_CHATTING
                if (conv.getStatus() != ConversationStatus.HUMAN_CHATTING) {
                        throw new RuntimeException("Conversation is not in HUMAN_CHATTING status");
                }

                conv.setStatus(ConversationStatus.AI_HANDLING);
                conv.setResolvedAt(LocalDateTime.now());

                Conversation saved = conversationRepository.save(conv);

                log.info("[Conversation] Student {} ended chat for conversation {}. Human session: {}",
                                studentId, conversationId, conv.getCurrentHumanSession());

                // Broadcast STUDENT_ENDED_CHAT cho librarian qua WebSocket
                messagingTemplate.convertAndSend("/topic/escalate", Map.of(
                                "type", "STUDENT_ENDED_CHAT",
                                "conversationId", conversationId.toString(),
                                "studentName", conv.getStudent().getFullName()));

                // Broadcast CONVERSATION_RESOLVED để frontend xóa khỏi active list
                messagingTemplate.convertAndSend("/topic/escalate", Map.of(
                                "type", "CONVERSATION_RESOLVED",
                                "conversationId", conversationId.toString()));

                librarianNotificationService.broadcastPendingCounts("CHAT", "STUDENT_RESOLVED");
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

                // Broadcast pending counts để badge thủ thư tự cập nhật
                librarianNotificationService.broadcastPendingCounts("CHAT", "CANCELLED");
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
         * Tạo conversation, escalate, và lưu aiSessionId để backend có thể đọc
         * chat history từ MongoDB qua AI service API
         */
        @Transactional
        public ConversationDTO createAndEscalateWithHistory(UUID studentId, String reason,
                        List<Map<String, Object>> messageHistory, String aiSessionId) {
                // Tạo hoặc lấy conversation
                Conversation conv = getOrCreateConversation(studentId);

                // Increment human session counter (1, 2, 3, ...)
                int newHumanSession = (conv.getCurrentHumanSession() != null ? conv.getCurrentHumanSession() : 0) + 1;
                conv.setCurrentHumanSession(newHumanSession);
                conv.setStatus(ConversationStatus.QUEUE_WAITING);
                conv.setEscalationReason(reason);
                conv.setEscalatedAt(LocalDateTime.now());

                // Lưu AI session ID để load chat history từ MongoDB
                if (aiSessionId != null && !aiSessionId.trim().isEmpty()) {
                        conv.setAiSessionId(aiSessionId);
                        log.info("[Conversation] Saved aiSessionId={} for conversation {}", aiSessionId, conv.getId());
                }

                Conversation saved = conversationRepository.save(conv);

                // Lưu escalation messages vào PostgreSQL
                // Để thủ thư thấy context: "Chat với Thủ thư SLIB" + "Dạ mình đang điều
                // hướng..."
                Message studentMsg = Message.builder()
                                .conversation(saved)
                                .content("Chat với Thủ thư SLIB")
                                .type(MessageType.TEXT)
                                .senderType("STUDENT")
                                .sender(saved.getStudent())
                                .receiver(saved.getStudent())
                                .humanSessionId(newHumanSession)
                                .build();
                messageRepository.save(studentMsg);

                Message aiMsg = Message.builder()
                                .conversation(saved)
                                .content("Dạ mình đang điều hướng bạn tới bộ phận thủ thư của thư viện, bạn vui lòng đợi chút nhé")
                                .type(MessageType.TEXT)
                                .senderType("AI")
                                .sender(saved.getStudent())
                                .receiver(saved.getStudent())
                                .humanSessionId(newHumanSession)
                                .build();
                messageRepository.save(aiMsg);

                // Broadcast cho tất cả Librarian
                ConversationDTO dto = convertToDTO(saved);
                messagingTemplate.convertAndSend("/topic/escalate", dto);
                log.info("[Conversation] Escalated conversation {} to human. Human session: {}, Reason: {}",
                                conv.getId(), newHumanSession, reason);

                // Broadcast queue position updates to all waiting students
                broadcastQueuePositionUpdates();

                // Broadcast pending counts để badge thủ thư tự cập nhật
                librarianNotificationService.broadcastPendingCounts("CHAT", "ESCALATED");

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
                                .studentCode(conv.getStudent().getUserCode())
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
         * Lấy messages của conversation:
         * 1. Messages từ PostgreSQL (chat với thủ thư)
         * 2. Messages từ MongoDB qua AI service API (chat với AI)
         * Merge và sort theo thời gian
         */
        public List<ChatMessageDTO> getConversationMessages(UUID conversationId) {
                Conversation conv = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

                // 1. Load messages từ PostgreSQL (librarian/student chat sau escalation)
                Integer currentHumanSession = conv.getCurrentHumanSession();
                List<ChatMessageDTO> allMessages = new java.util.ArrayList<>();

                if (currentHumanSession != null && currentHumanSession > 0) {
                        List<Message> dbMessages = messageRepository.findBySessionWithHumanSessionFilter(
                                        conversationId, currentHumanSession);
                        allMessages.addAll(dbMessages.stream()
                                        .map(this::convertMessageToDTO)
                                        .collect(Collectors.toList()));
                        log.info("[Conversation] Loaded {} DB messages for session {}",
                                        dbMessages.size(), currentHumanSession);
                }

                // 2. Load messages từ MongoDB qua AI service API
                // Nếu lần escalate >= 2: chỉ lấy AI messages SAU thời điểm session trước kết
                // thúc
                if (conv.getAiSessionId() != null && !conv.getAiSessionId().isEmpty()) {
                        try {
                                List<ChatMessageDTO> aiMessages = fetchAiChatHistory(conv.getAiSessionId(),
                                                conv.getStudent().getId());
                                if (!aiMessages.isEmpty()) {
                                        // Nếu có session trước (lần 2+), filter AI messages
                                        if (currentHumanSession != null && currentHumanSession > 1) {
                                                int prevSession = currentHumanSession - 1;
                                                LocalDateTime prevSessionEnd = messageRepository
                                                                .findMaxCreatedAtBySession(
                                                                                conversationId, prevSession);
                                                if (prevSessionEnd != null) {
                                                        log.info("[Conversation] Filtering AI messages after previous session {} end: {}",
                                                                        prevSession, prevSessionEnd);
                                                        aiMessages = aiMessages.stream()
                                                                        .filter(m -> m.getCreatedAt() != null
                                                                                        && m.getCreatedAt().isAfter(
                                                                                                        prevSessionEnd))
                                                                        .collect(Collectors.toList());
                                                }
                                        }
                                        allMessages.addAll(aiMessages);
                                        log.info("[Conversation] Loaded {} AI messages from MongoDB for session {}",
                                                        aiMessages.size(), conv.getAiSessionId());
                                }
                        } catch (Exception e) {
                                log.warn("[Conversation] Failed to load AI messages from MongoDB: {}", e.getMessage());
                        }
                }

                // Sort theo thời gian
                allMessages.sort((a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null)
                                return 0;
                        if (a.getCreatedAt() == null)
                                return -1;
                        if (b.getCreatedAt() == null)
                                return 1;
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                });

                log.info("[Conversation] Total {} messages for conversation {}",
                                allMessages.size(), conversationId);
                return allMessages;
        }

        /**
         * Lấy messages của conversation với phân trang (cho mobile lazy loading)
         * Trả về page mới nhất trước (DESC), client reverse lại để hiển thị ASC
         */
        public Page<ChatMessageDTO> getConversationMessagesPaginated(UUID conversationId, int page, int size) {
                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                Page<Message> messagePage = messageRepository.findByConversationIdPaginated(conversationId, pageable);
                return messagePage.map(this::convertMessageToDTO);
        }

        /**
         * Gọi AI service API để lấy chat history từ MongoDB
         */
        @SuppressWarnings("unchecked")
        private List<ChatMessageDTO> fetchAiChatHistory(String aiSessionId, UUID studentId) {
                String url = aiServiceUrl + "/api/v1/chat/history/" + aiSessionId + "?limit=100";
                log.info("[Conversation] Fetching AI chat history from: {}", url);

                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                Map<String, Object> body = response.getBody();

                if (body == null || !Boolean.TRUE.equals(body.get("success"))) {
                        return List.of();
                }

                List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
                if (messages == null || messages.isEmpty()) {
                        return List.of();
                }

                int[] indexHolder = { 0 };
                return messages.stream().map(msg -> {
                        int idx = indexHolder[0]++;
                        String role = (String) msg.get("role");
                        String content = (String) msg.get("content");
                        String timestamp = (String) msg.get("timestamp");

                        // role: "user" -> STUDENT, "assistant" -> AI
                        String senderType = "user".equals(role) ? "STUDENT" : "AI";

                        // Parse timestamp (API trả format: 2026-02-23T08:37:57.826000 - không có Z)
                        // MongoDB lưu UTC, cần chuyển sang VN timezone
                        LocalDateTime createdAt = null;
                        if (timestamp != null) {
                                try {
                                        // Thử parse ISO instant (có Z)
                                        createdAt = Instant.parse(timestamp)
                                                        .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                                                        .toLocalDateTime();
                                } catch (Exception e1) {
                                        try {
                                                // Parse LocalDateTime (không có Z) rồi chuyển từ UTC sang VN
                                                LocalDateTime utcTime = LocalDateTime.parse(timestamp);
                                                createdAt = utcTime.atZone(ZoneId.of("UTC"))
                                                                .withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"))
                                                                .toLocalDateTime();
                                        } catch (Exception e2) {
                                                createdAt = LocalDateTime.now();
                                        }
                                }
                        }

                        // Generate deterministic UUID for dedup on frontend
                        UUID msgId = UUID.nameUUIDFromBytes(
                                        ("ai-" + aiSessionId + "-" + idx).getBytes());

                        return ChatMessageDTO.builder()
                                        .id(msgId)
                                        .content(content)
                                        .senderType(senderType)
                                        .senderName("user".equals(role) ? null : "SLIB AI")
                                        .createdAt(createdAt)
                                        .senderId(studentId)
                                        .build();
                }).collect(Collectors.toList());
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

                // Gửi notification cho phía bên kia
                try {
                        String notifyContent = content.contains("[IMAGES]") ? "Đã gửi một hình ảnh" : content;
                        if (notifyContent.length() > 100) {
                                notifyContent = notifyContent.substring(0, 100) + "...";
                        }

                        log.debug("[Chat] Processing message notification for senderType={} in conv {}", senderType,
                                        conversationId);

                        if ("STUDENT".equals(senderType)) {
                                // Student gửi → thông báo cho tất cả thủ thư qua WebSocket
                                Map<String, Object> chatNotify = new java.util.LinkedHashMap<>();
                                chatNotify.put("type", "CHAT_NEW_MESSAGE");
                                chatNotify.put("conversationId", conversationId.toString());
                                chatNotify.put("senderName", sender.getFullName());
                                chatNotify.put("content", notifyContent);
                                chatNotify.put("timestamp", java.time.Instant.now().toString());
                                messagingTemplate.convertAndSend("/topic/librarian-notifications", chatNotify);
                                log.info("[Chat] Sent librarian notification for new student message in conv {}",
                                                conversationId);
                        } else if ("LIBRARIAN".equals(senderType)) {
                                // Librarian gửi → push notification cho student qua FCM
                                UUID studentId = conv.getStudent().getId();
                                log.debug("[Chat] Sending FCM to student {} for conv {}", studentId, conversationId);
                                pushNotificationService.sendToUser(
                                                studentId,
                                                "Tin nhắn mới từ Thủ thư",
                                                notifyContent,
                                                NotificationType.CHAT_MESSAGE,
                                                conversationId);
                                log.info("[Chat] Sent push notification to student {} for conv {}", studentId,
                                                conversationId);
                        } else if ("AI".equals(senderType)) {
                                // AI messages don't need notifications
                                log.debug("[Chat] Skipping notification for AI message in conv {}", conversationId);
                        } else {
                                log.warn("[Chat] Unknown senderType for notification: {}", senderType);
                        }
                } catch (Exception e) {
                        log.error("[Chat] Failed to send chat notification: {}", e.getMessage(), e);
                }

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
                                .isRead(msg.isRead())
                                .build();
        }

        /**
         * Đánh dấu tất cả messages của đối phương là đã đọc
         * - Student gọi: mark LIBRARIAN messages as read
         * - Librarian gọi: mark STUDENT messages as read
         */
        @Transactional
        public int markConversationAsRead(UUID conversationId, UUID userId) {
                Conversation conv = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

                int updated;
                if (conv.getStudent().getId().equals(userId)) {
                        // Student gọi → mark librarian messages as read
                        updated = messageRepository.markConversationLibrarianMessagesAsRead(conversationId);
                } else {
                        // Librarian gọi → mark student messages as read
                        updated = messageRepository.markConversationStudentMessagesAsRead(conversationId);
                }

                if (updated > 0) {
                        log.info("[Conversation] Marked {} messages as read in conversation {} by user {}",
                                        updated, conversationId, userId);
                        // Broadcast MESSAGES_READ event cho đối phương
                        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId,
                                        Map.of("type", "MESSAGES_READ", "readBy", userId.toString()));
                }
                return updated;
        }
}
