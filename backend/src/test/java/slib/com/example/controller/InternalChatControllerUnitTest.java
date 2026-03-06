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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.chat.InternalChatController;
import slib.com.example.dto.chat.AIReplyRequest;
import slib.com.example.dto.chat.ConversationDTO;
import slib.com.example.dto.chat.EscalateRequest;
import slib.com.example.entity.chat.Conversation;
import slib.com.example.entity.chat.Message;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.UserRepository;
import slib.com.example.repository.chat.MessageRepository;
import slib.com.example.service.chat.ConversationService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for InternalChatController
 */
@WebMvcTest(value = InternalChatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("InternalChatController Unit Tests")
class InternalChatControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConversationService conversationService;

    @MockBean
    private MessageRepository messageRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================
    // === HEALTH CHECK ===
    // =========================================

    @Test
    @DisplayName("healthCheck_returns200WithStatus")
    void healthCheck_returns200WithStatus() throws Exception {
        mockMvc.perform(get("/internal/chat/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.service").value("internal-chat-api"));
    }

    // =========================================
    // === AI REPLY ===
    // =========================================

    @Test
    @DisplayName("aiReply_invalidApiKey_returns401")
    void aiReply_invalidApiKey_returns401() throws Exception {
        AIReplyRequest request = new AIReplyRequest();
        request.setStudentId(UUID.randomUUID());
        request.setContent("Hello");

        mockMvc.perform(post("/internal/chat/reply")
                .header("X-API-Key", "wrong-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid API Key"));
    }

    @Test
    @DisplayName("aiReply_validApiKey_returns200")
    void aiReply_validApiKey_returns200() throws Exception {
        UUID studentId = UUID.randomUUID();

        User botUser = new User();
        botUser.setId(UUID.randomUUID());
        botUser.setEmail("ai-bot@slib.system");

        User student = new User();
        student.setId(studentId);

        Message savedMessage = Message.builder()
                .id(UUID.randomUUID())
                .sender(botUser)
                .receiver(student)
                .content("AI response")
                .build();

        when(userRepository.findByEmail("ai-bot@slib.system")).thenReturn(Optional.of(botUser));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        AIReplyRequest request = new AIReplyRequest();
        request.setStudentId(studentId);
        request.setContent("AI response");

        mockMvc.perform(post("/internal/chat/reply")
                .header("X-API-Key", "default-internal-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================
    // === ESCALATE ===
    // =========================================

    @Test
    @DisplayName("escalate_invalidApiKey_returns401")
    void escalate_invalidApiKey_returns401() throws Exception {
        EscalateRequest request = new EscalateRequest();
        request.setConversationId(UUID.randomUUID());
        request.setReason("Need human help");

        mockMvc.perform(post("/internal/chat/escalate")
                .header("X-API-Key", "wrong-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid API Key"));
    }

    @Test
    @DisplayName("escalate_validRequest_returns200")
    void escalate_validRequest_returns200() throws Exception {
        UUID conversationId = UUID.randomUUID();

        ConversationDTO dto = new ConversationDTO();
        when(conversationService.escalateToHuman(conversationId, "Need human help")).thenReturn(dto);

        EscalateRequest request = new EscalateRequest();
        request.setConversationId(conversationId);
        request.setReason("Need human help");

        mockMvc.perform(post("/internal/chat/escalate")
                .header("X-API-Key", "default-internal-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("escalate_missingIds_returns400")
    void escalate_missingIds_returns400() throws Exception {
        EscalateRequest request = new EscalateRequest();
        // No conversationId or studentId
        request.setReason("Need help");

        mockMvc.perform(post("/internal/chat/escalate")
                .header("X-API-Key", "default-internal-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
