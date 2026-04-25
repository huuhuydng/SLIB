package slib.com.example.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-136: View chat details
 * Test Report: doc/Report/UnitTestReport/FE119_TestReport.md
 */
@WebMvcTest(value = UserChatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-136: View chat details - Unit Tests")
class FE136_ViewChatDetailsTest {

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

        private void mockCurrentUser(UUID userId) {
                User user = new User();
                user.setId(userId);
                user.setEmail("librarian@fpt.edu.vn");
                when(userService.getUserByEmail("librarian@fpt.edu.vn")).thenReturn(user);
        }

        // =========================================
        // === UTCID01: Normal - conversation with messages ===
        // =========================================
        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn")
        @DisplayName("UTCID01: View chat details for conversation with messages returns 200 OK")
        void viewChatDetails_withMessages_returns200() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID conversationId = UUID.randomUUID();
                mockCurrentUser(userId);

                ChatMessageDTO msg = new ChatMessageDTO();
                msg.setContent("Xin chao");

                when(conversationService.getConversationMessagesForViewer(eq(conversationId), eq(userId)))
                                .thenReturn(List.of(msg));

                mockMvc.perform(get("/slib/chat/conversations/{conversationId}/messages", conversationId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].content").value("Xin chao"));

                verify(conversationService, times(1)).verifyConversationAccess(eq(conversationId), eq(userId));
                verify(conversationService, times(1)).getConversationMessagesForViewer(eq(conversationId), eq(userId));
        }

        // =========================================
        // === UTCID02: Conversation with multiple messages ===
        // =========================================
        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn")
        @DisplayName("UTCID02: View chat details with multiple messages returns 200 OK")
        void viewChatDetails_multipleMessages_returns200() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID conversationId = UUID.randomUUID();
                mockCurrentUser(userId);

                ChatMessageDTO msg1 = new ChatMessageDTO();
                msg1.setContent("Tin nhan 1");
                ChatMessageDTO msg2 = new ChatMessageDTO();
                msg2.setContent("Tin nhan 2");

                when(conversationService.getConversationMessagesForViewer(eq(conversationId), eq(userId)))
                                .thenReturn(List.of(msg1, msg2));

                mockMvc.perform(get("/slib/chat/conversations/{conversationId}/messages", conversationId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2));
        }

        // =========================================
        // === UTCID03: Conversation with no messages ===
        // =========================================
        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn")
        @DisplayName("UTCID03: View chat details with no messages returns empty list 200 OK")
        void viewChatDetails_noMessages_returns200() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID conversationId = UUID.randomUUID();
                mockCurrentUser(userId);

                when(conversationService.getConversationMessagesForViewer(eq(conversationId), eq(userId)))
                                .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/chat/conversations/{conversationId}/messages", conversationId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));
        }

        // =========================================
        // === UTCID04: Conversation not found ===
        // =========================================
        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn")
        @DisplayName("UTCID04: View chat details for non-existent conversation returns error")
        void viewChatDetails_notFound_returnsError() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID conversationId = UUID.randomUUID();
                mockCurrentUser(userId);

                when(conversationService.getConversationMessagesForViewer(eq(conversationId), eq(userId)))
                                .thenThrow(new RuntimeException("Cuoc tro chuyen khong ton tai"));

                mockMvc.perform(get("/slib/chat/conversations/{conversationId}/messages", conversationId))
                                .andExpect(status().isInternalServerError());
        }

        // =========================================
        // === UTCID05: Service failure ===
        // =========================================
        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn")
        @DisplayName("UTCID05: View chat details when service fails returns error")
        void viewChatDetails_serviceFails_returnsError() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID conversationId = UUID.randomUUID();
                mockCurrentUser(userId);

                when(conversationService.getConversationMessagesForViewer(eq(conversationId), eq(userId)))
                                .thenThrow(new RuntimeException("Loi truy van tin nhan"));

                mockMvc.perform(get("/slib/chat/conversations/{conversationId}/messages", conversationId))
                                .andExpect(status().isInternalServerError());
        }
}
