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
import org.springframework.mock.web.MockMultipartFile;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-94: Create Violation Report
 * Test Report: doc/Report/UnitTestReport/FE94_TestReport.md
 */
@WebMvcTest(value = SeatViolationReportController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-94: Create Violation Report - Unit Tests")
class FE94_CreateViolationReportTest {

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
    // === UTCID01: Valid violation report without images ===
    // =========================================
    @Test
    @DisplayName("UTCID01: Submit valid violation report without images - returns 201 Created")
    void create_validWithoutImages_returns201() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        ViolationReportResponse response = ViolationReportResponse.builder()
                .id(UUID.randomUUID()).reporterId(reporterId).seatId(101)
                .violationType("NOISE").status("PENDING")
                .createdAt(LocalDateTime.now()).build();
        when(violationReportService.createReport(eq(reporterId), any(), isNull())).thenReturn(response);

        mockMvc.perform(multipart("/slib/violation-reports")
                        .param("seatId", "101")
                        .param("violationType", "NOISE")
                        .param("description", "Nguoi ngoi canh gay on ao")
                        .with(securityContext(userDetails())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.violationType").value("NOISE"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // =========================================
    // === UTCID02: Valid violation report with images ===
    // =========================================
    @Test
    @DisplayName("UTCID02: Submit valid violation report with images - returns 201 Created")
    void create_validWithImages_returns201() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        ViolationReportResponse response = ViolationReportResponse.builder()
                .id(UUID.randomUUID()).reporterId(reporterId).seatId(101)
                .violationType("LEFT_BELONGINGS").status("PENDING")
                .evidenceUrl("https://cloudinary.com/evidence.jpg")
                .createdAt(LocalDateTime.now()).build();
        when(violationReportService.createReport(eq(reporterId), any(), any())).thenReturn(response);

        MockMultipartFile imageFile = new MockMultipartFile("images", "evidence.jpg",
                "image/jpeg", "fake-image-content".getBytes());

        mockMvc.perform(multipart("/slib/violation-reports")
                        .file(imageFile)
                        .param("seatId", "101")
                        .param("violationType", "LEFT_BELONGINGS")
                        .param("description", "De do giu cho")
                        .with(securityContext(userDetails())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evidenceUrl").exists());
    }

    // =========================================
    // === UTCID03: Valid report with description only ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Submit valid report with description only - returns 201 Created")
    void create_descriptionOnly_returns201() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        ViolationReportResponse response = ViolationReportResponse.builder()
                .id(UUID.randomUUID()).reporterId(reporterId).seatId(102)
                .violationType("SLEEPING").status("PENDING")
                .description("Nguoi ngu tai cho ngoi")
                .createdAt(LocalDateTime.now()).build();
        when(violationReportService.createReport(eq(reporterId), any(), isNull())).thenReturn(response);

        mockMvc.perform(multipart("/slib/violation-reports")
                        .param("seatId", "102")
                        .param("violationType", "SLEEPING")
                        .param("description", "Nguoi ngu tai cho ngoi")
                        .with(securityContext(userDetails())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.violationType").value("SLEEPING"));
    }

    // =========================================
    // === UTCID04: Reporter or seat lookup fails ===
    // =========================================
    @Test
    @DisplayName("UTCID04: Seat lookup fails - returns 500")
    void create_seatLookupFails_returns500() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        when(violationReportService.createReport(eq(reporterId), any(), any()))
                .thenThrow(new RuntimeException("Seat not found"));

        mockMvc.perform(multipart("/slib/violation-reports")
                        .param("seatId", "99999")
                        .param("violationType", "NOISE")
                        .with(securityContext(userDetails())))
                .andExpect(status().isInternalServerError());
    }

    // =========================================
    // === UTCID05: Upload/save failure ===
    // =========================================
    @Test
    @DisplayName("UTCID05: Upload or save failure - returns 500")
    void create_uploadFailure_returns500() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        when(violationReportService.createReport(eq(reporterId), any(), any()))
                .thenThrow(new RuntimeException("Cloudinary upload failed"));

        MockMultipartFile imageFile = new MockMultipartFile("images", "evidence.jpg",
                "image/jpeg", "fake-image-content".getBytes());

        mockMvc.perform(multipart("/slib/violation-reports")
                        .file(imageFile)
                        .param("seatId", "101")
                        .param("violationType", "NOISE")
                        .with(securityContext(userDetails())))
                .andExpect(status().isInternalServerError());
    }
}
