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
import slib.com.example.dto.chat.ChatPartnerDTO;
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
 * Unit Tests for FE-135: View Chat List
 * Test Report: doc/Report/UnitTestReport/FE135_TestReport.md
 */
@WebMvcTest(value = UserChatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-135: View Chat List - Unit Tests")
class FE135_ViewChatListTest {

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
        // === UTCID01: Normal - conversations with partners ===
        // =========================================
        @Test
        @WithMockUser(username = "student@fpt.edu.vn")
        @DisplayName("UTCID01: View chat list with conversations returns 200 OK")
        void viewChatList_withConversations_returns200() throws Exception {
                UUID userId = UUID.randomUUID();
                mockCurrentUser(userId);

                ChatPartnerDTO partner = new ChatPartnerDTO();
                partner.setFullName("Thu Thu Nguyen");
                partner.setUnreadCount(3L);

                when(chatService.getConversations(eq(userId))).thenReturn(List.of(partner));

                mockMvc.perform(get("/slib/chat/conversations"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].fullName").value("Thu Thu Nguyen"))
                                .andExpect(jsonPath("$[0].unreadCount").value(3));
        }

        // =========================================
        // === UTCID02: Multiple conversation partners ===
        // =========================================
        @Test
        @WithMockUser(username = "student@fpt.edu.vn")
        @DisplayName("UTCID02: View chat list with multiple partners returns 200 OK")
        void viewChatList_multiplePartners_returns200() throws Exception {
                UUID userId = UUID.randomUUID();
                mockCurrentUser(userId);

                ChatPartnerDTO p1 = new ChatPartnerDTO();
                p1.setFullName("Thu thu A");
                ChatPartnerDTO p2 = new ChatPartnerDTO();
                p2.setFullName("Thu thu B");

                when(chatService.getConversations(eq(userId))).thenReturn(List.of(p1, p2));

                mockMvc.perform(get("/slib/chat/conversations"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2));
        }

        // =========================================
        // === UTCID03: Conversations with unread counts ===
        // =========================================
        @Test
        @WithMockUser(username = "student@fpt.edu.vn")
        @DisplayName("UTCID03: View chat list with unread counts returns 200 OK")
        void viewChatList_withUnreadCounts_returns200() throws Exception {
                UUID userId = UUID.randomUUID();
                mockCurrentUser(userId);

                ChatPartnerDTO partner = new ChatPartnerDTO();
                partner.setFullName("Thu thu C");
                partner.setUnreadCount(0L);

                when(chatService.getConversations(eq(userId))).thenReturn(List.of(partner));

                mockMvc.perform(get("/slib/chat/conversations"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].unreadCount").value(0));
        }

        // =========================================
        // === UTCID04: Empty conversation list ===
        // =========================================
        @Test
        @WithMockUser(username = "student@fpt.edu.vn")
        @DisplayName("UTCID04: View chat list with no conversations returns empty list 200 OK")
        void viewChatList_emptyList_returns200() throws Exception {
                UUID userId = UUID.randomUUID();
                mockCurrentUser(userId);

                when(chatService.getConversations(eq(userId))).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/chat/conversations"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));
        }

        // =========================================
        // === UTCID05: Service failure ===
        // =========================================
        @Test
        @WithMockUser(username = "student@fpt.edu.vn")
        @DisplayName("UTCID05: View chat list when service fails returns error")
        void viewChatList_serviceFails_returnsError() throws Exception {
                UUID userId = UUID.randomUUID();
                mockCurrentUser(userId);

                when(chatService.getConversations(eq(userId)))
                                .thenThrow(new RuntimeException("Loi truy van danh sach chat"));

                mockMvc.perform(get("/slib/chat/conversations"))
                                .andExpect(status().isInternalServerError());
        }
}
