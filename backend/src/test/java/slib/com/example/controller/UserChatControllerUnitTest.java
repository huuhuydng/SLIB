package slib.com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.chat.UserChatController;
import slib.com.example.dto.chat.ChatMessageDTO;
import slib.com.example.dto.chat.ChatPartnerDTO;
import slib.com.example.dto.chat.ConversationDTO;
import slib.com.example.entity.chat.ConversationStatus;
import slib.com.example.entity.chat.MessageType;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.UserService;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.chat.ConversationService;
import slib.com.example.service.chat.UserChatService;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for UserChatController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 */
@WebMvcTest(value = UserChatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserChatController Unit Tests")
class UserChatControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserChatService chatService;

    @MockBean
    private ConversationService conversationService;

    @MockBean
    private UserService userService;

    @MockBean
    private CloudinaryService cloudinaryService;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID currentUserId;
    private UUID partnerUserId;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        partnerUserId = UUID.randomUUID();

        User currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setEmail("student@example.com");

        when(userService.getUserByEmail("user")).thenReturn(currentUser);
    }

    // =========================================
    // === CHAT HISTORY ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("getChatHistory_validParams_returns200WithPage")
    void getChatHistory_validParams_returns200WithPage() throws Exception {
        ChatMessageDTO msg = new ChatMessageDTO();
        msg.setId(UUID.randomUUID());
        msg.setSenderId(currentUserId);
        msg.setReceiverId(partnerUserId);
        msg.setContent("Xin chao");
        msg.setCreatedAt(LocalDateTime.now());

        Page<ChatMessageDTO> page = new PageImpl<>(List.of(msg));
        when(chatService.getChatHistory(eq(currentUserId), eq(partnerUserId), eq(0), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/slib/chat/history/{otherUserId}", partnerUserId)
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].content").value("Xin chao"));

        verify(chatService, times(1)).getChatHistory(currentUserId, partnerUserId, 0, 20);
    }

    @Test
    @WithMockUser(username = "user")
    @DisplayName("getChatHistory_emptyResult_returns200WithEmptyPage")
    void getChatHistory_emptyResult_returns200WithEmptyPage() throws Exception {
        Page<ChatMessageDTO> emptyPage = new PageImpl<>(List.of());
        when(chatService.getChatHistory(eq(currentUserId), eq(partnerUserId), eq(0), eq(20)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/slib/chat/history/{otherUserId}", partnerUserId)
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // =========================================
    // === CONVERSATIONS ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("getConversations_returns200WithPartnerList")
    void getConversations_returns200WithPartnerList() throws Exception {
        ChatPartnerDTO partner = new ChatPartnerDTO();
        partner.setId(partnerUserId);
        partner.setFullName("Thu Thu");

        when(chatService.getConversations(currentUserId)).thenReturn(List.of(partner));

        mockMvc.perform(get("/slib/chat/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Thu Thu"));

        verify(chatService, times(1)).getConversations(currentUserId);
    }

    @Test
    @WithMockUser(username = "user")
    @DisplayName("getConversations_noPartners_returns200WithEmptyArray")
    void getConversations_noPartners_returns200WithEmptyArray() throws Exception {
        when(chatService.getConversations(currentUserId)).thenReturn(List.of());

        mockMvc.perform(get("/slib/chat/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =========================================
    // === UNREAD COUNT ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("getUnreadCount_returns200WithCount")
    void getUnreadCount_returns200WithCount() throws Exception {
        when(chatService.getUnreadCount(currentUserId)).thenReturn(5L);

        mockMvc.perform(get("/slib/chat/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5));

        verify(chatService, times(1)).getUnreadCount(currentUserId);
    }

    @Test
    @WithMockUser(username = "user")
    @DisplayName("getUnreadCount_zeroUnread_returns200With0")
    void getUnreadCount_zeroUnread_returns200With0() throws Exception {
        when(chatService.getUnreadCount(currentUserId)).thenReturn(0L);

        mockMvc.perform(get("/slib/chat/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    // =========================================
    // === MARK AS READ ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("markAsRead_validPayload_returns200")
    void markAsRead_validPayload_returns200() throws Exception {
        Map<String, String> payload = Map.of("senderId", partnerUserId.toString());
        doNothing().when(chatService).markMessagesAsRead(currentUserId, partnerUserId);

        mockMvc.perform(post("/slib/chat/mark-read")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(chatService, times(1)).markMessagesAsRead(currentUserId, partnerUserId);
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/chat/seen/" + partnerUserId),
                anyMap());
    }

    // =========================================
    // === MEDIA ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("getMedia_imageType_returns200WithMediaList")
    void getMedia_imageType_returns200WithMediaList() throws Exception {
        ChatMessageDTO media = new ChatMessageDTO();
        media.setId(UUID.randomUUID());
        media.setAttachmentUrl("https://example.com/image.jpg");
        media.setType(MessageType.IMAGE);

        when(chatService.getConversationMedia(currentUserId, partnerUserId, "IMAGE"))
                .thenReturn(List.of(media));

        mockMvc.perform(get("/slib/chat/media/{partnerId}", partnerUserId)
                .param("type", "IMAGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("IMAGE"));

        verify(chatService, times(1)).getConversationMedia(currentUserId, partnerUserId, "IMAGE");
    }

    // =========================================
    // === WAITING CONVERSATIONS ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("getWaitingConversations_returns200WithList")
    void getWaitingConversations_returns200WithList() throws Exception {
        ConversationDTO conv = new ConversationDTO();
        conv.setId(UUID.randomUUID());
        conv.setStatus(ConversationStatus.QUEUE_WAITING);

        when(conversationService.getWaitingConversations()).thenReturn(List.of(conv));

        mockMvc.perform(get("/slib/chat/conversations/waiting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("QUEUE_WAITING"));

        verify(conversationService, times(1)).getWaitingConversations();
    }

    // =========================================
    // === ACTIVE CONVERSATIONS ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("getActiveConversations_returns200WithList")
    void getActiveConversations_returns200WithList() throws Exception {
        ConversationDTO conv = new ConversationDTO();
        conv.setId(UUID.randomUUID());
        conv.setStatus(ConversationStatus.HUMAN_CHATTING);

        when(conversationService.getActiveConversations(currentUserId)).thenReturn(List.of(conv));

        mockMvc.perform(get("/slib/chat/conversations/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("HUMAN_CHATTING"));

        verify(conversationService, times(1)).getActiveConversations(currentUserId);
    }

    // =========================================
    // === TAKE OVER CONVERSATION ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("takeOverConversation_validId_returns200")
    void takeOverConversation_validId_returns200() throws Exception {
        UUID conversationId = UUID.randomUUID();
        ConversationDTO result = new ConversationDTO();
        result.setId(conversationId);
        result.setStatus(ConversationStatus.HUMAN_CHATTING);

        when(conversationService.takeOverConversation(conversationId, currentUserId))
                .thenReturn(result);

        mockMvc.perform(post("/slib/chat/conversations/{conversationId}/take-over", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HUMAN_CHATTING"));

        verify(conversationService, times(1)).takeOverConversation(conversationId, currentUserId);
    }

    // =========================================
    // === RESOLVE CONVERSATION ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("resolveConversation_validId_returns200")
    void resolveConversation_validId_returns200() throws Exception {
        UUID conversationId = UUID.randomUUID();
        ConversationDTO result = new ConversationDTO();
        result.setId(conversationId);
        result.setStatus(ConversationStatus.RESOLVED);

        when(conversationService.resolveConversation(conversationId)).thenReturn(result);

        mockMvc.perform(post("/slib/chat/conversations/{conversationId}/resolve", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        verify(conversationService, times(1)).resolveConversation(conversationId);
    }

    // =========================================
    // === COUNT WAITING CONVERSATIONS ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("countWaitingConversations_returns200WithCount")
    void countWaitingConversations_returns200WithCount() throws Exception {
        when(conversationService.countWaitingConversations()).thenReturn(3L);

        mockMvc.perform(get("/slib/chat/conversations/waiting/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
    }

    // =========================================
    // === QUEUE POSITION ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("getQueuePosition_returns200WithPosition")
    void getQueuePosition_returns200WithPosition() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(conversationService.getQueuePosition(conversationId)).thenReturn(2);
        when(conversationService.countWaitingConversations()).thenReturn(5L);

        mockMvc.perform(get("/slib/chat/conversations/{conversationId}/queue-position", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(2))
                .andExpect(jsonPath("$.totalWaiting").value(5));
    }

    // =========================================
    // === CANCEL ESCALATION ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("cancelEscalation_validId_returns200")
    void cancelEscalation_validId_returns200() throws Exception {
        UUID conversationId = UUID.randomUUID();
        ConversationDTO result = new ConversationDTO();
        result.setId(conversationId);
        result.setStatus(ConversationStatus.AI_HANDLING);

        when(conversationService.cancelEscalation(conversationId)).thenReturn(result);

        mockMvc.perform(
                post("/slib/chat/conversations/{conversationId}/cancel-escalation", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AI_HANDLING"));
    }

    // =========================================
    // === SEND MESSAGE ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("sendMessage_validContent_returns200")
    void sendMessage_validContent_returns200() throws Exception {
        UUID conversationId = UUID.randomUUID();
        Map<String, String> request = Map.of(
                "content", "Hello librarian",
                "senderType", "STUDENT");

        ChatMessageDTO savedMsg = new ChatMessageDTO();
        savedMsg.setId(UUID.randomUUID());
        savedMsg.setContent("Hello librarian");

        when(conversationService.addMessageToConversation(eq(conversationId), eq(currentUserId),
                eq("Hello librarian"), eq("STUDENT")))
                .thenReturn(savedMsg);

        mockMvc.perform(post("/slib/chat/conversations/{conversationId}/messages", conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello librarian"));
    }

    // =========================================
    // === CONVERSATION MESSAGES ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("getConversationMessages_validId_returns200")
    void getConversationMessages_validId_returns200() throws Exception {
        UUID conversationId = UUID.randomUUID();
        ChatMessageDTO msg = new ChatMessageDTO();
        msg.setId(UUID.randomUUID());
        msg.setContent("Test message");

        when(conversationService.getConversationMessages(conversationId)).thenReturn(List.of(msg));

        mockMvc.perform(get("/slib/chat/conversations/{conversationId}/messages", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].content").value("Test message"));
    }

    // =========================================
    // === MY ACTIVE CONVERSATION ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("getMyActiveConversation_hasActive_returns200WithDetails")
    void getMyActiveConversation_hasActive_returns200WithDetails() throws Exception {
        ConversationDTO conv = new ConversationDTO();
        conv.setId(UUID.randomUUID());
        conv.setStatus(ConversationStatus.HUMAN_CHATTING);
        conv.setLibrarianName("Thu Thu");

        when(conversationService.getActiveConversationForStudent(currentUserId)).thenReturn(conv);

        mockMvc.perform(get("/slib/chat/conversations/my-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasActive").value(true))
                .andExpect(jsonPath("$.status").value("HUMAN_CHATTING"));
    }

    @Test
    @WithMockUser(username = "user")
    @DisplayName("getMyActiveConversation_noActive_returns200WithFalse")
    void getMyActiveConversation_noActive_returns200WithFalse() throws Exception {
        when(conversationService.getActiveConversationForStudent(currentUserId)).thenReturn(null);

        mockMvc.perform(get("/slib/chat/conversations/my-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasActive").value(false));
    }

    // =========================================
    // === CANCEL QUEUE ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("cancelQueue_validId_returns200WithSuccess")
    void cancelQueue_validId_returns200WithSuccess() throws Exception {
        UUID conversationId = UUID.randomUUID();
        doNothing().when(conversationService).cancelQueue(conversationId, currentUserId);

        mockMvc.perform(post("/slib/chat/conversations/{conversationId}/cancel-queue", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(conversationService, times(1)).cancelQueue(conversationId, currentUserId);
    }

    // =========================================
    // === ALL CONVERSATIONS ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("getAllConversations_returns200WithList")
    void getAllConversations_returns200WithList() throws Exception {
        ConversationDTO conv1 = new ConversationDTO();
        conv1.setId(UUID.randomUUID());
        conv1.setStatus(ConversationStatus.QUEUE_WAITING);
        ConversationDTO conv2 = new ConversationDTO();
        conv2.setId(UUID.randomUUID());
        conv2.setStatus(ConversationStatus.HUMAN_CHATTING);

        when(conversationService.getAllConversationsForLibrarian(currentUserId))
                .thenReturn(List.of(conv1, conv2));

        mockMvc.perform(get("/slib/chat/conversations/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // =========================================
    // === REQUEST LIBRARIAN ENDPOINT ===
    // =========================================

    @Test
    @WithMockUser(username = "user")
    @DisplayName("requestLibrarian_validRequest_returns200WithQueueInfo")
    void requestLibrarian_validRequest_returns200WithQueueInfo() throws Exception {
        UUID conversationId = UUID.randomUUID();
        ConversationDTO conv = new ConversationDTO();
        conv.setId(conversationId);
        conv.setStatus(ConversationStatus.QUEUE_WAITING);

        when(conversationService.createAndEscalateWithHistory(eq(currentUserId), anyString(), any(), any()))
                .thenReturn(conv);
        when(conversationService.getQueuePosition(conversationId)).thenReturn(1);
        when(conversationService.countWaitingConversations()).thenReturn(1L);

        Map<String, Object> request = Map.of("reason", "Can giup do");

        mockMvc.perform(post("/slib/chat/conversations/request-librarian")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.queuePosition").value(1))
                .andExpect(jsonPath("$.totalWaiting").value(1));
    }
}
