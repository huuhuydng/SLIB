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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.dto.users.UserProfileResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.UserService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-04: View Profile
 * Test Report: doc/Report/FE04_TestReport.md
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-04: View Profile - Unit Tests")
class FE04_ViewProfileTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @Autowired
        private ObjectMapper objectMapper;

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

                mockMvc.perform(get("/slib/users/me"))
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

                mockMvc.perform(get("/slib/users/me"))
                        .andExpect(status().isNotFound());
        }

        // UTCD06: System error - 500
        @Test
        @DisplayName("UTCD06: System error returns 500 Internal Server Error")
        void viewProfile_systemError_returns500InternalServerError() throws Exception {
                when(userService.getMyProfile(anyString()))
                        .thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(get("/slib/users/me"))
                        .andExpect(status().isInternalServerError());
        }
}
