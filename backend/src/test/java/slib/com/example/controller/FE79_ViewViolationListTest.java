package slib.com.example.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.feedback.SeatViolationReportController;
import slib.com.example.dto.feedback.ViolationReportResponse;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.feedback.SeatViolationReportService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-79: View Violation List
 * Test Report: doc/Report/UnitTestReport/FE79_TestReport.md
 */
@WebMvcTest(value = SeatViolationReportController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-79: View Violation List - Unit Tests")
class FE79_ViewViolationListTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SeatViolationReportService violationReportService;

        @MockBean
        private UserRepository userRepository;

        private void mockCurrentUser(UUID userId) {
                User user = new User();
                user.setId(userId);
                user.setEmail("test@fpt.edu.vn");
                when(userRepository.findByEmail(eq("test@fpt.edu.vn"))).thenReturn(Optional.of(user));
        }

        // =========================================
        // === UTCID01: View my reports with data - Normal ===
        // =========================================

        @Test
        @WithMockUser(username = "test@fpt.edu.vn")
        @DisplayName("UTCID01: Get my violation reports with data returns 200 OK")
        void getMyReports_withData_returns200OK() throws Exception {
                UUID userId = UUID.randomUUID();
                mockCurrentUser(userId);

                ViolationReportResponse report1 = ViolationReportResponse.builder()
                                .id(UUID.randomUUID())
                                .violationType("NOISE")
                                .status("PENDING")
                                .build();
                ViolationReportResponse report2 = ViolationReportResponse.builder()
                                .id(UUID.randomUUID())
                                .violationType("FOOD")
                                .status("VERIFIED")
                                .build();

                when(violationReportService.getMyReports(eq(userId)))
                                .thenReturn(List.of(report1, report2));

                mockMvc.perform(get("/slib/violation-reports/my"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2));

                verify(violationReportService, times(1)).getMyReports(eq(userId));
        }

        // =========================================
        // === UTCID02: View my reports with single record - Abnormal ===
        // =========================================

        @Test
        @WithMockUser(username = "test@fpt.edu.vn")
        @DisplayName("UTCID02: Get my violation reports with single record returns 200 OK")
        void getMyReports_singleRecord_returns200OK() throws Exception {
                UUID userId = UUID.randomUUID();
                mockCurrentUser(userId);

                ViolationReportResponse report = ViolationReportResponse.builder()
                                .id(UUID.randomUUID())
                                .violationType("DAMAGE")
                                .status("PENDING")
                                .build();

                when(violationReportService.getMyReports(eq(userId)))
                                .thenReturn(List.of(report));

                mockMvc.perform(get("/slib/violation-reports/my"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(1));

                verify(violationReportService, times(1)).getMyReports(eq(userId));
        }

        // =========================================
        // === UTCID03: View violations against me - Abnormal ===
        // =========================================

        @Test
        @WithMockUser(username = "test@fpt.edu.vn")
        @DisplayName("UTCID03: Get violations against me returns 200 OK")
        void getViolationsAgainstMe_withData_returns200OK() throws Exception {
                UUID userId = UUID.randomUUID();
                mockCurrentUser(userId);

                ViolationReportResponse report = ViolationReportResponse.builder()
                                .id(UUID.randomUUID())
                                .violationType("NOISE")
                                .status("VERIFIED")
                                .build();

                when(violationReportService.getViolationsAgainstMe(eq(userId)))
                                .thenReturn(List.of(report));

                mockMvc.perform(get("/slib/violation-reports/against-me"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(1));

                verify(violationReportService, times(1)).getViolationsAgainstMe(eq(userId));
        }

        // =========================================
        // === UTCID04: No reports found - Abnormal ===
        // =========================================

        @Test
        @WithMockUser(username = "test@fpt.edu.vn")
        @DisplayName("UTCID04: Get my violation reports with no data returns 200 OK with empty list")
        void getMyReports_noData_returns200OKEmptyList() throws Exception {
                UUID userId = UUID.randomUUID();
                mockCurrentUser(userId);

                when(violationReportService.getMyReports(eq(userId)))
                                .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/violation-reports/my"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(violationReportService, times(1)).getMyReports(eq(userId));
        }

        // =========================================
        // === UTCID05: Service throws exception - Abnormal ===
        // =========================================

        @Test
        @WithMockUser(username = "test@fpt.edu.vn")
        @DisplayName("UTCID05: Get my violation reports when service fails returns 500 Internal Server Error")
        void getMyReports_serviceFails_returns500InternalServerError() throws Exception {
                UUID userId = UUID.randomUUID();
                mockCurrentUser(userId);

                when(violationReportService.getMyReports(eq(userId)))
                                .thenThrow(new RuntimeException("Database connection failed"));

                mockMvc.perform(get("/slib/violation-reports/my"))
                                .andExpect(status().isInternalServerError());

                verify(violationReportService, times(1)).getMyReports(eq(userId));
        }
}
