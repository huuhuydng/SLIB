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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.dto.feedback.FeedbackDTO;
import slib.com.example.entity.feedback.FeedbackEntity.FeedbackStatus;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.feedback.FeedbackService;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.feedback.FeedbackController;

/**
 * Unit Tests for FE-91: View list of feedbacks
 * Test Report: doc/Report/UnitTestReport/FE87_TestReport.md
 */
@WebMvcTest(value = FeedbackController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-91: View list of feedbacks - Unit Tests")
class FE91_ViewFeedbacksTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedbackService feedbackService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================
    // === UTCID01: Get all feedbacks without filter ===
    // =========================================
    @Test
    @DisplayName("UTCID01: Get all feedbacks without status filter - returns 200 OK")
    void getAll_noFilter_returns200() throws Exception {
        List<FeedbackDTO> feedbacks = List.of(
                FeedbackDTO.builder().id(UUID.randomUUID()).rating(5).content("Rat tot").status("NEW").build(),
                FeedbackDTO.builder().id(UUID.randomUUID()).rating(3).content("Binh thuong").status("REVIEWED").build());
        when(feedbackService.getAll()).thenReturn(feedbacks);

        mockMvc.perform(get("/slib/feedbacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(feedbackService, times(1)).getAll();
        verify(feedbackService, never()).getByStatus(any());
    }

    // =========================================
    // === UTCID02: Filter by NEW status ===
    // =========================================
    @Test
    @DisplayName("UTCID02: Filter feedbacks by NEW status - returns 200 OK")
    void getAll_filterNew_returns200() throws Exception {
        List<FeedbackDTO> newList = List.of(
                FeedbackDTO.builder().id(UUID.randomUUID()).rating(4).status("NEW").build());
        when(feedbackService.getByStatus(FeedbackStatus.NEW)).thenReturn(newList);

        mockMvc.perform(get("/slib/feedbacks").param("status", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("NEW"));

        verify(feedbackService, times(1)).getByStatus(FeedbackStatus.NEW);
        verify(feedbackService, never()).getAll();
    }

    // =========================================
    // === UTCID03: Filter by REVIEWED status ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Filter feedbacks by REVIEWED status - returns 200 OK")
    void getAll_filterReviewed_returns200() throws Exception {
        List<FeedbackDTO> reviewedList = List.of(
                FeedbackDTO.builder().id(UUID.randomUUID()).rating(5).status("REVIEWED").build());
        when(feedbackService.getByStatus(FeedbackStatus.REVIEWED)).thenReturn(reviewedList);

        mockMvc.perform(get("/slib/feedbacks").param("status", "REVIEWED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REVIEWED"));

        verify(feedbackService, times(1)).getByStatus(FeedbackStatus.REVIEWED);
    }

    // =========================================
    // === UTCID04: Invalid status filter ===
    // =========================================
    @Test
    @DisplayName("UTCID04: Invalid status filter - returns 400 Bad Request")
    void getAll_invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/slib/feedbacks").param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest());

        verify(feedbackService, never()).getAll();
        verify(feedbackService, never()).getByStatus(any());
    }

    // =========================================
    // === UTCID05: Repository failure ===
    // =========================================
    @Test
    @DisplayName("UTCID05: Repository failure - returns 500 Internal Server Error")
    void getAll_repositoryFailure_returns500() throws Exception {
        when(feedbackService.getAll()).thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/slib/feedbacks"))
                .andExpect(status().isInternalServerError());
    }
}
