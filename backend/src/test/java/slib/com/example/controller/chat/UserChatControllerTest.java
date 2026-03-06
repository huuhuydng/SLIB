package slib.com.example.controller.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UserDetails;

import slib.com.example.entity.users.User;
import slib.com.example.dto.chat.ChatMessageDTO;
import slib.com.example.dto.chat.ChatPartnerDTO;
import slib.com.example.dto.chat.ConversationDTO;
import slib.com.example.entity.chat.Message;
import slib.com.example.entity.chat.Conversation;
import slib.com.example.entity.chat.ConversationStatus;
import slib.com.example.service.UserService;
import slib.com.example.service.chat.UserChatService;
import slib.com.example.service.chat.ConversationService;
import slib.com.example.service.chat.CloudinaryService;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserChatControllerTest {
    @InjectMocks
    private UserChatController userChatController;

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private UserChatService chatService;
    @Mock
    private ConversationService conversationService;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private UserService userService;
    @Mock
    private UserDetails userDetails;

    private final UUID myId = UUID.randomUUID();
    private final UUID partnerId = UUID.randomUUID();
    private final UUID messageId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(userDetails.getUsername()).thenReturn("test@example.com");
        
        // FIX LỖI: Sử dụng class User đã import, không dùng path đầy đủ bị sai
        User user = new User(); 
        user.setId(myId);
        
        when(userService.getUserByEmail(anyString())).thenReturn(user);
    }

    @Test
    void getChatHistory() {
        // ĐÃ XÓA dòng import nằm trong method (Java không cho phép điều này)
        Page<ChatMessageDTO> page = new PageImpl<>(List.of(new ChatMessageDTO()));
        when(chatService.getChatHistory(eq(myId), eq(partnerId), anyInt(), anyInt())).thenReturn(page);
        
        ResponseEntity<?> response = userChatController.getChatHistory(partnerId, 0, 20, userDetails);
        
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(page, response.getBody());
    }

    @Test
    void getConversations() {
        List<ChatPartnerDTO> partners = List.of(new ChatPartnerDTO());
        when(chatService.getConversations(myId)).thenReturn(partners);
        
        ResponseEntity<?> response = userChatController.getConversations(userDetails);
        
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(partners, response.getBody());
    }

    @Test
    void searchInConversation() {
        // 1. Chuẩn bị dữ liệu mẫu (Mock Entity)
        Message mockMsg = new Message();
        mockMsg.setId(UUID.randomUUID());
        mockMsg.setContent("hello world");
        // Giả lập Sender và Receiver vì Controller có gọi msg.getSender().getId()
        slib.com.example.entity.users.User sender = new slib.com.example.entity.users.User();
        sender.setId(myId);
        slib.com.example.entity.users.User receiver = new slib.com.example.entity.users.User();
        receiver.setId(partnerId);
        
        mockMsg.setSender(sender);
        mockMsg.setReceiver(receiver);

        List<Message> messages = List.of(mockMsg);

        // 2. Thiết lập Mock Service
        when(chatService.searchConversation(myId, partnerId, "hello")).thenReturn(messages);

        // 3. Thực thi gọi Controller
        ResponseEntity<List<ChatMessageDTO>> response = (ResponseEntity<List<ChatMessageDTO>>) 
                userChatController.searchInConversation(partnerId, "hello", userDetails);

        // 4. Kiểm tra (Assertions)
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("hello world", response.getBody().get(0).getContent());
    }

    @Test
    void findMessagePage() {
        when(chatService.getPageNumberOfMessage(myId, partnerId, messageId)).thenReturn(2);
        ResponseEntity<?> response = userChatController.findMessagePage(partnerId, messageId, userDetails);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody());
    }

    @Test
    void getUnreadCount() {
        when(chatService.getUnreadCount(myId)).thenReturn(5L);
        ResponseEntity<?> response = userChatController.getUnreadCount(userDetails);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(5L, response.getBody());
    }

    @Test
    void markAsRead() {
        Map<String, String> payload = Map.of("senderId", partnerId.toString());
        doNothing().when(chatService).markMessagesAsRead(myId, partnerId);
        
        ResponseEntity<?> response = userChatController.markAsRead(payload, userDetails);
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void getMedia() {
        List<ChatMessageDTO> mediaList = List.of(new ChatMessageDTO());
        when(chatService.getConversationMedia(myId, partnerId, "IMAGE")).thenReturn(mediaList);

        ResponseEntity<?> response = userChatController.getMedia(partnerId, "IMAGE", userDetails);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(mediaList, response.getBody());
    }

    // ==========================================
    // CONVERSATION MANAGEMENT TESTS
    // ==========================================

    @Test
    void getWaitingConversations() {
        ConversationDTO convDTO = ConversationDTO.builder()
                .id(UUID.randomUUID())
                .studentId(myId)
                .status(ConversationStatus.QUEUE_WAITING)
                .build();
        when(conversationService.getWaitingConversations()).thenReturn(List.of(convDTO));

        ResponseEntity<List<ConversationDTO>> response = userChatController.getWaitingConversations(userDetails);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(ConversationStatus.QUEUE_WAITING, response.getBody().get(0).getStatus());
    }

    @Test
    void getActiveConversations() {
        ConversationDTO convDTO = ConversationDTO.builder()
                .id(UUID.randomUUID())
                .studentId(myId)
                .status(ConversationStatus.HUMAN_CHATTING)
                .librarianId(myId)
                .build();
        when(conversationService.getActiveConversations(myId)).thenReturn(List.of(convDTO));

        ResponseEntity<List<ConversationDTO>> response = userChatController.getActiveConversations(userDetails);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(ConversationStatus.HUMAN_CHATTING, response.getBody().get(0).getStatus());
    }

    @Test
    void getAllConversations() {
        ConversationDTO convDTO = ConversationDTO.builder()
                .id(UUID.randomUUID())
                .studentId(myId)
                .status(ConversationStatus.QUEUE_WAITING)
                .build();
        when(conversationService.getAllConversationsForLibrarian(myId)).thenReturn(List.of(convDTO));

        ResponseEntity<List<ConversationDTO>> response = userChatController.getAllConversations(userDetails);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void takeOverConversation() {
        UUID conversationId = UUID.randomUUID();
        ConversationDTO convDTO = ConversationDTO.builder()
                .id(conversationId)
                .studentId(myId)
                .status(ConversationStatus.HUMAN_CHATTING)
                .librarianId(myId)
                .build();
        when(conversationService.takeOverConversation(conversationId, myId)).thenReturn(convDTO);

        ResponseEntity<ConversationDTO> response = userChatController.takeOverConversation(conversationId, userDetails);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(ConversationStatus.HUMAN_CHATTING, response.getBody().getStatus());
    }

    @Test
    void resolveConversation() {
        UUID conversationId = UUID.randomUUID();
        ConversationDTO convDTO = ConversationDTO.builder()
                .id(conversationId)
                .studentId(myId)
                .status(ConversationStatus.AI_HANDLING)
                .build();
        when(conversationService.resolveConversation(conversationId)).thenReturn(convDTO);

        ResponseEntity<ConversationDTO> response = userChatController.resolveConversation(conversationId, userDetails);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    void countWaitingConversations() {
        when(conversationService.countWaitingConversations()).thenReturn(5L);

        ResponseEntity<Long> response = userChatController.countWaitingConversations(userDetails);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(5L, response.getBody());
    }

    @Test
    void getQueuePosition() {
        UUID conversationId = UUID.randomUUID();
        when(conversationService.getQueuePosition(conversationId)).thenReturn(2);
        when(conversationService.countWaitingConversations()).thenReturn(5L);

        ResponseEntity<Map<String, Object>> response = userChatController.getQueuePosition(conversationId, userDetails);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().get("position"));
        assertEquals(5L, response.getBody().get("totalWaiting"));
    }

    @Test
    void cancelEscalation() {
        UUID conversationId = UUID.randomUUID();
        ConversationDTO convDTO = ConversationDTO.builder()
                .id(conversationId)
                .studentId(myId)
                .status(ConversationStatus.AI_HANDLING)
                .build();
        when(conversationService.cancelEscalation(conversationId)).thenReturn(convDTO);

        ResponseEntity<ConversationDTO> response = userChatController.cancelEscalation(conversationId, userDetails);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    void requestLibrarian() {
        ConversationDTO convDTO = ConversationDTO.builder()
                .id(UUID.randomUUID())
                .studentId(myId)
                .status(ConversationStatus.QUEUE_WAITING)
                .build();
        when(conversationService.createAndEscalateWithHistory(eq(myId), anyString(), any(), any())).thenReturn(convDTO);
        when(conversationService.getQueuePosition(any())).thenReturn(1);
        when(conversationService.countWaitingConversations()).thenReturn(1L);

        Map<String, Object> request = new java.util.HashMap<>();
        request.put("reason", "Need help");
        ResponseEntity<Map<String, Object>> response = userChatController.requestLibrarian(request, userDetails);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
    }

    @Test
    void getConversationMessages() {
        UUID conversationId = UUID.randomUUID();
        ChatMessageDTO messageDTO = ChatMessageDTO.builder()
                .id(UUID.randomUUID())
                .content("Hello")
                .build();
        when(conversationService.getConversationMessages(conversationId)).thenReturn(List.of(messageDTO));

        ResponseEntity<List<ChatMessageDTO>> response = userChatController.getConversationMessages(conversationId, userDetails);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Hello", response.getBody().get(0).getContent());
    }

    @Test
    void sendMessage() {
        UUID conversationId = UUID.randomUUID();
        ChatMessageDTO messageDTO = ChatMessageDTO.builder()
                .id(UUID.randomUUID())
                .content("Test message")
                .senderId(myId)
                .build();
        when(conversationService.addMessageToConversation(eq(conversationId), eq(myId), anyString(), anyString()))
                .thenReturn(messageDTO);

        Map<String, String> request = Map.of("content", "Test message", "senderType", "STUDENT");
        ResponseEntity<ChatMessageDTO> response = userChatController.sendMessage(conversationId, request, userDetails);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("Test message", response.getBody().getContent());
    }

    @Test
    void getConversationStatus() {
        UUID conversationId = UUID.randomUUID();
        ConversationDTO convDTO = ConversationDTO.builder()
                .id(conversationId)
                .status(ConversationStatus.QUEUE_WAITING)
                .librarianName(null)
                .build();

        when(conversationService.getConversationById(conversationId)).thenReturn(java.util.Optional.of(
                slib.com.example.entity.chat.Conversation.builder()
                        .id(conversationId)
                        .status(ConversationStatus.QUEUE_WAITING)
                        .student(new User())
                        .build()
        ));
        when(conversationService.convertToDTO(any())).thenReturn(convDTO);
        when(conversationService.getQueuePosition(conversationId)).thenReturn(1);

        ResponseEntity<Map<String, Object>> response = userChatController.getConversationStatus(conversationId, userDetails);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    void getMyActiveConversation() {
        ConversationDTO convDTO = ConversationDTO.builder()
                .id(UUID.randomUUID())
                .studentId(myId)
                .status(ConversationStatus.HUMAN_CHATTING)
                .librarianName("Test Librarian")
                .build();
        when(conversationService.getActiveConversationForStudent(myId)).thenReturn(convDTO);

        ResponseEntity<Map<String, Object>> response = userChatController.getMyActiveConversation(userDetails);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("hasActive"));
    }

    @Test
    void getMyActiveConversation_NoActive() {
        when(conversationService.getActiveConversationForStudent(myId)).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = userChatController.getMyActiveConversation(userDetails);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().get("hasActive"));
    }

    @Test
    void cancelQueue() {
        UUID conversationId = UUID.randomUUID();
        doNothing().when(conversationService).cancelQueue(conversationId, myId);

        ResponseEntity<Map<String, Object>> response = userChatController.cancelQueue(conversationId, userDetails);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
    }
}