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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.AuthService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for AuthController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 */
@WebMvcTest(value = AuthController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Unit Tests")
class AuthControllerUnitTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AuthService authService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === GOOGLE LOGIN ENDPOINT ===
        // =========================================

        @Test
        @DisplayName("loginWithGoogle_validIdToken_returns200WithAuthResponse")
        void loginWithGoogle_validIdToken_returns200WithAuthResponse() throws Exception {
                // Arrange
                Map<String, String> request = new HashMap<>();
                request.put("idToken", "valid-google-id-token");
                request.put("fullName", "Test User");
                request.put("fcmToken", "fcm-token-123");
                request.put("deviceInfo", "iOS 17.0");

                AuthResponse mockResponse = AuthResponse.builder()
                                .id(UUID.randomUUID().toString())
                                .email("test@example.com")
                                .fullName("Test User")
                                .accessToken("jwt-access-token")
                                .refreshToken("jwt-refresh-token")
                                .role("STUDENT")
                                .build();

                when(authService.loginWithGoogle(anyString(), anyString(), anyString(), anyString()))
                                .thenReturn(mockResponse);

                // Act & Assert
                mockMvc.perform(post("/slib/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("test@example.com"))
                                .andExpect(jsonPath("$.accessToken").exists())
                                .andExpect(jsonPath("$.role").value("STUDENT"));

                verify(authService, times(1)).loginWithGoogle(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("loginWithGoogle_missingIdToken_throwsException")
        void loginWithGoogle_missingIdToken_throwsException() throws Exception {
                // Arrange
                Map<String, String> request = new HashMap<>();
                request.put("fullName", "Test User");

                // Act & Assert
                mockMvc.perform(post("/slib/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(authService, never()).loginWithGoogle(anyString(), anyString(), anyString(), anyString());
        }

        // =========================================
        // === PASSWORD LOGIN ENDPOINT ===
        // =========================================

        @Test
        @DisplayName("loginWithPassword_validCredentials_returns200WithAuthResponse")
        void loginWithPassword_validCredentials_returns200WithAuthResponse() throws Exception {
                // Arrange
                LoginRequest request = new LoginRequest();
                request.setIdentifier("user@example.com");
                request.setPassword("password123");

                AuthResponse mockResponse = AuthResponse.builder()
                                .id(UUID.randomUUID().toString())
                                .email("user@example.com")
                                .fullName("Test User")
                                .accessToken("jwt-access-token")
                                .refreshToken("jwt-refresh-token")
                                .role("STUDENT")
                                .build();

                when(authService.loginWithPassword(anyString(), anyString(), any()))
                                .thenReturn(mockResponse);

                // Act & Assert
                mockMvc.perform(post("/slib/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("user@example.com"))
                                .andExpect(jsonPath("$.accessToken").exists());

                verify(authService, times(1)).loginWithPassword(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("loginWithPassword_invalidCredentials_throwsException")
        void loginWithPassword_invalidCredentials_throwsException() throws Exception {
                // Arrange
                LoginRequest request = new LoginRequest();
                request.setIdentifier("user@example.com");
                request.setPassword("wrong-password");

                when(authService.loginWithPassword(anyString(), anyString(), any()))
                                .thenThrow(new RuntimeException("Mật khẩu không đúng"));

                // Act & Assert
                mockMvc.perform(post("/slib/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(authService, times(1)).loginWithPassword(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("loginWithPassword_missingIdentifier_throwsException")
        void loginWithPassword_missingIdentifier_throwsException() throws Exception {
                // Arrange
                LoginRequest request = new LoginRequest();
                request.setPassword("password123");

                // Act & Assert
                mockMvc.perform(post("/slib/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(authService, never()).loginWithPassword(anyString(), anyString(), any());
        }

        // =========================================
        // === REFRESH TOKEN ENDPOINT ===
        // =========================================

        @Test
        @DisplayName("refreshToken_validRefreshToken_returns200WithNewTokens")
        void refreshToken_validRefreshToken_returns200WithNewTokens() throws Exception {
                // Arrange
                Map<String, String> request = new HashMap<>();
                request.put("refreshToken", "valid-refresh-token");

                AuthResponse mockResponse = AuthResponse.builder()
                                .accessToken("new-access-token")
                                .refreshToken("new-refresh-token")
                                .build();

                when(authService.refreshAccessToken(anyString())).thenReturn(mockResponse);

                // Act & Assert
                mockMvc.perform(post("/slib/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));

                verify(authService, times(1)).refreshAccessToken(anyString());
        }

        @Test
        @DisplayName("refreshToken_missingRefreshToken_throwsException")
        void refreshToken_missingRefreshToken_throwsException() throws Exception {
                // Arrange
                Map<String, String> request = new HashMap<>();

                // Act & Assert
                mockMvc.perform(post("/slib/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(authService, never()).refreshAccessToken(anyString());
        }

        // =========================================
        // === LOGOUT ENDPOINT ===
        // =========================================

        @Test
        @DisplayName("logout_validRefreshToken_returns200")
        void logout_validRefreshToken_returns200() throws Exception {
                // Arrange
                Map<String, String> request = new HashMap<>();
                request.put("refreshToken", "valid-refresh-token");

                doNothing().when(authService).logout(anyString());

                // Act & Assert
                mockMvc.perform(post("/slib/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));

                verify(authService, times(1)).logout(anyString());
        }

        @Test
        @DisplayName("logout_emptyRefreshToken_returns200WithoutCallingService")
        void logout_emptyRefreshToken_returns200WithoutCallingService() throws Exception {
                // Arrange
                Map<String, String> request = new HashMap<>();

                // Act & Assert
                mockMvc.perform(post("/slib/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));

                verify(authService, never()).logout(anyString());
        }
}
