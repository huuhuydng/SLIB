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
import slib.com.example.repository.UserRepository;
import slib.com.example.service.SeatViolationReportService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-99: Verify Violation Report
 * Test Report: doc/Report/UnitTestReport/FE99_TestReport.md
 */
@WebMvcTest(value = SeatViolationReportController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-99: Verify Violation Report - Unit Tests")
class FE99_VerifyViolationReportTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeatViolationReportService violationReportService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID librarianId = UUID.randomUUID();
    private final UUID reportId = UUID.randomUUID();

    private slib.com.example.entity.users.User mockLibrarian() {
        slib.com.example.entity.users.User u = new slib.com.example.entity.users.User();
        u.setId(librarianId);
        u.setEmail("librarian@fpt.edu.vn");
        u.setFullName("Thu Thu A");
        u.setRole(slib.com.example.entity.users.Role.LIBRARIAN);
        return u;
    }

    private UserDetails librarianDetails() {
        return org.springframework.security.core.userdetails.User.withUsername("librarian@fpt.edu.vn")
                .password("pass").roles("LIBRARIAN").build();
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
    // === UTCID01: Verify a pending violation report ===
    // =========================================
    @Test
    @DisplayName("UTCID01: Verify a pending violation report - returns 200 OK")
    void verify_pendingReport_returns200() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        ViolationReportResponse response = ViolationReportResponse.builder()
                .id(reportId).status("VERIFIED").verifiedByName("Thu Thu A")
                .pointDeducted(10).verifiedAt(LocalDateTime.now()).build();
        when(violationReportService.verifyReport(reportId, librarianId)).thenReturn(response);

        mockMvc.perform(put("/slib/violation-reports/{id}/verify", reportId)
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.verifiedByName").value("Thu Thu A"))
                .andExpect(jsonPath("$.pointDeducted").value(10));

        verify(violationReportService, times(1)).verifyReport(reportId, librarianId);
    }

    // =========================================
    // === UTCID02: Reject a pending violation report ===
    // =========================================
    @Test
    @DisplayName("UTCID02: Reject a pending violation report - returns 200 OK")
    void reject_pendingReport_returns200() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        ViolationReportResponse response = ViolationReportResponse.builder()
                .id(reportId).status("REJECTED").verifiedByName("Thu Thu A")
                .verifiedAt(LocalDateTime.now()).build();
        when(violationReportService.rejectReport(reportId, librarianId)).thenReturn(response);

        mockMvc.perform(put("/slib/violation-reports/{id}/reject", reportId)
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        verify(violationReportService, times(1)).rejectReport(reportId, librarianId);
    }

    // =========================================
    // === UTCID03: Verify report with violator assignment ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Verify report triggers violator point deduction - returns 200 OK")
    void verify_withViolatorDeduction_returns200() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        ViolationReportResponse response = ViolationReportResponse.builder()
                .id(reportId).status("VERIFIED")
                .violatorId(UUID.randomUUID()).violatorName("Le Thi B")
                .verifiedByName("Thu Thu A").pointDeducted(15)
                .verifiedAt(LocalDateTime.now()).build();
        when(violationReportService.verifyReport(reportId, librarianId)).thenReturn(response);

        mockMvc.perform(put("/slib/violation-reports/{id}/verify", reportId)
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violatorName").value("Le Thi B"))
                .andExpect(jsonPath("$.pointDeducted").value(15));
    }

    // =========================================
    // === UTCID04: Report not found ===
    // =========================================
    @Test
    @DisplayName("UTCID04: Report not found - returns 500 (service throws RuntimeException)")
    void verify_reportNotFound_returns500() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        when(violationReportService.verifyReport(any(), any()))
                .thenThrow(new RuntimeException("Report not found"));

        mockMvc.perform(put("/slib/violation-reports/{id}/verify", reportId)
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isInternalServerError());
    }

    // =========================================
    // === UTCID05: Librarian not found ===
    // =========================================
    @Test
    @DisplayName("UTCID05: Librarian user not found - returns 500")
    void verify_librarianNotFound_returns500() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.empty());

        mockMvc.perform(put("/slib/violation-reports/{id}/verify", reportId)
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isInternalServerError());
    }
}
