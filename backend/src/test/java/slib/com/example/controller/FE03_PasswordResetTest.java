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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-03: Password Reset (Forgot Password)
 * Test Report: doc/Report/UnitTestReport/FE03_TestReport.md
 */
@WebMvcTest(value = PasswordResetController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-03: Password Reset - Unit Tests")
class FE03_PasswordResetTest {

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

        // =============================================
        // === FORGOT PASSWORD ===
        // =============================================

        @Test
        @DisplayName("UTCD01: Forgot password with valid email returns 200 OK")
        void forgotPassword_Success() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("email", "librarian@fpt.edu.vn");

                doNothing().when(otpService).sendPasswordResetOtp(anyString());

                mockMvc.perform(post("/slib/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").exists())
                        .andExpect(jsonPath("$.email").value("librarian@fpt.edu.vn"));

                verify(otpService, times(1)).sendPasswordResetOtp("librarian@fpt.edu.vn");
        }

        @Test
        @DisplayName("UTCD02: Forgot password with missing email returns 400 Bad Request")
        void forgotPassword_MissingEmail() throws Exception {
                Map<String, String> request = new HashMap<>();

                mockMvc.perform(post("/slib/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").exists());

                verify(otpService, never()).sendPasswordResetOtp(anyString());
        }

        @Test
        @DisplayName("UTCD03: Forgot password with blank email returns 400 Bad Request")
        void forgotPassword_BlankEmail() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("email", "   ");

                mockMvc.perform(post("/slib/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").exists());

                verify(otpService, never()).sendPasswordResetOtp(anyString());
        }

        @Test
        @DisplayName("UTCD04: Forgot password with non-existent email returns 400 Bad Request")
        void forgotPassword_EmailNotFound() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("email", "unknown@fpt.edu.vn");

                doThrow(new RuntimeException("Khong tim thay nguoi dung"))
                        .when(otpService).sendPasswordResetOtp(anyString());

                mockMvc.perform(post("/slib/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").exists());
        }

        // =============================================
        // === VERIFY OTP ===
        // =============================================

        @Test
        @DisplayName("UTCD05: Verify OTP with valid data returns 200 OK with reset token")
        void verifyOtp_Success() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("email", "librarian@fpt.edu.vn");
                request.put("token", "123456");

                User mockUser = new User();
                mockUser.setEmail("librarian@fpt.edu.vn");

                when(otpService.verifyOtp("librarian@fpt.edu.vn", "123456")).thenReturn(true);
                when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.of(mockUser));
                when(jwtService.generatePasswordResetToken(any(User.class))).thenReturn("reset-jwt-token");

                mockMvc.perform(post("/slib/auth/verify-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").exists())
                        .andExpect(jsonPath("$.result").exists());

                verify(otpService, times(1)).verifyOtp("librarian@fpt.edu.vn", "123456");
                verify(jwtService, times(1)).generatePasswordResetToken(any(User.class));
        }

        @Test
        @DisplayName("UTCD06: Verify OTP with invalid OTP returns 400 Bad Request")
        void verifyOtp_InvalidOtp() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("email", "librarian@fpt.edu.vn");
                request.put("token", "999999");

                when(otpService.verifyOtp("librarian@fpt.edu.vn", "999999")).thenReturn(false);

                mockMvc.perform(post("/slib/auth/verify-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("UTCD07: Verify OTP with missing email or token returns 400 Bad Request")
        void verifyOtp_MissingFields() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("email", "librarian@fpt.edu.vn");
                // token is missing

                mockMvc.perform(post("/slib/auth/verify-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false));

                verify(otpService, never()).verifyOtp(anyString(), anyString());
        }

        @Test
        @DisplayName("UTCD08: Verify OTP with valid OTP but user not found returns 400 Bad Request")
        void verifyOtp_UserNotFound() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("email", "librarian@fpt.edu.vn");
                request.put("token", "123456");

                when(otpService.verifyOtp("librarian@fpt.edu.vn", "123456")).thenReturn(true);
                when(userRepository.findByEmail("librarian@fpt.edu.vn")).thenReturn(Optional.empty());

                mockMvc.perform(post("/slib/auth/verify-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").exists());
        }

        // =============================================
        // === UPDATE PASSWORD ===
        // =============================================

        @Test
        @DisplayName("UTCD09: Update password with valid reset token returns 200 OK")
        void updatePassword_Success() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("password", "NewPassword@123");

                when(jwtService.isPasswordResetToken("valid-reset-token")).thenReturn(true);
                when(jwtService.extractEmail("valid-reset-token")).thenReturn("librarian@fpt.edu.vn");
                doNothing().when(authService).updatePassword(anyString(), anyString());

                mockMvc.perform(post("/slib/auth/update-password")
                                .header("Authorization", "Bearer valid-reset-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").exists());

                verify(authService, times(1)).updatePassword("librarian@fpt.edu.vn", "NewPassword@123");
        }

        @Test
        @DisplayName("UTCD10: Update password with non-reset token returns 403 Forbidden")
        void updatePassword_InvalidResetToken() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("password", "NewPassword@123");

                when(jwtService.isPasswordResetToken("access-token-not-reset")).thenReturn(false);

                mockMvc.perform(post("/slib/auth/update-password")
                                .header("Authorization", "Bearer access-token-not-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.message").exists());

                verify(authService, never()).updatePassword(anyString(), anyString());
        }

        @Test
        @DisplayName("UTCD11: Update password with short password returns 400 Bad Request")
        void updatePassword_ShortPassword() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("password", "abc");

                mockMvc.perform(post("/slib/auth/update-password")
                                .header("Authorization", "Bearer some-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").exists());

                verify(jwtService, never()).isPasswordResetToken(anyString());
                verify(authService, never()).updatePassword(anyString(), anyString());
        }

        @Test
        @DisplayName("UTCD12: Update password without Authorization header returns 500")
        void updatePassword_MissingAuthHeader() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("password", "NewPassword@123");

                mockMvc.perform(post("/slib/auth/update-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("UTCD13: Update password with null password returns 400 Bad Request")
        void updatePassword_NullPassword() throws Exception {
                Map<String, String> request = new HashMap<>();

                mockMvc.perform(post("/slib/auth/update-password")
                                .header("Authorization", "Bearer some-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").exists());
        }

        // =============================================
        // === RESEND OTP ===
        // =============================================

        @Test
        @DisplayName("UTCD14: Resend OTP with valid email returns 200 OK")
        void resendOtp_Success() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("email", "librarian@fpt.edu.vn");

                doNothing().when(otpService).resendOtp(anyString());

                mockMvc.perform(post("/slib/auth/resend-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").exists());

                verify(otpService, times(1)).resendOtp("librarian@fpt.edu.vn");
        }

        @Test
        @DisplayName("UTCD15: Resend OTP with missing email returns 400 Bad Request")
        void resendOtp_MissingEmail() throws Exception {
                Map<String, String> request = new HashMap<>();

                mockMvc.perform(post("/slib/auth/resend-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").exists());

                verify(otpService, never()).resendOtp(anyString());
        }

        @Test
        @DisplayName("UTCD16: Resend OTP with system error returns 500 Internal Server Error")
        void resendOtp_SystemError() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("email", "librarian@fpt.edu.vn");

                doThrow(new RuntimeException("Mail server error"))
                        .when(otpService).resendOtp(anyString());

                mockMvc.perform(post("/slib/auth/resend-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.message").exists());
        }
}
