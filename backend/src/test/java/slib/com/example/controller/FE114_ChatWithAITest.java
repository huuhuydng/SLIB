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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.UserService;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.chat.ConversationService;
import slib.com.example.service.chat.UserChatService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.chat.UserChatController;

/**
 * Unit Tests for FE-114: Chat with AI
 * Test Report: doc/Report/FE114_TestReport.md
 */
@WebMvcTest(value = UserChatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-114: Chat with AI - Unit Tests")
class FE114_ChatWithAITest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SimpMessagingTemplate messagingTemplate;

        @MockBean
        private UserChatService chatService;

        @MockBean
        private ConversationService conversationService;

        @MockBean
        private UserService userService;

        @MockBean
        private CloudinaryService cloudinaryService;

        // UTCD01: GET conversations without auth returns 500 (RuntimeException from null UserDetails)
        @Test
        @DisplayName("UTCD01: Access chat conversations endpoint returns error without auth principal")
        void chatConversations_noAuthPrincipal_returnsError() throws Exception {
                mockMvc.perform(get("/slib/chat/conversations"))
                        .andExpect(status().isInternalServerError());
        }

        // UTCD02: GET unread count without auth returns 500
        @Test
        @DisplayName("UTCD02: Access unread count endpoint returns error without auth principal")
        void chatUnreadCount_noAuthPrincipal_returnsError() throws Exception {
                mockMvc.perform(get("/slib/chat/unread-count"))
                        .andExpect(status().isInternalServerError());
        }

        // UTCD03: GET waiting conversations endpoint exists and is reachable
        @Test
        @DisplayName("UTCD03: Access waiting conversations endpoint returns error without auth principal")
        void chatWaitingConversations_noAuthPrincipal_returnsError() throws Exception {
                mockMvc.perform(get("/slib/chat/conversations/waiting"))
                        .andExpect(status().isInternalServerError());
        }

        // UTCD04: POST request-librarian without auth returns error
        @Test
        @DisplayName("UTCD04: Request librarian endpoint returns error without auth principal")
        void requestLibrarian_noAuthPrincipal_returnsError() throws Exception {
                mockMvc.perform(post("/slib/chat/conversations/request-librarian")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Test\"}"))
                        .andExpect(status().isInternalServerError());
        }
}
