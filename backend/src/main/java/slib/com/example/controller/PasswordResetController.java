package slib.com.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;
import slib.com.example.security.JwtService;
import slib.com.example.service.AuthService;
import slib.com.example.service.OtpService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/librarian")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {

    private final OtpService otpService;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    /**
     * Bước 1: Gửi OTP đến email
     * POST /api/librarian/forgot-password
     * Body: { "email": "user@example.com" }
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email là bắt buộc"));
        }

        log.info("Nhận yêu cầu quên mật khẩu cho: {}", email);

        try {
            otpService.sendPasswordResetOtp(email.trim().toLowerCase());
            return ResponseEntity.ok(Map.of(
                    "message", "Mã OTP đã được gửi đến email của bạn",
                    "email", email));
        } catch (RuntimeException e) {
            // Business errors (email not found, no email, etc.)
            log.warn("Forgot password business error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi gửi OTP: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Không thể gửi OTP. Vui lòng thử lại sau."));
        }
    }

    /**
     * Bước 2: Xác thực OTP
     * POST /api/librarian/verify-otp
     * Body: { "email": "...", "token": "123456", "type": "recovery" }
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String token = request.get("token");

        if (email == null || token == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email và mã OTP là bắt buộc"));
        }

        log.info("Xác thực OTP cho: {}", email);

        boolean isValid = otpService.verifyOtp(email.trim().toLowerCase(), token.trim());

        if (!isValid) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Mã OTP không đúng hoặc đã hết hạn"));
        }

        // Tạo temporary token để dùng cho bước reset password
        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy người dùng"));
        }

        String resetToken = jwtService.generateAccessToken(userOpt.get());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Xác thực OTP thành công",
                "result", "{\"access_token\": \"" + resetToken + "\"}"));
    }

    /**
     * Gửi lại OTP
     * POST /api/librarian/resend-otp
     * Body: { "email": "...", "type": "recovery" }
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email là bắt buộc"));
        }

        log.info("Gửi lại OTP cho: {}", email);

        try {
            otpService.resendOtp(email.trim().toLowerCase());
            return ResponseEntity.ok(Map.of("message", "Mã OTP mới đã được gửi đến email của bạn"));
        } catch (Exception e) {
            log.error("Lỗi gửi lại OTP: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Không thể gửi lại OTP. Vui lòng thử lại sau."));
        }
    }

    /**
     * Bước 3: Đặt mật khẩu mới
     * POST /api/librarian/update-password
     * Header: Authorization: Bearer <reset_token>
     * Body: { "password": "newPassword123" }
     */
    @PostMapping("/update-password")
    public ResponseEntity<Map<String, String>> updatePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {

        String newPassword = request.get("password");

        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Mật khẩu phải có ít nhất 6 ký tự"));
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Token không hợp lệ"));
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtService.extractEmail(token);

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Token không hợp lệ"));
            }

            authService.updatePassword(email, newPassword);
            log.info("Đặt lại mật khẩu thành công cho: {}", email);

            return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công"));
        } catch (Exception e) {
            log.error("Lỗi đặt lại mật khẩu: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage()));
        }
    }
}
