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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.chat.UserChatController;
import slib.com.example.dto.chat.ChatMessageDTO;
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
 * Unit Tests for FE-134: View history of chat
 * Test Report: doc/Report/UnitTestReport/FE117_TestReport.md
 */
@WebMvcTest(value = UserChatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-134: View history of chat - Unit Tests")
class FE134_ViewChatHistoryTest {

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
                user.setEmail("student@fpt.edu.vn");
                when(userService.getUserByEmail("student@fpt.edu.vn")).thenReturn(user);
        }

        // =========================================
        // === UTCID01: Normal - chat history with messages ===
        // =========================================
        @Test
        @WithMockUser(username = "student@fpt.edu.vn")
        @DisplayName("UTCID01: View chat history with messages returns 200 OK")
        void viewChatHistory_withMessages_returns200() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID otherUserId = UUID.randomUUID();
                mockCurrentUser(userId);

                ChatMessageDTO msg = new ChatMessageDTO();
                msg.setContent("Xin chao");
                Page<ChatMessageDTO> page = new PageImpl<>(List.of(msg));

                when(chatService.getChatHistory(eq(userId), eq(otherUserId), eq(0), eq(20)))
                                .thenReturn(page);

                mockMvc.perform(get("/slib/chat/history/{otherUserId}", otherUserId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].content").value("Xin chao"));
        }

        // =========================================
        // === UTCID02: Chat history with pagination ===
        // =========================================
        @Test
        @WithMockUser(username = "student@fpt.edu.vn")
        @DisplayName("UTCID02: View chat history with pagination returns 200 OK")
        void viewChatHistory_withPagination_returns200() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID otherUserId = UUID.randomUUID();
                mockCurrentUser(userId);

                Page<ChatMessageDTO> page = new PageImpl<>(Collections.emptyList());

                when(chatService.getChatHistory(eq(userId), eq(otherUserId), eq(2), eq(10)))
                                .thenReturn(page);

                mockMvc.perform(get("/slib/chat/history/{otherUserId}", otherUserId)
                                .param("page", "2")
                                .param("size", "10"))
                                .andExpect(status().isOk());

                verify(chatService, times(1)).getChatHistory(eq(userId), eq(otherUserId), eq(2), eq(10));
        }

        // =========================================
        // === UTCID03: Chat history empty ===
        // =========================================
        @Test
        @WithMockUser(username = "student@fpt.edu.vn")
        @DisplayName("UTCID03: View empty chat history returns 200 OK with empty content")
        void viewChatHistory_emptyConversation_returns200() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID otherUserId = UUID.randomUUID();
                mockCurrentUser(userId);

                Page<ChatMessageDTO> emptyPage = new PageImpl<>(Collections.emptyList());

                when(chatService.getChatHistory(eq(userId), eq(otherUserId), eq(0), eq(20)))
                                .thenReturn(emptyPage);

                mockMvc.perform(get("/slib/chat/history/{otherUserId}", otherUserId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(0));
        }

        // =========================================
        // === UTCID04: User not found ===
        // =========================================
        @Test
        @WithMockUser(username = "student@fpt.edu.vn")
        @DisplayName("UTCID04: View chat history when current user not found returns error")
        void viewChatHistory_userNotFound_returnsError() throws Exception {
                when(userService.getUserByEmail("student@fpt.edu.vn"))
                                .thenThrow(new RuntimeException("Nguoi dung khong ton tai"));

                UUID otherUserId = UUID.randomUUID();

                mockMvc.perform(get("/slib/chat/history/{otherUserId}", otherUserId))
                                .andExpect(status().isInternalServerError());
        }

        // =========================================
        // === UTCID05: Service failure ===
        // =========================================
        @Test
        @WithMockUser(username = "student@fpt.edu.vn")
        @DisplayName("UTCID05: View chat history when service fails returns error")
        void viewChatHistory_serviceFails_returnsError() throws Exception {
                UUID userId = UUID.randomUUID();
                UUID otherUserId = UUID.randomUUID();
                mockCurrentUser(userId);

                when(chatService.getChatHistory(eq(userId), eq(otherUserId), anyInt(), anyInt()))
                                .thenThrow(new RuntimeException("Loi truy van lich su chat"));

                mockMvc.perform(get("/slib/chat/history/{otherUserId}", otherUserId))
                                .andExpect(status().isInternalServerError());
        }
}
