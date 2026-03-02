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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.ai.ChatController;
import slib.com.example.entity.ai.ChatMessageEntity;
import slib.com.example.entity.ai.ChatSessionEntity;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.ai.ChatService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for ChatController
 */
@WebMvcTest(value = ChatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ChatController Unit Tests")
class ChatControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

    private User createMockUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@fpt.edu.vn");
        return user;
    }

    private void setSecurityContext(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // =========================================
    // === PROXY CHAT (No Auth) ===
    // =========================================

    @Test
    @DisplayName("proxyChat_aiServiceUnavailable_returns503")
    void proxyChat_aiServiceUnavailable_returns503() throws Exception {
        // AI service not available in test env, expects 503
        Map<String, Object> request = Map.of(
                "message", "Hello AI",
                "sessionId", 1L);

        mockMvc.perform(post("/slib/ai/proxy-chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================
    // === SEND MESSAGE (Student) ===
    // =========================================

    @Test
    @DisplayName("sendMessage_validMessage_returns200WithResponse")
    void sendMessage_validMessage_returns200WithResponse() throws Exception {
        User user = createMockUser();
        setSecurityContext(user);

        ChatService.ChatResponse response = new ChatService.ChatResponse(
                1L, "AI response", false, "ACTIVE");

        when(chatService.sendMessage(any(User.class), any(), anyString()))
                .thenReturn(response);

        mockMvc.perform(post("/slib/ai/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"Hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.sessionId").value(1))
                .andExpect(jsonPath("$.reply").value("AI response"));
    }

    @Test
    @DisplayName("sendMessage_emptyMessage_returns400")
    void sendMessage_emptyMessage_returns400() throws Exception {
        User user = createMockUser();
        setSecurityContext(user);

        mockMvc.perform(post("/slib/ai/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tin nhắn không được để trống"));
    }

    @Test
    @DisplayName("sendMessage_nullMessage_returns400")
    void sendMessage_nullMessage_returns400() throws Exception {
        User user = createMockUser();
        setSecurityContext(user);

        mockMvc.perform(post("/slib/ai/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================
    // === GET USER SESSIONS (Student) ===
    // =========================================

    @Test
    @DisplayName("getUserSessions_returns200WithSessions")
    void getUserSessions_returns200WithSessions() throws Exception {
        User user = createMockUser();
        setSecurityContext(user);

        ChatSessionEntity session = new ChatSessionEntity();
        session.setId(1L);
        session.setTitle("Test session");

        when(chatService.getUserSessions(any(UUID.class)))
                .thenReturn(List.of(session));

        mockMvc.perform(get("/slib/ai/chat/sessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test session"));
    }

    // =========================================
    // === GET SESSION DETAIL (Student) ===
    // =========================================

    @Test
    @DisplayName("getSessionDetail_validSession_returns200WithDetail")
    void getSessionDetail_validSession_returns200WithDetail() throws Exception {
        ChatSessionEntity session = new ChatSessionEntity();
        session.setId(1L);
        session.setTitle("Test");

        when(chatService.getSessionDetail(1L))
                .thenReturn(Map.of("session", session, "messages", List.of()));

        mockMvc.perform(get("/slib/ai/chat/session/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.id").value(1));
    }

    @Test
    @DisplayName("getSessionDetail_invalidSession_returns400")
    void getSessionDetail_invalidSession_returns400() throws Exception {
        when(chatService.getSessionDetail(999L))
                .thenThrow(new RuntimeException("Session not found: 999"));

        mockMvc.perform(get("/slib/ai/chat/session/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================
    // === CLOSE SESSION (Student) ===
    // =========================================

    @Test
    @DisplayName("closeSession_validSession_returns200")
    void closeSession_validSession_returns200() throws Exception {
        ChatSessionEntity session = new ChatSessionEntity();
        session.setId(1L);
        session.setStatus(ChatSessionEntity.SessionStatus.CLOSED);

        when(chatService.closeSession(1L)).thenReturn(session);

        mockMvc.perform(post("/slib/ai/chat/session/1/close")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã đóng phiên chat"));
    }

    @Test
    @DisplayName("closeSession_invalidSession_returns400")
    void closeSession_invalidSession_returns400() throws Exception {
        when(chatService.closeSession(999L))
                .thenThrow(new RuntimeException("Session not found: 999"));

        mockMvc.perform(post("/slib/ai/chat/session/999/close")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================
    // === GET ESCALATED SESSIONS (Librarian) ===
    // =========================================

    @Test
    @DisplayName("getEscalatedSessions_returns200WithSessions")
    void getEscalatedSessions_returns200WithSessions() throws Exception {
        ChatSessionEntity session = new ChatSessionEntity();
        session.setId(1L);
        session.setStatus(ChatSessionEntity.SessionStatus.ESCALATED);

        when(chatService.getEscalatedSessions())
                .thenReturn(List.of(session));

        mockMvc.perform(get("/slib/ai/admin/escalated")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("ESCALATED"));
    }

    // =========================================
    // === LIBRARIAN REPLY (Librarian) ===
    // =========================================

    @Test
    @DisplayName("librarianReply_validContent_returns200")
    void librarianReply_validContent_returns200() throws Exception {
        ChatMessageEntity message = new ChatMessageEntity();
        message.setId(1L);
        message.setContent("Librarian response");

        when(chatService.librarianReply(eq(1L), anyString()))
                .thenReturn(message);

        mockMvc.perform(post("/slib/ai/admin/reply/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Librarian response\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã gửi phản hồi"));
    }

    @Test
    @DisplayName("librarianReply_emptyContent_returns400")
    void librarianReply_emptyContent_returns400() throws Exception {
        mockMvc.perform(post("/slib/ai/admin/reply/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Nội dung phản hồi không được để trống"));
    }

    @Test
    @DisplayName("librarianReply_nullContent_returns400")
    void librarianReply_nullContent_returns400() throws Exception {
        mockMvc.perform(post("/slib/ai/admin/reply/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("librarianReply_invalidSession_returns400")
    void librarianReply_invalidSession_returns400() throws Exception {
        when(chatService.librarianReply(eq(999L), anyString()))
                .thenThrow(new RuntimeException("Session not found: 999"));

        mockMvc.perform(post("/slib/ai/admin/reply/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================
    // === RESOLVE SESSION (Librarian) ===
    // =========================================

    @Test
    @DisplayName("resolveSession_validSession_returns200")
    void resolveSession_validSession_returns200() throws Exception {
        ChatSessionEntity session = new ChatSessionEntity();
        session.setId(1L);
        session.setStatus(ChatSessionEntity.SessionStatus.ACTIVE);

        when(chatService.resolveSession(1L)).thenReturn(session);

        mockMvc.perform(put("/slib/ai/admin/resolve/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã xử lý xong phiên chat"));
    }

    @Test
    @DisplayName("resolveSession_invalidSession_returns400")
    void resolveSession_invalidSession_returns400() throws Exception {
        when(chatService.resolveSession(999L))
                .thenThrow(new RuntimeException("Session not found: 999"));

        mockMvc.perform(put("/slib/ai/admin/resolve/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================
    // === GET SESSION FOR ADMIN (Librarian) ===
    // =========================================

    @Test
    @DisplayName("getSessionForAdmin_validSession_returns200")
    void getSessionForAdmin_validSession_returns200() throws Exception {
        ChatSessionEntity session = new ChatSessionEntity();
        session.setId(1L);

        when(chatService.getSessionDetail(1L))
                .thenReturn(Map.of("session", session, "messages", List.of()));

        mockMvc.perform(get("/slib/ai/admin/session/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.id").value(1));
    }

    @Test
    @DisplayName("getSessionForAdmin_invalidSession_returns400")
    void getSessionForAdmin_invalidSession_returns400() throws Exception {
        when(chatService.getSessionDetail(999L))
                .thenThrow(new RuntimeException("Session not found: 999"));

        mockMvc.perform(get("/slib/ai/admin/session/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
