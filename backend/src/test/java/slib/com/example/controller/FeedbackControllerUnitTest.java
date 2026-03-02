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
import slib.com.example.dto.feedback.FeedbackDTO;
import slib.com.example.entity.feedback.FeedbackEntity.FeedbackStatus;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.FeedbackService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FeedbackController
 */
@WebMvcTest(value = FeedbackController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FeedbackController Unit Tests")
class FeedbackControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedbackService feedbackService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User createMockUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        return user;
    }

    // =========================================
    // === GET ALL FEEDBACKS ===
    // =========================================

    @Test
    @DisplayName("getAll_noStatusFilter_returns200WithAllFeedbacks")
    void getAll_noStatusFilter_returns200WithAllFeedbacks() throws Exception {
        List<FeedbackDTO> feedbacks = Arrays.asList(
                FeedbackDTO.builder().id(UUID.randomUUID()).rating(5).status("NEW").build(),
                FeedbackDTO.builder().id(UUID.randomUUID()).rating(3).status("REVIEWED").build());

        when(feedbackService.getAll()).thenReturn(feedbacks);

        mockMvc.perform(get("/slib/feedbacks")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rating").value(5));

        verify(feedbackService).getAll();
    }

    @Test
    @DisplayName("getAll_withStatusFilter_returns200WithFilteredFeedbacks")
    void getAll_withStatusFilter_returns200WithFilteredFeedbacks() throws Exception {
        List<FeedbackDTO> feedbacks = Collections.singletonList(
                FeedbackDTO.builder().id(UUID.randomUUID()).rating(4).status("NEW").build());

        when(feedbackService.getByStatus(FeedbackStatus.NEW)).thenReturn(feedbacks);

        mockMvc.perform(get("/slib/feedbacks")
                .param("status", "NEW")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("NEW"));

        verify(feedbackService).getByStatus(FeedbackStatus.NEW);
    }

    @Test
    @DisplayName("getAll_invalidStatus_returns400")
    void getAll_invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/slib/feedbacks")
                .param("status", "INVALID")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === GET MY FEEDBACKS ===
    // =========================================

    @Test
    @DisplayName("getMyFeedbacks_authenticated_returns200")
    @WithMockUser(username = "test@example.com")
    void getMyFeedbacks_authenticated_returns200() throws Exception {
        User user = createMockUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(feedbackService.getByStudent(user.getId())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/slib/feedbacks/my")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // =========================================
    // === CREATE FEEDBACK ===
    // =========================================

    @Test
    @DisplayName("create_validData_returns201")
    @WithMockUser(username = "test@example.com")
    void create_validData_returns201() throws Exception {
        User user = createMockUser();
        FeedbackDTO result = FeedbackDTO.builder()
                .id(UUID.randomUUID())
                .rating(5)
                .content("Great service")
                .status("NEW")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(feedbackService.create(eq(user.getId()), eq(5), eq("Great service"))).thenReturn(result);

        mockMvc.perform(post("/slib/feedbacks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5,\"content\":\"Great service\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.content").value("Great service"));
    }

    @Test
    @DisplayName("create_invalidRating_returns400")
    @WithMockUser(username = "test@example.com")
    void create_invalidRating_returns400() throws Exception {
        User user = createMockUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/slib/feedbacks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":6,\"content\":\"Too high\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("create_missingRating_returns400")
    @WithMockUser(username = "test@example.com")
    void create_missingRating_returns400() throws Exception {
        User user = createMockUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/slib/feedbacks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"No rating\"}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === MARK REVIEWED ===
    // =========================================

    @Test
    @DisplayName("markReviewed_validId_returns200")
    @WithMockUser(username = "test@example.com")
    void markReviewed_validId_returns200() throws Exception {
        User user = createMockUser();
        UUID feedbackId = UUID.randomUUID();
        FeedbackDTO result = FeedbackDTO.builder()
                .id(feedbackId)
                .status("REVIEWED")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(feedbackService.markReviewed(eq(feedbackId), eq(user.getId()))).thenReturn(result);

        mockMvc.perform(put("/slib/feedbacks/{id}/review", feedbackId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEWED"));
    }

    // =========================================
    // === GET COUNT ===
    // =========================================

    @Test
    @DisplayName("getCount_returns200WithCounts")
    void getCount_returns200WithCounts() throws Exception {
        when(feedbackService.countAll()).thenReturn(20L);
        when(feedbackService.countByStatus(FeedbackStatus.NEW)).thenReturn(10L);
        when(feedbackService.countByStatus(FeedbackStatus.REVIEWED)).thenReturn(8L);
        when(feedbackService.countByStatus(FeedbackStatus.ACTED)).thenReturn(2L);

        mockMvc.perform(get("/slib/feedbacks/count")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(20))
                .andExpect(jsonPath("$.new").value(10))
                .andExpect(jsonPath("$.reviewed").value(8))
                .andExpect(jsonPath("$.acted").value(2));
    }

    // =========================================
    // === DELETE BATCH ===
    // =========================================

    @Test
    @DisplayName("deleteBatch_validIds_returns200")
    void deleteBatch_validIds_returns200() throws Exception {
        // Arrange
        List<UUID> ids = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        List<String> idStrings = ids.stream().map(UUID::toString).toList();

        doNothing().when(feedbackService).deleteBatch(ids);

        Map<String, List<String>> request = new HashMap<>();
        request.put("ids", idStrings);

        // Act & Assert
        mockMvc.perform(delete("/slib/feedbacks/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(2));

        verify(feedbackService, times(1)).deleteBatch(ids);
    }

    @Test
    @DisplayName("deleteBatch_emptyIds_returns400")
    void deleteBatch_emptyIds_returns400() throws Exception {
        // Arrange
        Map<String, List<String>> request = new HashMap<>();
        request.put("ids", List.of());

        // Act & Assert
        mockMvc.perform(delete("/slib/feedbacks/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Danh sách ID không được trống"));

        verify(feedbackService, never()).deleteBatch(anyList());
    }

    @Test
    @DisplayName("deleteBatch_nullIds_returns400")
    void deleteBatch_nullIds_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/slib/feedbacks/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Danh sách ID không được trống"));

        verify(feedbackService, never()).deleteBatch(anyList());
    }
}
