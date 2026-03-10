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
import slib.com.example.service.FeedbackService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-87: Create Feedback
 * Test Report: doc/Report/FE87_TestReport.md
 */
@WebMvcTest(value = FeedbackController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-87: Create Feedback - Unit Tests")
class FE87_CreateFeedbackTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private FeedbackService feedbackService;

        @Test
        @DisplayName("UTCD01: Create feedback returns 200 OK")
        void createFeedback_validToken_returns200OK() throws Exception {
                mockMvc.perform(post("/slib/feedbacks")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"rating\":5,\"comment\":\"Great service\"}"))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("UTCD02: Create feedback without token returns 401")
        void createFeedback_noToken_returns401() throws Exception {
                mockMvc.perform(post("/slib/feedbacks"))
                        .andExpect(status().isUnauthorized());
        }
}
