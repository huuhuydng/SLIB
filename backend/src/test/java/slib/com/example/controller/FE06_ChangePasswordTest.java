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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import slib.com.example.exception.BadRequestException;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.AuthService;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-06: Change Password
 * Test Report: doc/Report/FE06_TestReport.md
 */
@WebMvcTest(value = AuthController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-06: Change Password - Unit Tests")
class FE06_ChangePasswordTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AuthService authService;

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

        // UTCD01: Valid data - Success
        @Test
        @DisplayName("UTCD01: Change password with valid data returns 200 OK")
        void changePassword_validData_returns200OK() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("currentPassword", "Old@123");
                request.put("newPassword", "New@456");

                doNothing().when(authService).changePassword(anyString(), anyString(), anyString());

                mockMvc.perform(post("/slib/auth/change-password")
                                .with(authenticatedUser("student@fpt.edu.vn"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").exists());

                verify(authService, times(1)).changePassword(anyString(), anyString(), anyString());
        }

        // UTCD02: No token - 500 (controller throws RuntimeException)
        @Test
        @DisplayName("UTCD02: Change password without token returns 500 Internal Server Error")
        void changePassword_noToken_returns500InternalServerError() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("currentPassword", "Old@123");
                request.put("newPassword", "New@456");

                mockMvc.perform(post("/slib/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isInternalServerError());
        }

        // UTCD03: Wrong current password - 400 (service throws BadRequestException)
        @Test
        @DisplayName("UTCD03: Wrong current password returns 400 Bad Request")
        void changePassword_wrongCurrentPassword_returns400BadRequest() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("currentPassword", "Wrong@123");
                request.put("newPassword", "New@456");

                doThrow(new BadRequestException("Mat khau hien tai khong dung"))
                        .when(authService).changePassword(anyString(), anyString(), anyString());

                mockMvc.perform(post("/slib/auth/change-password")
                                .with(authenticatedUser("student@fpt.edu.vn"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
        }

        // UTCD04: New password too short - 400 (service throws BadRequestException)
        @Test
        @DisplayName("UTCD04: New password too short returns 400 Bad Request")
        void changePassword_passwordTooShort_returns400BadRequest() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("currentPassword", "Old@123");
                request.put("newPassword", "New@1");

                doThrow(new BadRequestException("Mat khau phai co it nhat 8 ky tu"))
                        .when(authService).changePassword(anyString(), anyString(), anyString());

                mockMvc.perform(post("/slib/auth/change-password")
                                .with(authenticatedUser("student@fpt.edu.vn"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
        }

        // UTCD07: Same as old password - 400 (service throws BadRequestException)
        @Test
        @DisplayName("UTCD07: New password same as old returns 400 Bad Request")
        void changePassword_sameAsOld_returns400BadRequest() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("currentPassword", "Old@123");
                request.put("newPassword", "Old@123");

                doThrow(new BadRequestException("Mat khau moi trung voi mat khau cu"))
                        .when(authService).changePassword(anyString(), anyString(), anyString());

                mockMvc.perform(post("/slib/auth/change-password")
                                .with(authenticatedUser("student@fpt.edu.vn"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
        }

        // UTCD09: System error - 500
        @Test
        @DisplayName("UTCD09: System error returns 500 Internal Server Error")
        void changePassword_systemError_returns500InternalServerError() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("currentPassword", "Old@123");
                request.put("newPassword", "New@456");

                doThrow(new RuntimeException("Database error"))
                        .when(authService).changePassword(anyString(), anyString(), anyString());

                mockMvc.perform(post("/slib/auth/change-password")
                                .with(authenticatedUser("student@fpt.edu.vn"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isInternalServerError());
        }
}
