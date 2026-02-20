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
import slib.com.example.controller.feedback.SeatViolationReportController;
import slib.com.example.dto.feedback.ViolationReportResponse;
import slib.com.example.entity.feedback.SeatViolationReportEntity.ReportStatus;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.SeatViolationReportService;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for SeatViolationReportController
 */
@WebMvcTest(value = SeatViolationReportController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SeatViolationReportController Unit Tests")
class SeatViolationReportControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeatViolationReportService violationReportService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private ViolationReportResponse createMockResponse() {
        return ViolationReportResponse.builder()
                .id(UUID.randomUUID())
                .status(ReportStatus.PENDING.name())
                .build();
    }

    // =========================================
    // === GET ALL ===
    // =========================================

    @Test
    @DisplayName("getAll_noFilter_returns200WithList")
    void getAll_noFilter_returns200WithList() throws Exception {
        List<ViolationReportResponse> list = List.of(createMockResponse());
        when(violationReportService.getAll()).thenReturn(list);

        mockMvc.perform(get("/slib/violation-reports")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("getAll_withStatusFilter_returns200")
    void getAll_withStatusFilter_returns200() throws Exception {
        List<ViolationReportResponse> list = List.of(createMockResponse());
        when(violationReportService.getByStatus(ReportStatus.PENDING)).thenReturn(list);

        mockMvc.perform(get("/slib/violation-reports")
                .param("status", "PENDING")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(violationReportService).getByStatus(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("getAll_invalidStatus_returns400")
    void getAll_invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/slib/violation-reports")
                .param("status", "INVALID")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === GET MY REPORTS ===
    // =========================================

    @Test
    @WithMockUser(username = "student@test.com")
    @DisplayName("getMyReports_authenticated_returns200")
    void getMyReports_authenticated_returns200() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("student@test.com");
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(user));

        List<ViolationReportResponse> list = List.of(createMockResponse());
        when(violationReportService.getMyReports(user.getId())).thenReturn(list);

        mockMvc.perform(get("/slib/violation-reports/my")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // =========================================
    // === VERIFY ===
    // =========================================

    @Test
    @WithMockUser(username = "librarian@test.com")
    @DisplayName("verify_validId_returns200")
    void verify_validId_returns200() throws Exception {
        UUID reportId = UUID.randomUUID();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("librarian@test.com");
        when(userRepository.findByEmail("librarian@test.com")).thenReturn(Optional.of(user));

        ViolationReportResponse response = ViolationReportResponse.builder()
                .id(reportId)
                .status(ReportStatus.VERIFIED.name())
                .build();
        when(violationReportService.verifyReport(reportId, user.getId())).thenReturn(response);

        mockMvc.perform(put("/slib/violation-reports/{id}/verify", reportId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(violationReportService).verifyReport(reportId, user.getId());
    }

    // =========================================
    // === REJECT ===
    // =========================================

    @Test
    @WithMockUser(username = "librarian@test.com")
    @DisplayName("reject_validId_returns200")
    void reject_validId_returns200() throws Exception {
        UUID reportId = UUID.randomUUID();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("librarian@test.com");
        when(userRepository.findByEmail("librarian@test.com")).thenReturn(Optional.of(user));

        ViolationReportResponse response = ViolationReportResponse.builder()
                .id(reportId)
                .status(ReportStatus.REJECTED.name())
                .build();
        when(violationReportService.rejectReport(reportId, user.getId())).thenReturn(response);

        mockMvc.perform(put("/slib/violation-reports/{id}/reject", reportId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(violationReportService).rejectReport(reportId, user.getId());
    }

    // =========================================
    // === GET COUNT ===
    // =========================================

    @Test
    @DisplayName("getCount_returns200WithCounts")
    void getCount_returns200WithCounts() throws Exception {
        when(violationReportService.countByStatus(ReportStatus.PENDING)).thenReturn(3L);
        when(violationReportService.countByStatus(ReportStatus.VERIFIED)).thenReturn(7L);
        when(violationReportService.countByStatus(ReportStatus.RESOLVED)).thenReturn(15L);
        when(violationReportService.countByStatus(ReportStatus.REJECTED)).thenReturn(2L);

        mockMvc.perform(get("/slib/violation-reports/count")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(3))
                .andExpect(jsonPath("$.verified").value(7))
                .andExpect(jsonPath("$.resolved").value(15))
                .andExpect(jsonPath("$.rejected").value(2));
    }
}
