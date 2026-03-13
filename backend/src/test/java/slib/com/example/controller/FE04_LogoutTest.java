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
 * Unit Tests for FE-04: Logout
 * Test Report: doc/Report/FE04_TestReport.md
 */
@WebMvcTest(value = AuthController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-04: Logout - Unit Tests")
class FE04_LogoutTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AuthService authService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCD01: Logout successful ===
        // =========================================

        @Test
        @DisplayName("UTCD01: Logout with valid refresh token returns 200 OK")
        void logout_validToken_returns200OK() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("refreshToken", "valid.refresh.token");

                doNothing().when(authService).logout(anyString());

                mockMvc.perform(post("/slib/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("\u0110\u0103ng xu\u1EA5t th\u00E0nh c\u00F4ng"));

                verify(authService, times(1)).logout(anyString());
        }

        // =========================================
        // === UTCD02: No token provided ===
        // =========================================

        @Test
        @DisplayName("UTCD02: Logout without token returns 200 OK")
        void logout_noToken_returns200OK() throws Exception {
                Map<String, String> request = new HashMap<>();

                mockMvc.perform(post("/slib/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("\u0110\u0103ng xu\u1EA5t th\u00E0nh c\u00F4ng"));

                verify(authService, never()).logout(anyString());
        }

        // =========================================
        // === UTCD03: Token already revoked ===
        // =========================================

        /**
         * UTCD03: Logout with already revoked token
         * Expected: 400 Bad Request (service throws BadRequestException)
         */
        @Test
        @DisplayName("UTCD03: Logout with already revoked token returns 400 Bad Request")
        void logout_alreadyRevokedToken_returns400BadRequest() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("refreshToken", "already.revoked.token");

                doThrow(new BadRequestException("Refresh token da bi thu hoi"))
                                .when(authService).logout(anyString());

                mockMvc.perform(post("/slib/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, times(1)).logout(anyString());
        }

        // =========================================
        // === UTCD04: Invalid token format ===
        // =========================================

        /**
         * UTCD04: Logout with invalid token format
         * Expected: 400 Bad Request (service throws BadRequestException)
         */
        @Test
        @DisplayName("UTCD04: Logout with invalid token format returns 400 Bad Request")
        void logout_invalidTokenFormat_returns400BadRequest() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("refreshToken", "invalid.format");

                doThrow(new BadRequestException("Token khong hop le"))
                                .when(authService).logout(anyString());

                mockMvc.perform(post("/slib/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, times(1)).logout(anyString());
        }

        // =========================================
        // === UTCD05: Token expired ===
        // =========================================

        /**
         * UTCD05: Logout with expired token
         * Expected: 400 Bad Request (service throws BadRequestException)
         */
        @Test
        @DisplayName("UTCD05: Logout with expired token returns 400 Bad Request")
        void logout_expiredToken_returns400BadRequest() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("refreshToken", "expired.token");

                doThrow(new BadRequestException("Refresh token da het han"))
                                .when(authService).logout(anyString());

                mockMvc.perform(post("/slib/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, times(1)).logout(anyString());
        }

        // =========================================
        // === UTCD07: System error ===
        // =========================================

        @Test
        @DisplayName("UTCD07: System error during logout returns 500 Internal Server Error")
        void logout_systemError_returns500InternalServerError() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("refreshToken", "any.token");

                doThrow(new RuntimeException("Database connection failed"))
                                .when(authService).logout(anyString());

                mockMvc.perform(post("/slib/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(authService, times(1)).logout(anyString());
        }
}
