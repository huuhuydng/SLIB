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
import slib.com.example.dto.users.AuthResponse;
import slib.com.example.exception.BadRequestException;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.AuthService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-01: Login with Google Account
 * Test Report: doc/Report/FE01_TestReport.md
 */
@WebMvcTest(value = AuthController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-01: Login with Google Account - Unit Tests")
class FE01_LoginWithGoogleTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AuthService authService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCD01: Valid credentials - Success ===
        // =========================================

        /**
         * UTCD01: Login with valid Google token - existing FPT account
         * Precondition: Account exist with FPT email in database
         * Expected: 200 OK
         */
        @Test
        @DisplayName("UTCD01: Login with valid Google token - existing FPT account returns 200 OK")
        void loginWithGoogle_existingFPTAccount_returns200OK() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("idToken", "valid.google.token");
                request.put("fullName", "Nguyen Van A");
                request.put("fcmToken", "fcm-123");
                request.put("deviceInfo", "iPhone 15");

                AuthResponse mockResponse = AuthResponse.builder()
                                .id(UUID.randomUUID().toString())
                                .email("a.nguyenvan@fpt.edu.vn")
                                .fullName("Nguyen Van A")
                                .userCode("SE123456")
                                .accessToken("jwt-token")
                                .refreshToken("refresh-token")
                                .role("STUDENT")
                                .expiresIn(3600L)
                                .build();

                when(authService.loginWithGoogle(anyString(), any(), any(), any()))
                                .thenReturn(mockResponse);

                mockMvc.perform(post("/slib/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("a.nguyenvan@fpt.edu.vn"))
                                .andExpect(jsonPath("$.accessToken").exists());

                verify(authService, times(1)).loginWithGoogle(anyString(), any(), any(), any());
        }

        // =========================================
        // === UTCD02: First time login - create new account ===
        // =========================================

        /**
         * UTCD02: First time login - create new account
         * Precondition: No account in system
         * Expected: 200 OK (new user created)
         */
        @Test
        @DisplayName("UTCD02: First time login - create new account returns 200 OK")
        void loginWithGoogle_newUser_returns200OK() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("idToken", "valid.google.token.newuser");
                request.put("fullName", "Le Thi B");
                request.put("fcmToken", "fcm-456");

                AuthResponse mockResponse = AuthResponse.builder()
                                .id(UUID.randomUUID().toString())
                                .email("b.lethi@fpt.edu.vn")
                                .fullName("Le Thi B")
                                .userCode("SE789012")
                                .accessToken("jwt-token-new")
                                .refreshToken("refresh-token-new")
                                .role("STUDENT")
                                .expiresIn(3600L)
                                .build();

                when(authService.loginWithGoogle(anyString(), any(), any(), any()))
                                .thenReturn(mockResponse);

                mockMvc.perform(post("/slib/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("b.lethi@fpt.edu.vn"));

                verify(authService, times(1)).loginWithGoogle(anyString(), any(), any(), any());
        }

        // =========================================
        // === UTCD03: Non-FPT email rejected ===
        // =========================================

        /**
         * UTCD03: Login with non-FPT email - rejected
         * Precondition: Account with non-FPT email
         * Expected: 400 Bad Request (service throws BadRequestException)
         */
        @Test
        @DisplayName("UTCD03: Login with non-FPT email returns 400 Bad Request")
        void loginWithGoogle_nonFPTEmail_returns400BadRequest() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("idToken", "valid.google.token");
                request.put("email", "user@gmail.com");

                when(authService.loginWithGoogle(anyString(), any(), any(), any()))
                                .thenThrow(new BadRequestException("Chi chap nhan email @fpt.edu.vn hoac email trong whitelist"));

                mockMvc.perform(post("/slib/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, times(1)).loginWithGoogle(anyString(), any(), any(), any());
        }

        // =========================================
        // === UTCD04: Locked account ===
        // =========================================

        /**
         * UTCD04: Login with locked account
         * Precondition: Account is locked (isActive=false)
         * Expected: 400 Bad Request (service throws BadRequestException)
         */
        @Test
        @DisplayName("UTCD04: Login with locked account returns 400 Bad Request")
        void loginWithGoogle_lockedAccount_returns400BadRequest() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("idToken", "valid.google.token");
                request.put("email", "locked@fpt.edu.vn");

                when(authService.loginWithGoogle(anyString(), any(), any(), any()))
                                .thenThrow(new BadRequestException("Tai khoan da bi khoa"));

                mockMvc.perform(post("/slib/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, times(1)).loginWithGoogle(anyString(), any(), any(), any());
        }

        // =========================================
        // === UTCD05: Missing token ===
        // =========================================

        /**
         * UTCD05: Login with missing token
         * Precondition: No precondition
         * Expected: 500 Internal Server Error (controller throws RuntimeException)
         * Note: The controller throws RuntimeException("ID token is required")
         * which is handled by GlobalExceptionHandler as 500
         */
        @Test
        @DisplayName("UTCD05: Login with missing token returns 500 Internal Server Error")
        void loginWithGoogle_missingToken_returns500InternalServerError() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("fullName", "Test User");

                mockMvc.perform(post("/slib/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(authService, never()).loginWithGoogle(anyString(), any(), any(), any());
        }

        // =========================================
        // === UTCD06: Expired token ===
        // =========================================

        /**
         * UTCD06: Login with expired token
         * Precondition: Token expired
         * Expected: 400 Bad Request (service throws BadRequestException)
         */
        @Test
        @DisplayName("UTCD06: Login with expired token returns 400 Bad Request")
        void loginWithGoogle_expiredToken_returns400BadRequest() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("idToken", "expired.google.token");

                when(authService.loginWithGoogle(anyString(), any(), any(), any()))
                                .thenThrow(new BadRequestException("Google ID token khong hop le"));

                mockMvc.perform(post("/slib/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, times(1)).loginWithGoogle(anyString(), any(), any(), any());
        }

        // =========================================
        // === UTCD07: Whitelisted email ===
        // =========================================

        /**
         * UTCD07: Login with whitelisted email (non-FPT)
         * Precondition: FPT email in whitelist
         * Expected: 200 OK
         */
        @Test
        @DisplayName("UTCD07: Login with whitelisted email returns 200 OK")
        void loginWithGoogle_whitelistedEmail_returns200OK() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("idToken", "valid.google.token");
                request.put("email", "huuhuydng@gmail.com");
                request.put("fullName", "Huy Nguyen");

                AuthResponse mockResponse = AuthResponse.builder()
                                .id(UUID.randomUUID().toString())
                                .email("huuhuydng@gmail.com")
                                .fullName("Huy Nguyen")
                                .userCode("HUYNGUYEN")
                                .accessToken("jwt-token-whitelist")
                                .refreshToken("refresh-token-whitelist")
                                .role("STUDENT")
                                .expiresIn(3600L)
                                .build();

                when(authService.loginWithGoogle(anyString(), any(), any(), any()))
                                .thenReturn(mockResponse);

                mockMvc.perform(post("/slib/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("huuhuydng@gmail.com"));

                verify(authService, times(1)).loginWithGoogle(anyString(), any(), any(), any());
        }

        // =========================================
        // === UTCD08: System error ===
        // =========================================

        /**
         * UTCD08: System error during login process
         * Precondition: No precondition
         * Expected: 500 Internal Server Error
         */
        @Test
        @DisplayName("UTCD08: System error returns 500 Internal Server Error")
        void loginWithGoogle_systemError_returns500InternalServerError() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("idToken", "valid.google.token");

                when(authService.loginWithGoogle(anyString(), any(), any(), any()))
                                .thenThrow(new RuntimeException("Database connection failed"));

                mockMvc.perform(post("/slib/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(authService, times(1)).loginWithGoogle(anyString(), any(), any(), any());
        }
}
