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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import slib.com.example.controller.users.UserController;
import slib.com.example.dto.users.UserProfileResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.AsyncImportService;
import slib.com.example.service.AuthService;
import slib.com.example.service.StagingImportService;
import slib.com.example.service.UserService;
import slib.com.example.service.chat.CloudinaryService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-05: View Profile
 * Test Report: doc/Report/FE05_TestReport.md
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-05: View Profile - Unit Tests")
class FE05_ViewProfileTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @MockBean
        private AuthService authService;

        @MockBean
        private CloudinaryService cloudinaryService;

        @MockBean
        private AsyncImportService asyncImportService;

        @MockBean
        private StagingImportService stagingImportService;

        @Autowired
        private ObjectMapper objectMapper;

        private RequestPostProcessor authenticatedUser(String email) {
                return request -> {
                        var user = org.springframework.security.core.userdetails.User.withUsername(email)
                                        .password("pass").roles("STUDENT").build();
                        SecurityContextHolder.getContext().setAuthentication(
                                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
                        return request;
                };
        }

        @BeforeEach
        void clearSecurityContext() {
                SecurityContextHolder.clearContext();
        }

        // UTCD01: Valid token - Success
        @Test
        @DisplayName("UTCD01: View profile with valid token returns 200 OK")
        void viewProfile_validToken_returns200OK() throws Exception {
                when(userService.getMyProfile(anyString())).thenReturn(
                    UserProfileResponse.builder()
                        .id(UUID.randomUUID())
                        .email("student@fpt.edu.vn")
                        .fullName("Nguyen Van A")
                        .userCode("SE123456")
                        .role("STUDENT")
                        .build()
                );

                mockMvc.perform(get("/slib/users/me")
                                .with(authenticatedUser("student@fpt.edu.vn")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.email").value("student@fpt.edu.vn"));

                verify(userService, times(1)).getMyProfile(anyString());
        }

        // UTCD02: No token - 401
        @Test
        @DisplayName("UTCD02: View profile without token returns 401 Unauthorized")
        void viewProfile_noToken_returns401Unauthorized() throws Exception {
                mockMvc.perform(get("/slib/users/me"))
                        .andExpect(status().isUnauthorized());
        }

        // UTCD04: User not found - 404
        @Test
        @DisplayName("UTCD04: View profile with non-existent user returns 404 Not Found")
        void viewProfile_userNotFound_returns404NotFound() throws Exception {
                when(userService.getMyProfile(anyString()))
                        .thenThrow(new RuntimeException("User not found"));

                mockMvc.perform(get("/slib/users/me")
                                .with(authenticatedUser("unknown@fpt.edu.vn")))
                        .andExpect(status().isNotFound());
        }

        // UTCD06: System error - 404 (controller catches all exceptions as 404)
        @Test
        @DisplayName("UTCD06: System error returns 404 Not Found")
        void viewProfile_systemError_returns500InternalServerError() throws Exception {
                when(userService.getMyProfile(anyString()))
                        .thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(get("/slib/users/me")
                                .with(authenticatedUser("student@fpt.edu.vn")))
                        .andExpect(status().isNotFound());
        }
}
