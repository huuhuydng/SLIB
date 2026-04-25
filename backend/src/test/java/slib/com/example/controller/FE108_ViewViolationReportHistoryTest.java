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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import slib.com.example.controller.feedback.SeatViolationReportController;
import slib.com.example.dto.feedback.ViolationReportResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.feedback.SeatViolationReportService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-108: View history of sending report seat violation
 * Test Report: doc/Report/UnitTestReport/FE95_TestReport.md
 */
@WebMvcTest(value = SeatViolationReportController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-108: View history of sending report seat violation - Unit Tests")
class FE108_ViewViolationReportHistoryTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeatViolationReportService violationReportService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID reporterId = UUID.randomUUID();

    private slib.com.example.entity.users.User mockStudent() {
        slib.com.example.entity.users.User u = new slib.com.example.entity.users.User();
        u.setId(reporterId);
        u.setEmail("student@fpt.edu.vn");
        u.setFullName("Nguyen Van A");
        u.setRole(slib.com.example.entity.users.Role.STUDENT);
        return u;
    }

    private UserDetails userDetails() {
        return org.springframework.security.core.userdetails.User.withUsername("student@fpt.edu.vn")
                .password("pass").roles("STUDENT").build();
    }

    private RequestPostProcessor securityContext(UserDetails userDetails) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()));
            return request;
        };
    }

    // =========================================
    // === UTCID01: Reporter has violation reports ===
    // =========================================
    @Test
    @DisplayName("UTCID01: Reporter has violation reports - returns 200 OK with list")
    void getMyReports_withReports_returns200() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        List<ViolationReportResponse> reports = List.of(
                ViolationReportResponse.builder()
                        .id(UUID.randomUUID()).reporterId(reporterId).seatId(101)
                        .violationType("NOISE").status("PENDING")
                        .createdAt(LocalDateTime.now()).build());
        when(violationReportService.getMyReports(reporterId)).thenReturn(reports);

        mockMvc.perform(get("/slib/violation-reports/my").with(securityContext(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].violationType").value("NOISE"));

        verify(violationReportService, times(1)).getMyReports(reporterId);
    }

    // =========================================
    // === UTCID02: Reporter has multiple reports ===
    // =========================================
    @Test
    @DisplayName("UTCID02: Reporter has multiple reports - returns 200 OK")
    void getMyReports_multipleReports_returns200() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        List<ViolationReportResponse> reports = List.of(
                ViolationReportResponse.builder().id(UUID.randomUUID()).reporterId(reporterId)
                        .violationType("NOISE").status("PENDING").build(),
                ViolationReportResponse.builder().id(UUID.randomUUID()).reporterId(reporterId)
                        .violationType("LEFT_BELONGINGS").status("VERIFIED").build());
        when(violationReportService.getMyReports(reporterId)).thenReturn(reports);

        mockMvc.perform(get("/slib/violation-reports/my").with(securityContext(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // =========================================
    // === UTCID03: Reporter has reports with different statuses ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Reports with mixed statuses - returns 200 OK")
    void getMyReports_mixedStatuses_returns200() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        List<ViolationReportResponse> reports = List.of(
                ViolationReportResponse.builder().id(UUID.randomUUID()).reporterId(reporterId)
                        .violationType("NOISE").status("REJECTED").build());
        when(violationReportService.getMyReports(reporterId)).thenReturn(reports);

        mockMvc.perform(get("/slib/violation-reports/my").with(securityContext(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REJECTED"));
    }

    // =========================================
    // === UTCID04: Reporter has no prior reports ===
    // =========================================
    @Test
    @DisplayName("UTCID04: Reporter has no prior reports - returns 200 OK with empty list")
    void getMyReports_noReports_returns200Empty() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        when(violationReportService.getMyReports(reporterId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/slib/violation-reports/my").with(securityContext(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =========================================
    // === UTCID05: Repository failure ===
    // =========================================
    @Test
    @DisplayName("UTCID05: Repository failure - returns 500 Internal Server Error")
    void getMyReports_repositoryFailure_returns500() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        when(violationReportService.getMyReports(reporterId))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/slib/violation-reports/my").with(securityContext(userDetails())))
                .andExpect(status().isInternalServerError());
    }
}
