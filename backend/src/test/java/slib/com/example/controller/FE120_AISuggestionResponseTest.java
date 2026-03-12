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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.support.SupportRequestController;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.support.SupportRequestService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-120: AI Suggestion Response
 * Test Report: doc/Report/UnitTestReport/FE120_TestReport.md
 */
@WebMvcTest(value = SupportRequestController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-120: AI Suggestion Response - Unit Tests")
class FE120_AISuggestionResponseTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SupportRequestService supportRequestService;

        @MockBean
        private UserRepository userRepository;

        @Autowired
        private ObjectMapper objectMapper;

        private UUID mockLibrarian() {
                UUID librarianId = UUID.randomUUID();
                User librarian = new User();
                librarian.setId(librarianId);
                librarian.setEmail("librarian@fpt.edu.vn");
                when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(librarian));
                return librarianId;
        }

        // =========================================
        // === UTCID01: Normal - start chat for support request ===
        // =========================================
        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn")
        @DisplayName("UTCID01: Start chat for support request returns 200 OK with conversationId")
        void startChat_validRequest_returns200() throws Exception {
                UUID librarianId = mockLibrarian();
                UUID requestId = UUID.randomUUID();
                UUID conversationId = UUID.randomUUID();

                when(supportRequestService.startChatForRequest(eq(requestId), eq(librarianId)))
                                .thenReturn(conversationId);

                mockMvc.perform(post("/slib/support-requests/{id}/chat", requestId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()));

                verify(supportRequestService, times(1)).startChatForRequest(eq(requestId), eq(librarianId));
        }

        // =========================================
        // === UTCID02: Start chat reuses existing conversation ===
        // =========================================
        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn")
        @DisplayName("UTCID02: Start chat reuses existing conversation returns 200 OK")
        void startChat_reusesConversation_returns200() throws Exception {
                UUID librarianId = mockLibrarian();
                UUID requestId = UUID.randomUUID();
                UUID existingConversationId = UUID.randomUUID();

                when(supportRequestService.startChatForRequest(eq(requestId), eq(librarianId)))
                                .thenReturn(existingConversationId);

                mockMvc.perform(post("/slib/support-requests/{id}/chat", requestId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.conversationId").value(existingConversationId.toString()));
        }

        // =========================================
        // === UTCID03: Start chat creates new conversation ===
        // =========================================
        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn")
        @DisplayName("UTCID03: Start chat creates new conversation returns 200 OK")
        void startChat_createsNewConversation_returns200() throws Exception {
                UUID librarianId = mockLibrarian();
                UUID requestId = UUID.randomUUID();
                UUID newConversationId = UUID.randomUUID();

                when(supportRequestService.startChatForRequest(eq(requestId), eq(librarianId)))
                                .thenReturn(newConversationId);

                mockMvc.perform(post("/slib/support-requests/{id}/chat", requestId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.conversationId").exists());
        }

        // =========================================
        // === UTCID04: Support request not found ===
        // =========================================
        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn")
        @DisplayName("UTCID04: Start chat for non-existent support request returns 500 error")
        void startChat_requestNotFound_returns500() throws Exception {
                UUID librarianId = mockLibrarian();
                UUID requestId = UUID.randomUUID();

                when(supportRequestService.startChatForRequest(eq(requestId), eq(librarianId)))
                                .thenThrow(new RuntimeException("Yeu cau ho tro khong ton tai"));

                mockMvc.perform(post("/slib/support-requests/{id}/chat", requestId))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.error").value("Yeu cau ho tro khong ton tai"));
        }

        // =========================================
        // === UTCID05: Conversation service failure ===
        // =========================================
        @Test
        @WithMockUser(username = "librarian@fpt.edu.vn")
        @DisplayName("UTCID05: Start chat when conversation service fails returns 500 error")
        void startChat_serviceFails_returns500() throws Exception {
                UUID librarianId = mockLibrarian();
                UUID requestId = UUID.randomUUID();

                when(supportRequestService.startChatForRequest(eq(requestId), eq(librarianId)))
                                .thenThrow(new RuntimeException("Loi tao cuoc tro chuyen"));

                mockMvc.perform(post("/slib/support-requests/{id}/chat", requestId))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.error").value("Loi tao cuoc tro chuyen"));
        }
}
