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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.users.StudentProfileController;
import slib.com.example.dto.users.StudentProfileResponse;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.users.StudentProfileService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-81: View history of changed reputation points
 * Test Report: doc/Report/UnitTestReport/FE77_TestReport.md
 */
@WebMvcTest(value = StudentProfileController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-81: View history of changed reputation points - Unit Tests")
class FE81_ViewHistoryPointsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private StudentProfileService studentProfileService;

        // =========================================
        // === UTCID01: View profile with reputation score - Normal ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Get student profile by userId with reputation score returns 200 OK")
        void getProfileByUserId_withScore_returns200OK() throws Exception {
                UUID userId = UUID.randomUUID();
                StudentProfileResponse mockProfile = StudentProfileResponse.builder()
                                .userId(userId)
                                .reputationScore(85)
                                .totalStudyHours(120.5)
                                .violationCount(2)
                                .build();

                when(studentProfileService.getProfileByUserId(eq(userId))).thenReturn(Optional.of(mockProfile));

                mockMvc.perform(get("/slib/student-profile/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reputationScore").value(85))
                                .andExpect(jsonPath("$.violationCount").value(2));

                verify(studentProfileService, times(1)).getProfileByUserId(eq(userId));
        }

        // =========================================
        // === UTCID02: View profile with zero violations - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get student profile with zero violations returns 200 OK")
        void getProfileByUserId_zeroViolations_returns200OK() throws Exception {
                UUID userId = UUID.randomUUID();
                StudentProfileResponse mockProfile = StudentProfileResponse.builder()
                                .userId(userId)
                                .reputationScore(100)
                                .totalStudyHours(50.0)
                                .violationCount(0)
                                .build();

                when(studentProfileService.getProfileByUserId(eq(userId))).thenReturn(Optional.of(mockProfile));

                mockMvc.perform(get("/slib/student-profile/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reputationScore").value(100))
                                .andExpect(jsonPath("$.violationCount").value(0));

                verify(studentProfileService, times(1)).getProfileByUserId(eq(userId));
        }

        // =========================================
        // === UTCID03: View profile with low reputation - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get student profile with low reputation returns 200 OK")
        void getProfileByUserId_lowReputation_returns200OK() throws Exception {
                UUID userId = UUID.randomUUID();
                StudentProfileResponse mockProfile = StudentProfileResponse.builder()
                                .userId(userId)
                                .reputationScore(20)
                                .totalStudyHours(10.0)
                                .violationCount(8)
                                .build();

                when(studentProfileService.getProfileByUserId(eq(userId))).thenReturn(Optional.of(mockProfile));

                mockMvc.perform(get("/slib/student-profile/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reputationScore").value(20));

                verify(studentProfileService, times(1)).getProfileByUserId(eq(userId));
        }

        // =========================================
        // === UTCID04: Profile not found - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Get student profile when not found returns 404 Not Found")
        void getProfileByUserId_notFound_returns404NotFound() throws Exception {
                UUID userId = UUID.randomUUID();

                when(studentProfileService.getProfileByUserId(eq(userId))).thenReturn(Optional.empty());

                mockMvc.perform(get("/slib/student-profile/{userId}", userId))
                                .andExpect(status().isNotFound());

                verify(studentProfileService, times(1)).getProfileByUserId(eq(userId));
        }

        // =========================================
        // === UTCID05: Invalid userId format - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get student profile with invalid userId format returns 400 Bad Request")
        void getProfileByUserId_invalidUserId_returns400BadRequest() throws Exception {
                mockMvc.perform(get("/slib/student-profile/{userId}", "invalid-uuid"))
                                .andExpect(status().isBadRequest());
        }
}
