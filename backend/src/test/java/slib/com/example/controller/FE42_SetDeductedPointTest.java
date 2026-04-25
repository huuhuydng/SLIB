package slib.com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import slib.com.example.controller.feedback.SeatViolationReportController;
import slib.com.example.dto.feedback.ViolationReportResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.feedback.SeatViolationReportService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-42: Set the deducted point for each reputation rule
 * Test Report: doc/Report/UnitTestReport/FE35_TestReport.md
 */
@WebMvcTest(value = SeatViolationReportController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-42: Set the deducted point for each reputation rule - Unit Tests")
class FE42_SetDeductedPointTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SeatViolationReportService violationReportService;

        @MockBean
        private UserRepository userRepository;

        @Autowired
        private ObjectMapper objectMapper;

        private RequestPostProcessor authenticatedUser(String email, String role) {
                return request -> {
                        var user = org.springframework.security.core.userdetails.User.withUsername(email)
                                        .password("pass").roles(role).build();
                        SecurityContextHolder.getContext().setAuthentication(
                                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
                        return request;
                };
        }

        @BeforeEach
        void clearSecurityContext() {
                SecurityContextHolder.clearContext();
        }

        // =========================================
        // === UTCID01: Verify report - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Verify report with valid JWT token returns 200 OK")
        void verifyReport_validToken_returns200OK() throws Exception {
                UUID reportId = UUID.randomUUID();
                ViolationReportResponse response = ViolationReportResponse.builder().build();

                when(violationReportService.verifyReport(eq(reportId), any(UUID.class)))
                                .thenReturn(response);

                slib.com.example.entity.users.User mockUser = new slib.com.example.entity.users.User();
                mockUser.setId(UUID.randomUUID());
                mockUser.setEmail("admin@fpt.edu.vn");

                when(userRepository.findByEmail("admin@fpt.edu.vn")).thenReturn(java.util.Optional.of(mockUser));

                mockMvc.perform(put("/slib/violation-reports/" + reportId + "/verify")
                                .with(authenticatedUser("admin@fpt.edu.vn", "ADMIN")))
                                .andExpect(status().isOk());
        }

        // =========================================
        // === UTCID02: No authentication - AccessDeniedException ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Verify report without authentication returns 403 Forbidden")
        void verifyReport_noAuth_returns403() throws Exception {
                UUID reportId = UUID.randomUUID();

                mockMvc.perform(put("/slib/violation-reports/" + reportId + "/verify"))
                                .andExpect(status().isForbidden());
        }

        // =========================================
        // === UTCID03: Non-admin - Forbidden ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Verify report with non-admin JWT returns 403 Forbidden")
        void verifyReport_nonAdmin_returns403Forbidden() throws Exception {
                UUID reportId = UUID.randomUUID();

                slib.com.example.entity.users.User mockUser = new slib.com.example.entity.users.User();
                mockUser.setId(UUID.randomUUID());
                mockUser.setEmail("student@fpt.edu.vn");

                when(userRepository.findByEmail("student@fpt.edu.vn")).thenReturn(java.util.Optional.of(mockUser));
                when(violationReportService.verifyReport(eq(reportId), any(UUID.class)))
                                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                                                "Khong co quyen truy cap"));

                mockMvc.perform(put("/slib/violation-reports/" + reportId + "/verify")
                                .with(authenticatedUser("student@fpt.edu.vn", "STUDENT")))
                                .andExpect(status().isForbidden());
        }

        // =========================================
        // === UTCID04: Invalid point - Bad Request ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Verify report with invalid point returns 400 Bad Request")
        void verifyReport_invalidPoint_returns400BadRequest() throws Exception {
                UUID reportId = UUID.randomUUID();

                slib.com.example.entity.users.User mockUser = new slib.com.example.entity.users.User();
                mockUser.setId(UUID.randomUUID());
                mockUser.setEmail("admin@fpt.edu.vn");

                when(userRepository.findByEmail("admin@fpt.edu.vn")).thenReturn(java.util.Optional.of(mockUser));
                when(violationReportService.verifyReport(eq(reportId), any(UUID.class)))
                                .thenThrow(new slib.com.example.exception.BadRequestException(
                                                "Diem tru khong hop le"));

                mockMvc.perform(put("/slib/violation-reports/" + reportId + "/verify")
                                .with(authenticatedUser("admin@fpt.edu.vn", "ADMIN")))
                                .andExpect(status().isBadRequest());
        }

        // =========================================
        // === UTCID05: Non-admin role ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Verify report with non-admin role returns 403 Forbidden")
        void verifyReport_nonAdminRole_returns403Forbidden() throws Exception {
                UUID reportId = UUID.randomUUID();

                slib.com.example.entity.users.User mockUser = new slib.com.example.entity.users.User();
                mockUser.setId(UUID.randomUUID());
                mockUser.setEmail("librarian@fpt.edu.vn");

                when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(java.util.Optional.of(mockUser));
                when(violationReportService.verifyReport(eq(reportId), any(UUID.class)))
                                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                                                "Khong co quyen truy cap"));

                mockMvc.perform(put("/slib/violation-reports/" + reportId + "/verify")
                                .with(authenticatedUser("librarian@fpt.edu.vn", "LIBRARIAN")))
                                .andExpect(status().isForbidden());
        }
}
