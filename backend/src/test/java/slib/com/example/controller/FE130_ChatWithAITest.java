package slib.com.example.controller;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import slib.com.example.controller.ai.ChatController;
import slib.com.example.entity.ai.ChatMessageEntity;
import slib.com.example.entity.ai.ChatSessionEntity;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.ai.ChatService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ChatController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-130: Chat with AI virtual assistant - Unit Tests")
class FE130_ChatWithAITest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatController chatController;

    @MockBean
    private ChatService chatService;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(chatController, "aiServiceUrl", "http://mock-ai");
        ReflectionTestUtils.setField(chatController, "restTemplate", restTemplate);
    }

    private User buildStudentUser() {
        return User.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .email("student@fpt.edu.vn")
                .fullName("Student User")
                .role(Role.STUDENT)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("UTCID01: Proxy chat with valid AI service response")
    void proxyChat_withValidAiServiceResponse() throws Exception {
        when(restTemplate.exchange(
                eq("http://mock-ai/api/ai/chat"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"success\":true,\"reply\":\"Xin chào từ AI\"}"));

        mockMvc.perform(post("/slib/ai/proxy-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Xin chào"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.reply").value("Xin chào từ AI"));
    }

    @Test
    @DisplayName("UTCID02: Proxy chat when AI service is unavailable")
    void proxyChat_whenAiServiceIsUnavailable() throws Exception {
        when(restTemplate.exchange(
                eq("http://mock-ai/api/ai/chat"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(post("/slib/ai/proxy-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Xin chào"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.reply").value(org.hamcrest.Matchers.containsString("Không thể kết nối đến AI Service")));
    }

    @Test
    @DisplayName("UTCID03: View AI chat session detail with valid sessionId")
    void viewAiChatSessionDetail_withValidSessionId() throws Exception {
        User currentUser = buildStudentUser();
        ChatSessionEntity session = ChatSessionEntity.builder()
                .id(15L)
                .user(currentUser)
                .status(ChatSessionEntity.SessionStatus.ACTIVE)
                .title("How do I renew a book?")
                .createdAt(LocalDateTime.of(2026, 4, 9, 10, 0))
                .build();
        ChatMessageEntity message = ChatMessageEntity.builder()
                .id(21L)
                .session(session)
                .role(ChatMessageEntity.MessageRole.AI)
                .content("Bạn có thể gia hạn sách trong mục tài khoản.")
                .needsReview(false)
                .confidenceScore(0.96)
                .createdAt(LocalDateTime.of(2026, 4, 9, 10, 1))
                .build();

        when(chatService.getSessionDetail(15L)).thenReturn(Map.of(
                "session", session,
                "messages", List.of(message)));

        mockMvc.perform(get("/slib/ai/chat/session/{sessionId}", 15L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.id").value(15))
                .andExpect(jsonPath("$.session.userId").value(currentUser.getId().toString()))
                .andExpect(jsonPath("$.session.status").value("ACTIVE"))
                .andExpect(jsonPath("$.messages[0].id").value(21))
                .andExpect(jsonPath("$.messages[0].role").value("AI"));
    }

    @Test
    @DisplayName("UTCID04: View AI chat session detail with invalid sessionId")
    void viewAiChatSessionDetail_withInvalidSessionId() throws Exception {
        when(chatService.getSessionDetail(99L)).thenThrow(new RuntimeException("Session not found: 99"));

        mockMvc.perform(get("/slib/ai/chat/session/{sessionId}", 99L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Session not found: 99"));
    }

    @Test
    @DisplayName("UTCID05: Close AI chat session with valid sessionId")
    void closeAiChatSession_withValidSessionId() throws Exception {
        ChatSessionEntity session = ChatSessionEntity.builder()
                .id(16L)
                .status(ChatSessionEntity.SessionStatus.CLOSED)
                .title("Need help with overdue fine")
                .createdAt(LocalDateTime.of(2026, 4, 9, 9, 30))
                .closedAt(LocalDateTime.of(2026, 4, 9, 9, 45))
                .build();

        when(chatService.closeSession(16L)).thenReturn(session);

        mockMvc.perform(post("/slib/ai/chat/session/{sessionId}/close", 16L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã đóng phiên chat"))
                .andExpect(jsonPath("$.session.id").value(16))
                .andExpect(jsonPath("$.session.status").value("CLOSED"));
    }

    @Test
    @DisplayName("UTCID06: Librarian reply with blank content")
    void librarianReply_withBlankContent() throws Exception {
        mockMvc.perform(post("/slib/ai/admin/reply/{sessionId}", 17L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Nội dung phản hồi không được để trống"));
    }
}
