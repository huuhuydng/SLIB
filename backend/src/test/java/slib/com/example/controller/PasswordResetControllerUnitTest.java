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
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.UserRepository;
import slib.com.example.security.JwtService;
import slib.com.example.service.AuthService;
import slib.com.example.service.OtpService;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for PasswordResetController
 */
@WebMvcTest(value = PasswordResetController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PasswordResetController Unit Tests")
class PasswordResetControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OtpService otpService;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================
    // === FORGOT PASSWORD ===
    // =========================================

    @Test
    @DisplayName("forgotPassword_validEmail_returns200")
    void forgotPassword_validEmail_returns200() throws Exception {
        doNothing().when(otpService).sendPasswordResetOtp("test@example.com");

        mockMvc.perform(post("/api/librarian/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "test@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mã OTP đã được gửi đến email của bạn"));

        verify(otpService).sendPasswordResetOtp("test@example.com");
    }

    @Test
    @DisplayName("forgotPassword_blankEmail_returns400")
    void forgotPassword_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/librarian/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("forgotPassword_serviceThrowsRuntimeException_returns400")
    void forgotPassword_serviceThrowsRuntimeException_returns400() throws Exception {
        doThrow(new RuntimeException("Email not found")).when(otpService).sendPasswordResetOtp("bad@example.com");

        mockMvc.perform(post("/api/librarian/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "bad@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email not found"));
    }

    // =========================================
    // === VERIFY OTP ===
    // =========================================

    @Test
    @DisplayName("verifyOtp_validOtp_returns200WithToken")
    void verifyOtp_validOtp_returns200WithToken() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");

        when(otpService.verifyOtp("test@example.com", "123456")).thenReturn(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("mock-token");

        mockMvc.perform(post("/api/librarian/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "test@example.com", "token", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("verifyOtp_invalidOtp_returns400")
    void verifyOtp_invalidOtp_returns400() throws Exception {
        when(otpService.verifyOtp("test@example.com", "000000")).thenReturn(false);

        mockMvc.perform(post("/api/librarian/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "test@example.com", "token", "000000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("verifyOtp_missingFields_returns400")
    void verifyOtp_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/librarian/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "test@example.com"))))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === RESEND OTP ===
    // =========================================

    @Test
    @DisplayName("resendOtp_validEmail_returns200")
    void resendOtp_validEmail_returns200() throws Exception {
        doNothing().when(otpService).resendOtp("test@example.com");

        mockMvc.perform(post("/api/librarian/resend-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "test@example.com"))))
                .andExpect(status().isOk());

        verify(otpService).resendOtp("test@example.com");
    }

    @Test
    @DisplayName("resendOtp_blankEmail_returns400")
    void resendOtp_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/librarian/resend-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", ""))))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === UPDATE PASSWORD ===
    // =========================================

    @Test
    @DisplayName("updatePassword_validRequest_returns200")
    void updatePassword_validRequest_returns200() throws Exception {
        when(jwtService.extractEmail("valid-token")).thenReturn("test@example.com");
        doNothing().when(authService).updatePassword("test@example.com", "newPassword123");

        mockMvc.perform(post("/api/librarian/update-password")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", "newPassword123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(authService).updatePassword("test@example.com", "newPassword123");
    }

    @Test
    @DisplayName("updatePassword_shortPassword_returns400")
    void updatePassword_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/librarian/update-password")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", "123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("updatePassword_invalidAuthHeader_returns400")
    void updatePassword_invalidAuthHeader_returns400() throws Exception {
        mockMvc.perform(post("/api/librarian/update-password")
                .header("Authorization", "InvalidHeader")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", "newPassword123"))))
                .andExpect(status().isBadRequest());
    }
}
