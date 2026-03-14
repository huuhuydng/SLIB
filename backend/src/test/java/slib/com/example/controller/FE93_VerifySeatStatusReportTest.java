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
import slib.com.example.controller.feedback.SeatStatusReportController;
import slib.com.example.dto.feedback.SeatStatusReportResponse;
import slib.com.example.exception.BadRequestException;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.feedback.SeatStatusReportService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-93: Verify Seat Status Report
 * Test Report: doc/Report/UnitTestReport/FE93_TestReport.md
 */
@WebMvcTest(value = SeatStatusReportController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-93: Verify Seat Status Report - Unit Tests")
class FE93_VerifySeatStatusReportTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeatStatusReportService seatStatusReportService;

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
    // === UTCID01: Verify a pending report ===
    // =========================================
    @Test
    @DisplayName("UTCID01: Verify a pending report - returns 200 OK")
    void verify_pendingReport_returns200() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        SeatStatusReportResponse response = SeatStatusReportResponse.builder()
                .id(reportId).status("VERIFIED").verifiedByName("Thu Thu A")
                .verifiedAt(LocalDateTime.now()).build();
        when(seatStatusReportService.verifyReport(reportId, librarianId)).thenReturn(response);

        mockMvc.perform(put("/slib/seat-status-reports/{id}/verify", reportId)
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.verifiedByName").value("Thu Thu A"));
    }

    // =========================================
    // === UTCID02: Reject a pending report ===
    // =========================================
    @Test
    @DisplayName("UTCID02: Reject a pending report - returns 200 OK")
    void reject_pendingReport_returns200() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        SeatStatusReportResponse response = SeatStatusReportResponse.builder()
                .id(reportId).status("REJECTED").verifiedByName("Thu Thu A").build();
        when(seatStatusReportService.rejectReport(reportId, librarianId)).thenReturn(response);

        mockMvc.perform(put("/slib/seat-status-reports/{id}/reject", reportId)
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    // =========================================
    // === UTCID03: Resolve a verified report ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Resolve a verified report - returns 200 OK")
    void resolve_verifiedReport_returns200() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        SeatStatusReportResponse response = SeatStatusReportResponse.builder()
                .id(reportId).status("RESOLVED").verifiedByName("Thu Thu A")
                .resolvedAt(LocalDateTime.now()).build();
        when(seatStatusReportService.resolveReport(reportId, librarianId)).thenReturn(response);

        mockMvc.perform(put("/slib/seat-status-reports/{id}/resolve", reportId)
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    // =========================================
    // === UTCID04: Resolve a report with no verifiedBy yet ===
    // =========================================
    @Test
    @DisplayName("UTCID04: Resolve a report with no verifiedBy yet - returns 200 OK")
    void resolve_noVerifiedByYet_returns200() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        SeatStatusReportResponse response = SeatStatusReportResponse.builder()
                .id(reportId).status("RESOLVED").verifiedByName("Thu Thu A")
                .resolvedAt(LocalDateTime.now()).build();
        when(seatStatusReportService.resolveReport(reportId, librarianId)).thenReturn(response);

        mockMvc.perform(put("/slib/seat-status-reports/{id}/resolve", reportId)
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    // =========================================
    // === UTCID05: Resolve a rejected report ===
    // =========================================
    @Test
    @DisplayName("UTCID05: Resolve a rejected report - returns 400 Bad Request")
    void resolve_rejectedReport_returns400() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        when(seatStatusReportService.resolveReport(reportId, librarianId))
                .thenThrow(new BadRequestException("Khong the giai quyet bao cao da bi tu choi"));

        mockMvc.perform(put("/slib/seat-status-reports/{id}/resolve", reportId)
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === UTCID06: Process unknown report id ===
    // =========================================
    @Test
    @DisplayName("UTCID06: Process unknown report id - returns 404 Not Found")
    void verify_unknownId_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        when(seatStatusReportService.verifyReport(unknownId, librarianId))
                .thenThrow(new ResourceNotFoundException("SeatStatusReport", "id", unknownId));

        mockMvc.perform(put("/slib/seat-status-reports/{id}/verify", unknownId)
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isNotFound());
    }
}
