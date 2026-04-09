package slib.com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.chat.UserChatController;
import slib.com.example.dto.chat.ChatMessageDTO;
import slib.com.example.dto.chat.ConversationDTO;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.users.UserService;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.chat.ConversationService;
import slib.com.example.service.chat.UserChatService;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-122: Chat with Librarian
 * Test Report: doc/Report/UnitTestReport/FE114_TestReport.md
 */
@WebMvcTest(value = UserChatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-122: Chat with Librarian - Unit Tests")
class FE122_ChatWithLibrarianTest {

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

        private void mockCurrentUser(UUID userId) {
                User user = new User();
                user.setId(userId);
                user.setEmail("student@fpt.edu.vn");
                when(userService.getUserByEmail("student@fpt.edu.vn")).thenReturn(user);
        }

        // =========================================
        // === UTCID01: Normal - request librarian chat ===
        // =========================================
        @Test
        @WithMockUser(username = "student@fpt.edu.vn")
        @DisplayName("UTCID01: Request librarian chat returns 200 OK with conversationId")
        void requestLibrarian_validRequest_returns200() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID conversationId = UUID.randomUUID();
                mockCurrentUser(userId);

                ConversationDTO conversationDTO = new ConversationDTO();
                conversationDTO.setId(conversationId);

                when(conversationService.createAndEscalateWithHistory(eq(userId), anyString(), isNull(), isNull()))
                                .thenReturn(conversationDTO);
                when(conversationService.getQueuePosition(eq(conversationId))).thenReturn(1);
                when(conversationService.countWaitingConversations()).thenReturn(1L);

                mockMvc.perform(post("/slib/chat/conversations/request-librarian")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("reason", "Can ho tro"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.conversationId").exists());
        }

        // =========================================
        // === UTCID02: Request librarian with AI session ===
        // =========================================
        @Test
        @WithMockUser(username = "student@fpt.edu.vn")
        @DisplayName("UTCID02: Request librarian with AI session returns 200 OK")
        void requestLibrarian_withAiSession_returns200() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID conversationId = UUID.randomUUID();
                mockCurrentUser(userId);

                ConversationDTO conversationDTO = new ConversationDTO();
                conversationDTO.setId(conversationId);

                when(conversationService.createAndEscalateWithHistory(eq(userId), anyString(), isNull(), eq("ai-session-123")))
                                .thenReturn(conversationDTO);
                when(conversationService.getQueuePosition(eq(conversationId))).thenReturn(1);
                when(conversationService.countWaitingConversations()).thenReturn(1L);

                mockMvc.perform(post("/slib/chat/conversations/request-librarian")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("reason", "Can ho tro", "aiSessionId", "ai-session-123"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
        }

        // =========================================
        // === UTCID03: Send message to conversation ===
        // =========================================
        @Test
        @WithMockUser(username = "student@fpt.edu.vn")
        @DisplayName("UTCID03: Send message to conversation returns 200 OK")
        void sendMessage_validContent_returns200() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID conversationId = UUID.randomUUID();
                mockCurrentUser(userId);

                ChatMessageDTO responseMsg = new ChatMessageDTO();
                responseMsg.setId(UUID.randomUUID());
                responseMsg.setContent("Xin chao thu thu");

                when(conversationService.addMessageToConversation(eq(conversationId), eq(userId), eq("Xin chao thu thu"), eq("STUDENT")))
                                .thenReturn(responseMsg);

                mockMvc.perform(post("/slib/chat/conversations/{conversationId}/messages", conversationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("content", "Xin chao thu thu", "senderType", "STUDENT"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").value("Xin chao thu thu"));
        }

        // =========================================
        // === UTCID04: Request librarian without authentication ===
        // =========================================
        @Test
        @DisplayName("UTCID04: Request librarian without authentication returns error")
        void requestLibrarian_noAuth_returnsError() throws Exception {
                when(userService.getUserByEmail(isNull()))
                                .thenThrow(new RuntimeException("Vui long dang nhap"));

                mockMvc.perform(post("/slib/chat/conversations/request-librarian")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("reason", "Can ho tro"))))
                                .andExpect(status().isInternalServerError());
        }

        // =========================================
        // === UTCID05: Conversation service failure ===
        // =========================================
        @Test
        @WithMockUser(username = "student@fpt.edu.vn")
        @DisplayName("UTCID05: Request librarian when service fails returns error")
        void requestLibrarian_serviceFails_returnsError() throws Exception {
                UUID userId = UUID.randomUUID();
                mockCurrentUser(userId);

                when(conversationService.createAndEscalateWithHistory(eq(userId), anyString(), isNull(), isNull()))
                                .thenThrow(new RuntimeException("Loi tao cuoc tro chuyen"));

                mockMvc.perform(post("/slib/chat/conversations/request-librarian")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("reason", "Can ho tro"))))
                                .andExpect(status().isInternalServerError());
        }
}
