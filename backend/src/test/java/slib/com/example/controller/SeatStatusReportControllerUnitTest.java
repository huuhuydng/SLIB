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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.feedback.SeatStatusReportController;
import slib.com.example.dto.feedback.SeatStatusReportResponse;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.SeatStatusReportService;

import java.time.LocalDateTime;
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
 * Unit Tests for SeatStatusReportController
 */
@WebMvcTest(value = SeatStatusReportController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SeatStatusReportController Unit Tests")
class SeatStatusReportControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeatStatusReportService seatStatusReportService;

    @MockBean
    private UserRepository userRepository;

    private User createMockStudent() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("student@example.com");
        user.setRole(Role.STUDENT);
        return user;
    }

    private User createMockLibrarian() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("librarian@example.com");
        user.setRole(Role.LIBRARIAN);
        return user;
    }

    private SeatStatusReportResponse createMockResponse(UUID id, String status) {
        return SeatStatusReportResponse.builder()
                .id(id)
                .reporterId(UUID.randomUUID())
                .reporterName("Student Test")
                .seatId(1)
                .seatCode("A1")
                .issueType("BROKEN")
                .issueTypeLabel("Ghế hỏng")
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // =========================================
    // === CREATE REPORT ===
    // =========================================

    @Test
    @DisplayName("create_validData_returns201")
    @WithMockUser(username = "student@example.com")
    void create_validData_returns201() throws Exception {
        User user = createMockStudent();
        UUID reportId = UUID.randomUUID();
        SeatStatusReportResponse response = createMockResponse(reportId, "PENDING");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(seatStatusReportService.createReport(eq(user.getId()), any(), isNull())).thenReturn(response);

        mockMvc.perform(multipart("/slib/seat-status-reports")
                .param("seatId", "1")
                .param("issueType", "BROKEN"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seatCode").value("A1"))
                .andExpect(jsonPath("$.issueType").value("BROKEN"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("create_withDescription_returns201")
    @WithMockUser(username = "student@example.com")
    void create_withDescription_returns201() throws Exception {
        User user = createMockStudent();
        SeatStatusReportResponse response = createMockResponse(UUID.randomUUID(), "PENDING");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(seatStatusReportService.createReport(eq(user.getId()), any(), isNull())).thenReturn(response);

        mockMvc.perform(multipart("/slib/seat-status-reports")
                .param("seatId", "1")
                .param("issueType", "DIRTY")
                .param("description", "Ghế rất bẩn"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("create_withImage_returns201")
    @WithMockUser(username = "student@example.com")
    void create_withImage_returns201() throws Exception {
        User user = createMockStudent();
        SeatStatusReportResponse response = createMockResponse(UUID.randomUUID(), "PENDING");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(seatStatusReportService.createReport(eq(user.getId()), any(), any())).thenReturn(response);

        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test-image".getBytes());

        mockMvc.perform(multipart("/slib/seat-status-reports")
                .file(image)
                .param("seatId", "1")
                .param("issueType", "BROKEN"))
                .andExpect(status().isCreated());
    }

    // =========================================
    // === GET MY REPORTS ===
    // =========================================

    @Test
    @DisplayName("getMyReports_authenticated_returns200")
    @WithMockUser(username = "student@example.com")
    void getMyReports_authenticated_returns200() throws Exception {
        User user = createMockStudent();
        List<SeatStatusReportResponse> reports = Arrays.asList(
                createMockResponse(UUID.randomUUID(), "PENDING"),
                createMockResponse(UUID.randomUUID(), "VERIFIED"));

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(seatStatusReportService.getMyReports(user.getId())).thenReturn(reports);

        mockMvc.perform(get("/slib/seat-status-reports/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("getMyReports_noReports_returns200EmptyList")
    @WithMockUser(username = "student@example.com")
    void getMyReports_noReports_returns200EmptyList() throws Exception {
        User user = createMockStudent();

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(seatStatusReportService.getMyReports(user.getId())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/slib/seat-status-reports/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =========================================
    // === GET ALL (STAFF ONLY) ===
    // =========================================

    @Test
    @DisplayName("getAll_asLibrarian_returns200")
    @WithMockUser(username = "librarian@example.com")
    void getAll_asLibrarian_returns200() throws Exception {
        User librarian = createMockLibrarian();
        List<SeatStatusReportResponse> reports = Collections.singletonList(
                createMockResponse(UUID.randomUUID(), "PENDING"));

        when(userRepository.findByEmail("librarian@example.com")).thenReturn(Optional.of(librarian));
        when(seatStatusReportService.getAll(isNull())).thenReturn(reports);

        mockMvc.perform(get("/slib/seat-status-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("getAll_withStatusFilter_returns200")
    @WithMockUser(username = "librarian@example.com")
    void getAll_withStatusFilter_returns200() throws Exception {
        User librarian = createMockLibrarian();
        List<SeatStatusReportResponse> reports = Collections.singletonList(
                createMockResponse(UUID.randomUUID(), "PENDING"));

        when(userRepository.findByEmail("librarian@example.com")).thenReturn(Optional.of(librarian));
        when(seatStatusReportService.getAll("PENDING")).thenReturn(reports);

        mockMvc.perform(get("/slib/seat-status-reports")
                .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("getAll_asStudent_returns403")
    @WithMockUser(username = "student@example.com")
    void getAll_asStudent_returns403() throws Exception {
        User student = createMockStudent();
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student));

        mockMvc.perform(get("/slib/seat-status-reports"))
                .andExpect(status().isForbidden());
    }

    // =========================================
    // === GET BY ID ===
    // =========================================

    @Test
    @DisplayName("getById_asOwner_returns200")
    @WithMockUser(username = "student@example.com")
    void getById_asOwner_returns200() throws Exception {
        User student = createMockStudent();
        UUID reportId = UUID.randomUUID();
        SeatStatusReportResponse response = SeatStatusReportResponse.builder()
                .id(reportId)
                .reporterId(student.getId())
                .seatId(1)
                .seatCode("A1")
                .issueType("BROKEN")
                .issueTypeLabel("Ghế hỏng")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student));
        when(seatStatusReportService.getById(reportId)).thenReturn(response);

        mockMvc.perform(get("/slib/seat-status-reports/{id}", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId.toString()));
    }

    // =========================================
    // === VERIFY REPORT ===
    // =========================================

    @Test
    @DisplayName("verify_asLibrarian_returns200")
    @WithMockUser(username = "librarian@example.com")
    void verify_asLibrarian_returns200() throws Exception {
        User librarian = createMockLibrarian();
        UUID reportId = UUID.randomUUID();
        SeatStatusReportResponse response = createMockResponse(reportId, "VERIFIED");

        when(userRepository.findByEmail("librarian@example.com")).thenReturn(Optional.of(librarian));
        when(seatStatusReportService.verifyReport(reportId, librarian.getId())).thenReturn(response);

        mockMvc.perform(put("/slib/seat-status-reports/{id}/verify", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    // =========================================
    // === REJECT REPORT ===
    // =========================================

    @Test
    @DisplayName("reject_asLibrarian_returns200")
    @WithMockUser(username = "librarian@example.com")
    void reject_asLibrarian_returns200() throws Exception {
        User librarian = createMockLibrarian();
        UUID reportId = UUID.randomUUID();
        SeatStatusReportResponse response = createMockResponse(reportId, "REJECTED");

        when(userRepository.findByEmail("librarian@example.com")).thenReturn(Optional.of(librarian));
        when(seatStatusReportService.rejectReport(reportId, librarian.getId())).thenReturn(response);

        mockMvc.perform(put("/slib/seat-status-reports/{id}/reject", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    // =========================================
    // === RESOLVE REPORT ===
    // =========================================

    @Test
    @DisplayName("resolve_asLibrarian_returns200")
    @WithMockUser(username = "librarian@example.com")
    void resolve_asLibrarian_returns200() throws Exception {
        User librarian = createMockLibrarian();
        UUID reportId = UUID.randomUUID();
        SeatStatusReportResponse response = createMockResponse(reportId, "RESOLVED");

        when(userRepository.findByEmail("librarian@example.com")).thenReturn(Optional.of(librarian));
        when(seatStatusReportService.resolveReport(reportId, librarian.getId())).thenReturn(response);

        mockMvc.perform(put("/slib/seat-status-reports/{id}/resolve", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }
}
