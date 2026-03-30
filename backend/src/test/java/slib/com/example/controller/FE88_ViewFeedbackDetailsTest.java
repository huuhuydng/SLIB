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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.feedback.FeedbackService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.feedback.FeedbackController;

/**
 * Unit Tests for FE-88: View Feedback Details
 * Test Report: doc/Report/UnitTestReport/FE88_TestReport.md
 */
@WebMvcTest(value = FeedbackController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-88: View Feedback Details - Unit Tests")
class FE88_ViewFeedbackDetailsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedbackService feedbackService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================
    // === UTCID01: Feedback exists with full detail fields ===
    // =========================================
    @Test
    @DisplayName("UTCID01: Feedback exists with full detail fields - returns 200 OK")
    void getAll_feedbackWithFullDetails_returns200() throws Exception {
        UUID feedbackId = UUID.randomUUID();
        FeedbackDTO dto = FeedbackDTO.builder()
                .id(feedbackId)
                .studentId(UUID.randomUUID())
                .studentName("Nguyen Van A")
                .rating(5)
                .content("Thu vien rat tot")
                .status("NEW")
                .createdAt(LocalDateTime.now())
                .build();
        when(feedbackService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/slib/feedbacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(feedbackId.toString()))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].content").value("Thu vien rat tot"))
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    // =========================================
    // === UTCID02: Feedback with reviewed info ===
    // =========================================
    @Test
    @DisplayName("UTCID02: Feedback with reviewed info - returns 200 OK")
    void getAll_feedbackWithReviewedInfo_returns200() throws Exception {
        FeedbackDTO dto = FeedbackDTO.builder()
                .id(UUID.randomUUID())
                .rating(4)
                .content("Can cai thien wifi")
                .status("REVIEWED")
                .reviewedByName("Thu Thu B")
                .reviewedAt(LocalDateTime.now())
                .build();
        when(feedbackService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/slib/feedbacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewedByName").value("Thu Thu B"))
                .andExpect(jsonPath("$[0].status").value("REVIEWED"));
    }

    // =========================================
    // === UTCID03: Feedback with category ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Feedback with category - returns 200 OK")
    void getAll_feedbackWithCategory_returns200() throws Exception {
        FeedbackDTO dto = FeedbackDTO.builder()
                .id(UUID.randomUUID())
                .rating(3)
                .content("Ghe hong nhieu")
                .category("FACILITY")
                .status("NEW")
                .build();
        when(feedbackService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/slib/feedbacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("FACILITY"));
    }

    // =========================================
    // === UTCID04: Empty feedback list ===
    // =========================================
    @Test
    @DisplayName("UTCID04: No feedbacks found - returns 200 OK with empty list")
    void getAll_noFeedbacks_returns200Empty() throws Exception {
        when(feedbackService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/slib/feedbacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =========================================
    // === UTCID05: Repository failure ===
    // =========================================
    @Test
    @DisplayName("UTCID05: Repository failure - returns 500 Internal Server Error")
    void getAll_repositoryFailure_returns500() throws Exception {
        when(feedbackService.getAll()).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/slib/feedbacks"))
                .andExpect(status().isInternalServerError());
    }
}
