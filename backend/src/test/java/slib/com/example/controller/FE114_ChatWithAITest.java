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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.ChatService;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
        private ChatService chatService;

        // UTCD01: Valid message - Success
        @Test
        @DisplayName("UTCD01: Chat with AI with valid message returns 200 OK")
        void chatWithAI_validMessage_returns200OK() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("message", "Hello AI");

                when(chatService.sendToAI(anyString(), anyString())).thenReturn("AI response");

                mockMvc.perform(post("/slib/chat/ai")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"message\":\"Hello AI\"}"))
                        .andExpect(status().isOk());

                verify(chatService, times(1)).sendToAI(anyString(), anyString());
        }

        // UTCD02: No token - 401
        @Test
        @DisplayName("UTCD02: Chat with AI without token returns 401 Unauthorized")
        void chatWithAI_noToken_returns401() throws Exception {
                mockMvc.perform(post("/slib/chat/ai")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"message\":\"Hello\"}"))
                        .andExpect(status().isUnauthorized());
        }

        // UTCD04: Empty message - 400
        @Test
        @DisplayName("UTCD04: Chat with AI with empty message returns 400 Bad Request")
        void chatWithAI_emptyMessage_returns400() throws Exception {
                when(chatService.sendToAI(anyString(), anyString()))
                        .thenThrow(new RuntimeException("Message cannot be empty"));

                mockMvc.perform(post("/slib/chat/ai")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"message\":\"\"}"))
                        .andExpect(status().isBadRequest());
        }

        // UTCD06: System error - 500
        @Test
        @DisplayName("UTCD06: Chat with AI system error returns 500 Internal Server Error")
        void chatWithAI_systemError_returns500() throws Exception {
                when(chatService.sendToAI(anyString(), anyString()))
                        .thenThrow(new RuntimeException("AI service unavailable"));

                mockMvc.perform(post("/slib/chat/ai")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"message\":\"Hello\"}"))
                        .andExpect(status().isInternalServerError());
        }
}
