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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.support.SupportRequestController;
import slib.com.example.dto.support.SupportRequestDTO;
import slib.com.example.entity.support.SupportRequestStatus;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.support.SupportRequestService;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for SupportRequestController
 */
@WebMvcTest(value = SupportRequestController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SupportRequestController Unit Tests")
class SupportRequestControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SupportRequestService supportRequestService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User createMockUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("librarian@test.com");
        return user;
    }

    private SupportRequestDTO createMockDTO() {
        return SupportRequestDTO.builder()
                .id(UUID.randomUUID())
                .description("Test support request")
                .status(SupportRequestStatus.PENDING)
                .build();
    }

    // =========================================
    // === GET ALL ===
    // =========================================

    @Test
    @DisplayName("getAll_noFilter_returns200WithList")
    void getAll_noFilter_returns200WithList() throws Exception {
        List<SupportRequestDTO> list = List.of(createMockDTO());
        when(supportRequestService.getAll()).thenReturn(list);

        mockMvc.perform(get("/slib/support-requests")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("getAll_withStatusFilter_returns200WithFilteredList")
    void getAll_withStatusFilter_returns200WithFilteredList() throws Exception {
        List<SupportRequestDTO> list = List.of(createMockDTO());
        when(supportRequestService.getByStatus(SupportRequestStatus.PENDING)).thenReturn(list);

        mockMvc.perform(get("/slib/support-requests")
                .param("status", "PENDING")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(supportRequestService).getByStatus(SupportRequestStatus.PENDING);
    }

    @Test
    @DisplayName("getAll_invalidStatus_returns400")
    void getAll_invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/slib/support-requests")
                .param("status", "INVALID_STATUS")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === GET COUNT ===
    // =========================================

    @Test
    @DisplayName("getCount_returns200WithCounts")
    void getCount_returns200WithCounts() throws Exception {
        when(supportRequestService.countByStatus(SupportRequestStatus.PENDING)).thenReturn(5L);
        when(supportRequestService.countByStatus(SupportRequestStatus.IN_PROGRESS)).thenReturn(3L);
        when(supportRequestService.countByStatus(SupportRequestStatus.RESOLVED)).thenReturn(10L);
        when(supportRequestService.countByStatus(SupportRequestStatus.REJECTED)).thenReturn(2L);

        mockMvc.perform(get("/slib/support-requests/count")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(5))
                .andExpect(jsonPath("$.inProgress").value(3))
                .andExpect(jsonPath("$.resolved").value(10))
                .andExpect(jsonPath("$.rejected").value(2));
    }

    // =========================================
    // === GET MY REQUESTS ===
    // =========================================

    @Test
    @WithMockUser(username = "student@test.com")
    @DisplayName("getMyRequests_authenticated_returns200")
    void getMyRequests_authenticated_returns200() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("student@test.com");
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(user));

        List<SupportRequestDTO> list = List.of(createMockDTO());
        when(supportRequestService.getByStudent(user.getId())).thenReturn(list);

        mockMvc.perform(get("/slib/support-requests/my")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // =========================================
    // === UPDATE STATUS ===
    // =========================================

    @Test
    @WithMockUser(username = "librarian@test.com")
    @DisplayName("updateStatus_validStatus_returns200")
    void updateStatus_validStatus_returns200() throws Exception {
        UUID requestId = UUID.randomUUID();
        User user = createMockUser();
        when(userRepository.findByEmail("librarian@test.com")).thenReturn(Optional.of(user));

        SupportRequestDTO dto = createMockDTO();
        dto.setStatus(SupportRequestStatus.IN_PROGRESS);
        when(supportRequestService.updateStatus(eq(requestId), eq(SupportRequestStatus.IN_PROGRESS), eq(user.getId())))
                .thenReturn(dto);

        mockMvc.perform(put("/slib/support-requests/{id}/status", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "librarian@test.com")
    @DisplayName("updateStatus_missingStatus_returns400")
    void updateStatus_missingStatus_returns400() throws Exception {
        UUID requestId = UUID.randomUUID();
        User user = createMockUser();
        when(userRepository.findByEmail("librarian@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(put("/slib/support-requests/{id}/status", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("other", "value"))))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === RESPOND ===
    // =========================================

    @Test
    @WithMockUser(username = "librarian@test.com")
    @DisplayName("respond_validResponse_returns200")
    void respond_validResponse_returns200() throws Exception {
        UUID requestId = UUID.randomUUID();
        User user = createMockUser();
        when(userRepository.findByEmail("librarian@test.com")).thenReturn(Optional.of(user));

        SupportRequestDTO dto = createMockDTO();
        when(supportRequestService.respond(eq(requestId), eq("Response text"), eq(user.getId())))
                .thenReturn(dto);

        mockMvc.perform(put("/slib/support-requests/{id}/respond", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("response", "Response text"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "librarian@test.com")
    @DisplayName("respond_blankResponse_returns400")
    void respond_blankResponse_returns400() throws Exception {
        UUID requestId = UUID.randomUUID();
        User user = createMockUser();
        when(userRepository.findByEmail("librarian@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(put("/slib/support-requests/{id}/respond", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("response", ""))))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === START CHAT ===
    // =========================================

    @Test
    @WithMockUser(username = "librarian@test.com")
    @DisplayName("startChat_validRequest_returns200WithConversationId")
    void startChat_validRequest_returns200WithConversationId() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        User user = createMockUser();
        when(userRepository.findByEmail("librarian@test.com")).thenReturn(Optional.of(user));
        when(supportRequestService.startChatForRequest(requestId, user.getId())).thenReturn(conversationId);

        mockMvc.perform(post("/slib/support-requests/{id}/chat", requestId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()));
    }

    // =========================================
    // === CREATE SUPPORT REQUEST ===
    // =========================================

    @Test
    @WithMockUser(username = "student@test.com")
    @DisplayName("createSupportRequest_validRequest_returns201")
    void createSupportRequest_validRequest_returns201() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("student@test.com");
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(user));

        SupportRequestDTO dto = createMockDTO();
        when(supportRequestService.create(eq(user.getId()), eq("Test description"), any()))
                .thenReturn(dto);

        mockMvc.perform(post("/slib/support-requests")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("description", "Test description"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Test support request"));
    }

    @Test
    @WithMockUser(username = "student@test.com")
    @DisplayName("createSupportRequest_missingDescription_returns400")
    void createSupportRequest_missingDescription_returns400() throws Exception {
        mockMvc.perform(post("/slib/support-requests")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === DELETE BATCH ===
    // =========================================

    @Test
    @WithMockUser(username = "librarian@test.com")
    @DisplayName("deleteBatch_validIds_returns200")
    void deleteBatch_validIds_returns200() throws Exception {
        User user = createMockUser();
        when(userRepository.findByEmail("librarian@test.com")).thenReturn(Optional.of(user));

        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        doNothing().when(supportRequestService).deleteBatch(ids);

        mockMvc.perform(delete("/slib/support-requests/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("ids", ids.stream().map(UUID::toString).toList()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(2));
    }

    @Test
    @WithMockUser(username = "librarian@test.com")
    @DisplayName("deleteBatch_emptyIds_returns400")
    void deleteBatch_emptyIds_returns400() throws Exception {
        mockMvc.perform(delete("/slib/support-requests/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("ids", List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
