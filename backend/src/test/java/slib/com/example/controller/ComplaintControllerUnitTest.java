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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.dto.complaint.ComplaintDTO;
import slib.com.example.entity.complaint.ComplaintEntity.ComplaintStatus;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.ComplaintService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for ComplaintController
 */
@WebMvcTest(value = ComplaintController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ComplaintController Unit Tests")
class ComplaintControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComplaintService complaintService;

    @MockBean
    private UserRepository userRepository;

    private User createMockUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        return user;
    }

    // =========================================
    // === GET ALL COMPLAINTS ===
    // =========================================

    @Test
    @DisplayName("getAll_noStatusFilter_returns200WithAllComplaints")
    void getAll_noStatusFilter_returns200WithAllComplaints() throws Exception {
        List<ComplaintDTO> complaints = Arrays.asList(
                ComplaintDTO.builder().id(UUID.randomUUID()).subject("Test 1").status("PENDING").build(),
                ComplaintDTO.builder().id(UUID.randomUUID()).subject("Test 2").status("ACCEPTED").build());

        when(complaintService.getAll()).thenReturn(complaints);

        mockMvc.perform(get("/slib/complaints")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].subject").value("Test 1"));

        verify(complaintService).getAll();
    }

    @Test
    @DisplayName("getAll_withStatusFilter_returns200WithFilteredComplaints")
    void getAll_withStatusFilter_returns200WithFilteredComplaints() throws Exception {
        List<ComplaintDTO> complaints = Collections.singletonList(
                ComplaintDTO.builder().id(UUID.randomUUID()).subject("Pending").status("PENDING").build());

        when(complaintService.getByStatus(ComplaintStatus.PENDING)).thenReturn(complaints);

        mockMvc.perform(get("/slib/complaints")
                .param("status", "PENDING")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(complaintService).getByStatus(ComplaintStatus.PENDING);
    }

    @Test
    @DisplayName("getAll_invalidStatus_returns400")
    void getAll_invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/slib/complaints")
                .param("status", "INVALID_STATUS")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === GET MY COMPLAINTS ===
    // =========================================

    @Test
    @DisplayName("getMyComplaints_authenticated_returns200")
    @WithMockUser(username = "test@example.com")
    void getMyComplaints_authenticated_returns200() throws Exception {
        User user = createMockUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(complaintService.getByStudent(user.getId())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/slib/complaints/my")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // =========================================
    // === CREATE COMPLAINT ===
    // =========================================

    @Test
    @DisplayName("create_validData_returns201")
    @WithMockUser(username = "test@example.com")
    void create_validData_returns201() throws Exception {
        User user = createMockUser();
        ComplaintDTO result = ComplaintDTO.builder()
                .id(UUID.randomUUID())
                .subject("Test complaint")
                .content("Complaint content")
                .status("PENDING")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(complaintService.create(eq(user.getId()), eq("Test complaint"), eq("Complaint content"), isNull(),
                isNull())).thenReturn(result);

        mockMvc.perform(post("/slib/complaints")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"Test complaint\",\"content\":\"Complaint content\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value("Test complaint"));
    }

    @Test
    @DisplayName("create_missingSubject_returns400")
    @WithMockUser(username = "test@example.com")
    void create_missingSubject_returns400() throws Exception {
        User user = createMockUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/slib/complaints")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Only content\"}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === ACCEPT COMPLAINT ===
    // =========================================

    @Test
    @DisplayName("accept_validId_returns200")
    @WithMockUser(username = "test@example.com")
    void accept_validId_returns200() throws Exception {
        User user = createMockUser();
        UUID complaintId = UUID.randomUUID();
        ComplaintDTO result = ComplaintDTO.builder()
                .id(complaintId)
                .status("ACCEPTED")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(complaintService.accept(eq(complaintId), eq(user.getId()), any())).thenReturn(result);

        mockMvc.perform(put("/slib/complaints/{id}/accept", complaintId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Accepted\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    // =========================================
    // === DENY COMPLAINT ===
    // =========================================

    @Test
    @DisplayName("deny_validId_returns200")
    @WithMockUser(username = "test@example.com")
    void deny_validId_returns200() throws Exception {
        User user = createMockUser();
        UUID complaintId = UUID.randomUUID();
        ComplaintDTO result = ComplaintDTO.builder()
                .id(complaintId)
                .status("DENIED")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(complaintService.deny(eq(complaintId), eq(user.getId()), any())).thenReturn(result);

        mockMvc.perform(put("/slib/complaints/{id}/deny", complaintId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Denied\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DENIED"));
    }

    // =========================================
    // === GET COUNT ===
    // =========================================

    @Test
    @DisplayName("getCount_returns200WithCounts")
    void getCount_returns200WithCounts() throws Exception {
        when(complaintService.countByStatus(ComplaintStatus.PENDING)).thenReturn(5L);
        when(complaintService.countByStatus(ComplaintStatus.ACCEPTED)).thenReturn(3L);
        when(complaintService.countByStatus(ComplaintStatus.DENIED)).thenReturn(2L);

        mockMvc.perform(get("/slib/complaints/count")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(5))
                .andExpect(jsonPath("$.accepted").value(3))
                .andExpect(jsonPath("$.denied").value(2));
    }
}
