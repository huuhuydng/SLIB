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
import slib.com.example.entity.chat.Message;
import slib.com.example.service.UserService;
import slib.com.example.service.chat.UserChatService;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
}