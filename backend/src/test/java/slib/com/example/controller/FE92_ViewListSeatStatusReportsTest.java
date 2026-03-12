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
import slib.com.example.repository.UserRepository;
import slib.com.example.service.SeatStatusReportService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-92: View List of Seat Status Reports
 * Test Report: doc/Report/UnitTestReport/FE92_TestReport.md
 */
@WebMvcTest(value = SeatStatusReportController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-92: View List of Seat Status Reports - Unit Tests")
class FE92_ViewListSeatStatusReportsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeatStatusReportService seatStatusReportService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID librarianId = UUID.randomUUID();

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
                .password("pass")
                .roles("LIBRARIAN")
                .build();
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
    // === UTCID01: Load all seat status reports ===
    // =========================================
    @Test
    @DisplayName("UTCID01: Load all seat status reports - returns 200 OK")
    void getAll_noFilter_returns200() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        List<SeatStatusReportResponse> reports = List.of(
                SeatStatusReportResponse.builder().id(UUID.randomUUID()).seatId(101)
                        .issueType("BROKEN").status("PENDING").createdAt(LocalDateTime.now()).build(),
                SeatStatusReportResponse.builder().id(UUID.randomUUID()).seatId(102)
                        .issueType("DIRTY").status("VERIFIED").createdAt(LocalDateTime.now()).build());
        when(seatStatusReportService.getAll(isNull())).thenReturn(reports);

        mockMvc.perform(get("/slib/seat-status-reports").with(securityContext(librarianDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(seatStatusReportService, times(1)).getAll(isNull());
    }

    // =========================================
    // === UTCID02: Filter reports by PENDING status ===
    // =========================================
    @Test
    @DisplayName("UTCID02: Filter reports by PENDING status - returns 200 OK")
    void getAll_filterPending_returns200() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        List<SeatStatusReportResponse> pendingReports = List.of(
                SeatStatusReportResponse.builder().id(UUID.randomUUID()).seatId(101)
                        .issueType("BROKEN").status("PENDING").build());
        when(seatStatusReportService.getAll("PENDING")).thenReturn(pendingReports);

        mockMvc.perform(get("/slib/seat-status-reports")
                        .param("status", "PENDING")
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    // =========================================
    // === UTCID03: Filter reports by VERIFIED status ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Filter reports by VERIFIED status - returns 200 OK")
    void getAll_filterVerified_returns200() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        List<SeatStatusReportResponse> verifiedReports = List.of(
                SeatStatusReportResponse.builder().id(UUID.randomUUID()).seatId(102)
                        .issueType("DIRTY").status("VERIFIED").build());
        when(seatStatusReportService.getAll("VERIFIED")).thenReturn(verifiedReports);

        mockMvc.perform(get("/slib/seat-status-reports")
                        .param("status", "VERIFIED")
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("VERIFIED"));
    }

    // =========================================
    // === UTCID04: No report matches filter ===
    // =========================================
    @Test
    @DisplayName("UTCID04: No report matches the selected filter - returns 200 OK with empty list")
    void getAll_noMatch_returns200Empty() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        when(seatStatusReportService.getAll("RESOLVED")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/slib/seat-status-reports")
                        .param("status", "RESOLVED")
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =========================================
    // === UTCID05: Invalid status filter ===
    // =========================================
    @Test
    @DisplayName("UTCID05: Invalid status filter - returns 400 Bad Request")
    void getAll_invalidStatus_returns400() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        when(seatStatusReportService.getAll("INVALID_STATUS"))
                .thenThrow(new BadRequestException("Trang thai khong hop le"));

        mockMvc.perform(get("/slib/seat-status-reports")
                        .param("status", "INVALID_STATUS")
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isBadRequest());
    }
}
