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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import slib.com.example.controller.feedback.SeatStatusReportController;
import slib.com.example.dto.feedback.SeatStatusReportResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.feedback.SeatStatusReportService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-103: View history of sending seat status report
 * Test Report: doc/Report/UnitTestReport/FE90_TestReport.md
 */
@WebMvcTest(value = SeatStatusReportController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-103: View history of sending seat status report - Unit Tests")
class FE103_ViewHistorySeatStatusReportTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeatStatusReportService seatStatusReportService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID studentId = UUID.randomUUID();

    private slib.com.example.entity.users.User mockStudent() {
        slib.com.example.entity.users.User u = new slib.com.example.entity.users.User();
        u.setId(studentId);
        u.setEmail("student@fpt.edu.vn");
        u.setFullName("Nguyen Van A");
        u.setRole(slib.com.example.entity.users.Role.STUDENT);
        return u;
    }

    private UserDetails userDetails() {
        return org.springframework.security.core.userdetails.User.withUsername("student@fpt.edu.vn")
                .password("pass")
                .roles("STUDENT")
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
    // === UTCID01: History contains submitted reports ===
    // =========================================
    @Test
    @DisplayName("UTCID01: History contains submitted reports - returns 200 OK")
    void getMyReports_withReports_returns200() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        List<SeatStatusReportResponse> reports = List.of(
                SeatStatusReportResponse.builder()
                        .id(UUID.randomUUID()).reporterId(studentId).seatId(101)
                        .issueType("BROKEN").status("PENDING").createdAt(LocalDateTime.now()).build(),
                SeatStatusReportResponse.builder()
                        .id(UUID.randomUUID()).reporterId(studentId).seatId(102)
                        .issueType("DIRTY").status("VERIFIED").createdAt(LocalDateTime.now()).build());
        when(seatStatusReportService.getMyReports(studentId)).thenReturn(reports);

        mockMvc.perform(get("/slib/seat-status-reports/my").with(securityContext(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(seatStatusReportService, times(1)).getMyReports(studentId);
    }

    // =========================================
    // === UTCID02: History is empty ===
    // =========================================
    @Test
    @DisplayName("UTCID02: History is empty - returns 200 OK with empty list")
    void getMyReports_empty_returns200() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        when(seatStatusReportService.getMyReports(studentId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/slib/seat-status-reports/my").with(securityContext(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =========================================
    // === UTCID03: Session expired / unauthenticated ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Session expired - returns 403 Forbidden")
    void getMyReports_unauthenticated_returns403() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn"))
                .thenThrow(new AccessDeniedException("Session expired. Please log in again."));

        mockMvc.perform(get("/slib/seat-status-reports/my").with(securityContext(userDetails())))
                .andExpect(status().isForbidden());
    }

    // =========================================
    // === UTCID04: Unexpected repository failure ===
    // =========================================
    @Test
    @DisplayName("UTCID04: Unexpected repository failure - returns 500")
    void getMyReports_repositoryFailure_returns500() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        when(seatStatusReportService.getMyReports(studentId))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/slib/seat-status-reports/my").with(securityContext(userDetails())))
                .andExpect(status().isInternalServerError());
    }
}
