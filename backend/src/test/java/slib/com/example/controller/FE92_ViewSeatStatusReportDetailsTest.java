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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.feedback.SeatStatusReportService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-92: View Seat Status Report Details
 * Test Report: doc/Report/UnitTestReport/FE92_TestReport.md
 */
@WebMvcTest(value = SeatStatusReportController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-92: View Seat Status Report Details - Unit Tests")
class FE92_ViewSeatStatusReportDetailsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SeatStatusReportService seatStatusReportService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID librarianId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID reportId = UUID.randomUUID();

    private slib.com.example.entity.users.User mockLibrarian() {
        slib.com.example.entity.users.User u = new slib.com.example.entity.users.User();
        u.setId(librarianId);
        u.setEmail("librarian@fpt.edu.vn");
        u.setFullName("Thu Thu A");
        u.setRole(slib.com.example.entity.users.Role.LIBRARIAN);
        return u;
    }

    private slib.com.example.entity.users.User mockStudent() {
        slib.com.example.entity.users.User u = new slib.com.example.entity.users.User();
        u.setId(studentId);
        u.setEmail("student@fpt.edu.vn");
        u.setFullName("Nguyen Van A");
        u.setRole(slib.com.example.entity.users.Role.STUDENT);
        return u;
    }

    private UserDetails librarianDetails() {
        return org.springframework.security.core.userdetails.User.withUsername("librarian@fpt.edu.vn")
                .password("pass").roles("LIBRARIAN").build();
    }

    private UserDetails studentDetails() {
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
    // === UTCID01: Librarian/admin views existing report detail ===
    // =========================================
    @Test
    @DisplayName("UTCID01: Librarian views existing report detail - returns 200 OK")
    void getById_librarianViews_returns200() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        SeatStatusReportResponse response = SeatStatusReportResponse.builder()
                .id(reportId).reporterId(studentId).seatId(101)
                .issueType("BROKEN").issueTypeLabel("Ghe hong")
                .description("Ghe bi gay chan").status("PENDING")
                .createdAt(LocalDateTime.now()).build();
        when(seatStatusReportService.getById(reportId)).thenReturn(response);

        mockMvc.perform(get("/slib/seat-status-reports/{id}", reportId)
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId.toString()))
                .andExpect(jsonPath("$.issueType").value("BROKEN"));
    }

    // =========================================
    // === UTCID02: Student views own report detail ===
    // =========================================
    @Test
    @DisplayName("UTCID02: Student views own report detail - returns 200 OK")
    void getById_studentViewsOwn_returns200() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        SeatStatusReportResponse response = SeatStatusReportResponse.builder()
                .id(reportId).reporterId(studentId).seatId(101)
                .issueType("DIRTY").status("PENDING").build();
        when(seatStatusReportService.getById(reportId)).thenReturn(response);

        mockMvc.perform(get("/slib/seat-status-reports/{id}", reportId)
                        .with(securityContext(studentDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reporterId").value(studentId.toString()));
    }

    // =========================================
    // === UTCID03: Student accesses another student's report ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Student accesses another student's report - returns 403 Forbidden")
    void getById_studentAccessOther_returns403() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        UUID otherReporterId = UUID.randomUUID();
        SeatStatusReportResponse response = SeatStatusReportResponse.builder()
                .id(reportId).reporterId(otherReporterId).seatId(101)
                .issueType("BROKEN").status("PENDING").build();
        when(seatStatusReportService.getById(reportId)).thenReturn(response);

        mockMvc.perform(get("/slib/seat-status-reports/{id}", reportId)
                        .with(securityContext(studentDetails())))
                .andExpect(status().isForbidden());
    }

    // =========================================
    // === UTCID04: Unknown report id ===
    // =========================================
    @Test
    @DisplayName("UTCID04: Unknown report id - returns 404 Not Found")
    void getById_unknownId_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        when(seatStatusReportService.getById(unknownId))
                .thenThrow(new ResourceNotFoundException("SeatStatusReport", "id", unknownId));

        mockMvc.perform(get("/slib/seat-status-reports/{id}", unknownId)
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isNotFound());
    }

    // =========================================
    // === UTCID05: Unexpected lookup failure ===
    // =========================================
    @Test
    @DisplayName("UTCID05: Unexpected lookup failure - returns 500")
    void getById_unexpectedFailure_returns500() throws Exception {
        when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockLibrarian()));
        when(seatStatusReportService.getById(reportId))
                .thenThrow(new RuntimeException("Database connection error"));

        mockMvc.perform(get("/slib/seat-status-reports/{id}", reportId)
                        .with(securityContext(librarianDetails())))
                .andExpect(status().isInternalServerError());
    }
}
