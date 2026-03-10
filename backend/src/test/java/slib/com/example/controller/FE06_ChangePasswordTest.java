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

        // UTCD01: Valid data - Success
        @Test
        @DisplayName("UTCD01: Change password with valid data returns 200 OK")
        void changePassword_validData_returns200OK() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("currentPassword", "Old@123");
                request.put("newPassword", "New@456");

                doNothing().when(authService).changePassword(anyString(), anyString(), anyString());

                mockMvc.perform(post("/slib/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").exists());

                verify(authService, times(1)).changePassword(anyString(), anyString(), anyString());
        }

        // UTCD02: No token - 401
        @Test
        @DisplayName("UTCD02: Change password without token returns 401 Unauthorized")
        void changePassword_noToken_returns401Unauthorized() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("currentPassword", "Old@123");
                request.put("newPassword", "New@456");

                mockMvc.perform(post("/slib/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
        }

        // UTCD03: Wrong current password - 401
        @Test
        @DisplayName("UTCD03: Wrong current password returns 401 Unauthorized")
        void changePassword_wrongCurrentPassword_returns401Unauthorized() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("currentPassword", "Wrong@123");
                request.put("newPassword", "New@456");

                doThrow(new RuntimeException("Mật khẩu hiện tại không đúng"))
                        .when(authService).changePassword(anyString(), anyString(), anyString());

                mockMvc.perform(post("/slib/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
        }

        // UTCD04: New password too short - 400
        @Test
        @DisplayName("UTCD04: New password too short returns 400 Bad Request")
        void changePassword_passwordTooShort_returns400BadRequest() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("currentPassword", "Old@123");
                request.put("newPassword", "New@1");

                doThrow(new RuntimeException("Mật khẩu phải có ít nhất 8 ký tự"))
                        .when(authService).changePassword(anyString(), anyString(), anyString());

                mockMvc.perform(post("/slib/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
        }

        // UTCD07: Same as old password - 400
        @Test
        @DisplayName("UTCD07: New password same as old returns 400 Bad Request")
        void changePassword_sameAsOld_returns400BadRequest() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("currentPassword", "Old@123");
                request.put("newPassword", "Old@123");

                doThrow(new RuntimeException("Mật khẩu mới trùng với mật khẩu cũ"))
                        .when(authService).changePassword(anyString(), anyString(), anyString());

                mockMvc.perform(post("/slib/auth/change-password")
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isInternalServerError());
        }
}
