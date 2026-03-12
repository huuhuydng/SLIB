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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import slib.com.example.controller.users.StudentProfileController;
import slib.com.example.dto.users.StudentProfileResponse;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.StudentProfileService;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-77: View Reputation Score
 * Test Report: doc/Report/FE77_TestReport.md
 */
@WebMvcTest(value = StudentProfileController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-77: View Reputation Score - Unit Tests")
class FE77_ViewReputationTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private StudentProfileService studentProfileService;

        private final UUID studentId = UUID.randomUUID();

        private User mockUser() {
                User u = new User();
                u.setId(studentId);
                u.setEmail("student@fpt.edu.vn");
                u.setFullName("Nguyen Van A");
                u.setRole(Role.STUDENT);
                return u;
        }

        private RequestPostProcessor authenticatedUser(User user) {
                return request -> {
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
                        return request;
                };
        }

        @Test
        @DisplayName("UTCD01: View student profile returns 200 OK")
        void viewReputation_validToken_returns200OK() throws Exception {
                User user = mockUser();
                StudentProfileResponse response = StudentProfileResponse.builder()
                        .userId(studentId).reputationScore(100).build();
                when(studentProfileService.getOrCreateProfile(any(User.class))).thenReturn(response);

                mockMvc.perform(get("/slib/student-profile/me")
                                .with(authenticatedUser(user)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.reputationScore").value(100));
        }

        @Test
        @DisplayName("UTCD02: View student profile by userId returns 200 OK")
        void viewReputation_byUserId_returns200OK() throws Exception {
                StudentProfileResponse response = StudentProfileResponse.builder()
                        .userId(studentId).reputationScore(85).build();
                when(studentProfileService.getProfileByUserId(studentId))
                        .thenReturn(java.util.Optional.of(response));

                mockMvc.perform(get("/slib/student-profile/{userId}", studentId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.reputationScore").value(85));
        }
}
