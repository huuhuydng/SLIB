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
import slib.com.example.controller.feedback.SeatStatusReportController;
import slib.com.example.dto.feedback.SeatStatusReportResponse;
import slib.com.example.exception.BadRequestException;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.feedback.SeatStatusReportService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-89: Create Seat Status Report
 * Test Report: doc/Report/UnitTestReport/FE89_TestReport.md
 */
@WebMvcTest(value = SeatStatusReportController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-89: Create Seat Status Report - Unit Tests")
class FE89_CreateSeatStatusReportTest {

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
    // === UTCID01: Valid report without image ===
    // =========================================
    @Test
    @DisplayName("UTCID01: Submit valid seat status report without image - returns 201 Created")
    void create_validWithoutImage_returns201() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        SeatStatusReportResponse response = SeatStatusReportResponse.builder()
                .id(UUID.randomUUID()).reporterId(studentId).seatId(101)
                .issueType("BROKEN").status("PENDING").createdAt(LocalDateTime.now()).build();
        when(seatStatusReportService.createReport(eq(studentId), any(), isNull())).thenReturn(response);

        mockMvc.perform(multipart("/slib/seat-status-reports")
                        .param("seatId", "101")
                        .param("issueType", "BROKEN")
                        .param("description", "Ghe bi gay chan")
                        .with(securityContext(userDetails())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.issueType").value("BROKEN"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // =========================================
    // === UTCID02: Valid report with image ===
    // =========================================
    @Test
    @DisplayName("UTCID02: Submit valid seat status report with image - returns 201 Created")
    void create_validWithImage_returns201() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        SeatStatusReportResponse response = SeatStatusReportResponse.builder()
                .id(UUID.randomUUID()).reporterId(studentId).seatId(101)
                .issueType("DIRTY").status("PENDING")
                .imageUrl("https://cloudinary.com/image.jpg")
                .createdAt(LocalDateTime.now()).build();
        when(seatStatusReportService.createReport(eq(studentId), any(), any())).thenReturn(response);

        MockMultipartFile imageFile = new MockMultipartFile("image", "photo.jpg",
                "image/jpeg", "fake-image-content".getBytes());

        mockMvc.perform(multipart("/slib/seat-status-reports")
                        .file(imageFile)
                        .param("seatId", "101")
                        .param("issueType", "DIRTY")
                        .param("description", "Ghe ban")
                        .with(securityContext(userDetails())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value("https://cloudinary.com/image.jpg"));
    }

    // =========================================
    // === UTCID03: Invalid issueType ===
    // =========================================
    @Test
    @DisplayName("UTCID03: Submit invalid issueType - returns 400 Bad Request")
    void create_invalidIssueType_returns400() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        when(seatStatusReportService.createReport(eq(studentId), any(), any()))
                .thenThrow(new BadRequestException("Loai van de khong hop le"));

        mockMvc.perform(multipart("/slib/seat-status-reports")
                        .param("seatId", "101")
                        .param("issueType", "INVALID_TYPE")
                        .with(securityContext(userDetails())))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === UTCID04: Unknown seatId ===
    // =========================================
    @Test
    @DisplayName("UTCID04: Submit unknown seatId - returns 400 Bad Request")
    void create_unknownSeatId_returns400() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        when(seatStatusReportService.createReport(eq(studentId), any(), any()))
                .thenThrow(new BadRequestException("Ghe khong ton tai"));

        mockMvc.perform(multipart("/slib/seat-status-reports")
                        .param("seatId", "99999")
                        .param("issueType", "BROKEN")
                        .with(securityContext(userDetails())))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === UTCID05: Unexpected persistence failure ===
    // =========================================
    @Test
    @DisplayName("UTCID05: Unexpected upload or persistence failure - returns 500")
    void create_unexpectedFailure_returns500() throws Exception {
        when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(Optional.of(mockStudent()));
        when(seatStatusReportService.createReport(eq(studentId), any(), any()))
                .thenThrow(new RuntimeException("Cloudinary upload failed"));

        mockMvc.perform(multipart("/slib/seat-status-reports")
                        .param("seatId", "101")
                        .param("issueType", "BROKEN")
                        .with(securityContext(userDetails())))
                .andExpect(status().isInternalServerError());
    }
}
