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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.ai.ChatController;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.ai.ChatService;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-46: Test AI Chat
 * Test Report: doc/Report/UnitTestReport/FE46_TestReport.md
 */
@WebMvcTest(value = ChatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-46: Test AI Chat - Unit Tests")
class FE46_TestAIChatTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ChatService chatService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Send message without authentication ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Send message without authentication returns 400 Bad Request")
        void sendMessage_noAuth_returns400() throws Exception {
                Map<String, Object> request = new HashMap<>();
                request.put("message", "Xin chao");
                request.put("sessionId", 1);

                // Without @AuthenticationPrincipal user, the controller receives null user
                // and the endpoint catches exception returning 400
                mockMvc.perform(post("/slib/ai/chat/message")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // =========================================
        // === UTCID02: Send valid message via proxy ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Send message via proxy endpoint returns 503 when AI service unreachable")
        void proxyChat_validMessage_returns503WhenAiUnavailable() throws Exception {
                Map<String, Object> request = new HashMap<>();
                request.put("message", "Xin chao AI");
                request.put("session_id", "test-session");

                // proxy-chat uses RestTemplate internally (new RestTemplate()), which is not mocked in WebMvcTest
                // The call will fail with connection error, returning SERVICE_UNAVAILABLE
                mockMvc.perform(post("/slib/ai/proxy-chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isServiceUnavailable())
                                .andExpect(jsonPath("$.success").value(false));
        }

        // =========================================
        // === UTCID03: Get session detail - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get session detail with valid sessionId returns 200 OK")
        void getSessionDetail_validId_returns200OK() throws Exception {
                Map<String, Object> mockDetail = new HashMap<>();
                mockDetail.put("session", Map.of("id", 1, "title", "Test session"));
                mockDetail.put("messages", java.util.Collections.emptyList());

                when(chatService.getSessionDetail(1L)).thenReturn(mockDetail);

                mockMvc.perform(get("/slib/ai/chat/session/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.session.id").value(1));

                verify(chatService, times(1)).getSessionDetail(1L);
        }

        // =========================================
        // === UTCID04: Send empty message ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Send empty message returns 400 Bad Request")
        void sendMessage_emptyMessage_returns400() throws Exception {
                Map<String, Object> request = new HashMap<>();
                request.put("message", "");
                request.put("sessionId", 1);

                // Without authenticated user, endpoint returns bad request
                mockMvc.perform(post("/slib/ai/chat/message")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // =========================================
        // === UTCID05: Get session detail - Invalid session ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get session detail with invalid sessionId returns 400")
        void getSessionDetail_invalidId_returns400() throws Exception {
                when(chatService.getSessionDetail(999L))
                                .thenThrow(new RuntimeException("Session not found: 999"));

                mockMvc.perform(get("/slib/ai/chat/session/999"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false));

                verify(chatService, times(1)).getSessionDetail(999L);
        }

        // =========================================
        // === UTCID06: Close session failure ===
        // =========================================

        @Test
        @DisplayName("UTCID06: Close session with runtime failure returns 400")
        void closeSession_failure_returns400() throws Exception {
                when(chatService.closeSession(999L))
                                .thenThrow(new RuntimeException("Session not found: 999"));

                mockMvc.perform(post("/slib/ai/chat/session/999/close"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false));

                verify(chatService, times(1)).closeSession(999L);
        }
}
