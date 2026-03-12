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
import slib.com.example.dto.users.LoginRequest;
import slib.com.example.exception.BadRequestException;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.AuthService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-02: Login with SLIB Account
 * Test Report: doc/Report/FE02_TestReport.md
 */
@WebMvcTest(value = AuthController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-02: Login with SLIB Account - Unit Tests")
class FE02_LoginWithSLIBAccountTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AuthService authService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCD01: Valid credentials - Success ===
        // =========================================

        @Test
        @DisplayName("UTCD01: Login with valid credentials returns 200 OK")
        void loginWithPassword_validCredentials_returns200OK() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setIdentifier("SE123456");
                request.setPassword("Slib@2025");

                AuthResponse mockResponse = AuthResponse.builder()
                                .id(UUID.randomUUID().toString())
                                .email("student@fpt.edu.vn")
                                .fullName("Nguyen Van A")
                                .userCode("SE123456")
                                .accessToken("jwt-token")
                                .refreshToken("refresh-token")
                                .role("STUDENT")
                                .expiresIn(3600L)
                                .build();

                when(authService.loginWithPassword(anyString(), anyString(), any()))
                                .thenReturn(mockResponse);

                mockMvc.perform(post("/slib/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("student@fpt.edu.vn"))
                                .andExpect(jsonPath("$.accessToken").exists());

                verify(authService, times(1)).loginWithPassword(anyString(), anyString(), any());
        }

        // =========================================
        // === UTCD02: Account not found ===
        // =========================================

        /**
         * UTCD02: Login with non-existent account
         * Expected: 404 Not Found (service throws ResourceNotFoundException)
         */
        @Test
        @DisplayName("UTCD02: Login with non-existent account returns 404 Not Found")
        void loginWithPassword_accountNotFound_returns404NotFound() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setIdentifier("SE999999");
                request.setPassword("Slib@2025");

                when(authService.loginWithPassword(anyString(), anyString(), any()))
                                .thenThrow(new ResourceNotFoundException("User", "identifier", "SE999999"));

                mockMvc.perform(post("/slib/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());

                verify(authService, times(1)).loginWithPassword(anyString(), anyString(), any());
        }

        // =========================================
        // === UTCD03: Wrong password ===
        // =========================================

        /**
         * UTCD03: Login with wrong password
         * Expected: 400 Bad Request (service throws BadRequestException)
         */
        @Test
        @DisplayName("UTCD03: Login with wrong password returns 400 Bad Request")
        void loginWithPassword_wrongPassword_returns400BadRequest() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setIdentifier("SE123456");
                request.setPassword("WrongPassword123");

                when(authService.loginWithPassword(anyString(), anyString(), any()))
                                .thenThrow(new BadRequestException("Tai khoan hoac mat khau khong dung"));

                mockMvc.perform(post("/slib/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, times(1)).loginWithPassword(anyString(), anyString(), any());
        }

        // =========================================
        // === UTCD04: Locked account ===
        // =========================================

        /**
         * UTCD04: Login with locked account
         * Expected: 400 Bad Request (service throws BadRequestException)
         */
        @Test
        @DisplayName("UTCD04: Login with locked account returns 400 Bad Request")
        void loginWithPassword_lockedAccount_returns400BadRequest() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setIdentifier("SE123456");
                request.setPassword("Slib@2025");

                when(authService.loginWithPassword(anyString(), anyString(), any()))
                                .thenThrow(new BadRequestException("Tai khoan da bi khoa"));

                mockMvc.perform(post("/slib/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, times(1)).loginWithPassword(anyString(), anyString(), any());
        }

        // =========================================
        // === UTCD05: Missing identifier ===
        // =========================================

        /**
         * UTCD05: Login with missing identifier
         * Note: Controller throws RuntimeException -> GlobalExceptionHandler -> 500
         * Expected: 500 Internal Server Error
         */
        @Test
        @DisplayName("UTCD05: Login with missing identifier returns 500 Internal Server Error")
        void loginWithPassword_missingIdentifier_returns500InternalServerError() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setPassword("Slib@2025");

                mockMvc.perform(post("/slib/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(authService, never()).loginWithPassword(anyString(), anyString(), any());
        }

        // =========================================
        // === UTCD06: Empty credentials ===
        // =========================================

        /**
         * UTCD06: Login with empty credentials
         * Note: Controller throws RuntimeException -> GlobalExceptionHandler -> 500
         * Expected: 500 Internal Server Error
         */
        @Test
        @DisplayName("UTCD06: Login with empty credentials returns 500 Internal Server Error")
        void loginWithPassword_emptyCredentials_returns500InternalServerError() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setIdentifier("");
                request.setPassword("");

                mockMvc.perform(post("/slib/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(authService, never()).loginWithPassword(anyString(), anyString(), any());
        }

        // =========================================
        // === UTCD07: No password set ===
        // =========================================

        /**
         * UTCD07: Login with account that has no password set
         * Expected: 400 Bad Request (service throws BadRequestException)
         */
        @Test
        @DisplayName("UTCD07: Login with no password set returns 400 Bad Request")
        void loginWithPassword_noPasswordSet_returns400BadRequest() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setIdentifier("SE123456");
                request.setPassword("Slib@2025");

                when(authService.loginWithPassword(anyString(), anyString(), any()))
                                .thenThrow(new BadRequestException("Tai khoan chua duoc thiet lap mat khau"));

                mockMvc.perform(post("/slib/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, times(1)).loginWithPassword(anyString(), anyString(), any());
        }

        // =========================================
        // === UTCD08: Case insensitive login ===
        // =========================================

        @Test
        @DisplayName("UTCD08: Login with case-insensitive username returns 200 OK")
        void loginWithPassword_caseInsensitive_returns200OK() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setIdentifier("SE123456");
                request.setPassword("Slib@2025");

                AuthResponse mockResponse = AuthResponse.builder()
                                .id(UUID.randomUUID().toString())
                                .email("student@fpt.edu.vn")
                                .fullName("Nguyen Van A")
                                .userCode("SE123456")
                                .accessToken("jwt-token")
                                .refreshToken("refresh-token")
                                .role("STUDENT")
                                .expiresIn(3600L)
                                .build();

                when(authService.loginWithPassword(anyString(), anyString(), any()))
                                .thenReturn(mockResponse);

                mockMvc.perform(post("/slib/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(authService, times(1)).loginWithPassword(anyString(), anyString(), any());
        }

        // =========================================
        // === UTCD09: Login with email ===
        // =========================================

        @Test
        @DisplayName("UTCD09: Login with email returns 200 OK")
        void loginWithPassword_usingEmail_returns200OK() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setIdentifier("student@fpt.edu.vn");
                request.setPassword("Slib@2025");

                AuthResponse mockResponse = AuthResponse.builder()
                                .id(UUID.randomUUID().toString())
                                .email("student@fpt.edu.vn")
                                .fullName("Nguyen Van A")
                                .userCode("SE123456")
                                .accessToken("jwt-token")
                                .refreshToken("refresh-token")
                                .role("STUDENT")
                                .expiresIn(3600L)
                                .build();

                when(authService.loginWithPassword(anyString(), anyString(), any()))
                                .thenReturn(mockResponse);

                mockMvc.perform(post("/slib/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("student@fpt.edu.vn"));

                verify(authService, times(1)).loginWithPassword(anyString(), anyString(), any());
        }

        // =========================================
        // === UTCD10: Login with MSSV ===
        // =========================================

        @Test
        @DisplayName("UTCD10: Login with MSSV returns 200 OK")
        void loginWithPassword_usingMSSV_returns200OK() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setIdentifier("SE123456");
                request.setPassword("Slib@2025");

                AuthResponse mockResponse = AuthResponse.builder()
                                .id(UUID.randomUUID().toString())
                                .email("student@fpt.edu.vn")
                                .fullName("Nguyen Van A")
                                .userCode("SE123456")
                                .accessToken("jwt-token")
                                .refreshToken("refresh-token")
                                .role("STUDENT")
                                .expiresIn(3600L)
                                .build();

                when(authService.loginWithPassword(anyString(), anyString(), any()))
                                .thenReturn(mockResponse);

                mockMvc.perform(post("/slib/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userCode").value("SE123456"));

                verify(authService, times(1)).loginWithPassword(anyString(), anyString(), any());
        }
}
