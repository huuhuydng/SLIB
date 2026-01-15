package slib.com.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.LibrarianLoginRequest;
import slib.com.example.dto.LibrarianLoginResponse;
import slib.com.example.service.LibrarianService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/librarian")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")  // ✅ THÊM CORS - Cho phép frontend gọi API
public class LibrarianController {

    private final LibrarianService librarianService;

    /**
     * API đăng nhập cho LIBRARIAN
     * POST /api/librarian/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LibrarianLoginRequest request) {
        try {
            log.info("Nhận yêu cầu đăng nhập từ librarian: {}", request.getEmail());
            
            LibrarianLoginResponse response = librarianService.login(request);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Lỗi khi đăng nhập librarian: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            
        } catch (Exception e) {
            log.error("Lỗi không xác định", e);
            
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi hệ thống, vui lòng thử lại sau");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * API đăng xuất cho LIBRARIAN
     * POST /api/librarian/logout
     * Header: Authorization: Bearer <token>
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            // Lấy email từ token (nếu cần log activity)
            String userEmail = "unknown";
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                // TODO: Extract email from token if needed
                // userEmail = jwtService.extractUsername(token);
            }
            
            log.info("✅ Librarian đã đăng xuất: {}", userEmail);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Đăng xuất thành công");
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi đăng xuất: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", "Lỗi khi đăng xuất");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ============ NHÓM CHỨC NĂNG QUÊN MẬT KHẨU & OTP ============

    /**
     * API Quên mật khẩu - Gửi OTP qua email
     * POST /api/librarian/forgot-password
     * Body: { "email": "librarian@example.com" }
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (email == null || email.trim().isEmpty()) {
            // ✅ Response JSON format đồng nhất
            Map<String, String> error = new HashMap<>();
            error.put("message", "Email không được để trống");
            return ResponseEntity.badRequest().body(error);
        }
        
        try {
            log.info("🟡 Librarian yêu cầu khôi phục mật khẩu: {}", email);
            
            librarianService.sendRecoveryEmail(email.trim().toLowerCase());
            
            log.info("✅ Email khôi phục đã được gửi đến: {}", email);
            
            // ✅ Response JSON format
            Map<String, String> response = new HashMap<>();
            response.put("message", "Mã OTP đã được gửi đến email của bạn");
            response.put("email", email);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("❌ Lỗi khi gửi email khôi phục: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * API Xác thực OTP
     * POST /api/librarian/verify-otp  ✅ ĐỔI TỪ /verify → /verify-otp
     * Body: { "email": "librarian@example.com", "token": "123456", "type": "recovery" }
     */
    @PostMapping("/verify-otp")  // ✅ THAY ĐỔI ENDPOINT
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String token = request.get("token");
        String type = request.getOrDefault("type", "recovery");

        if (email == null || email.trim().isEmpty() || token == null || token.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Email và token không được để trống");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            log.info("🟡 Librarian xác thực OTP: {}, type: {}", email, type);
            
            String result = librarianService.verifyEmailOtp(
                email.trim().toLowerCase(), 
                token.trim(), 
                type
            );
            
            log.info("✅ Xác thực OTP thành công cho: {}", email);
            
            // ✅ Response JSON format
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Xác thực OTP thành công");
            response.put("email", email);
            response.put("result", result);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("❌ Lỗi xác thực OTP: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * API Gửi lại OTP
     * POST /api/librarian/resend-otp
     * Body: { "email": "librarian@example.com", "type": "recovery" }
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String type = request.getOrDefault("type", "recovery");

        if (email == null || email.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Email không được để trống");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            log.info("🟡 Librarian yêu cầu gửi lại OTP: {}, type: {}", email, type);
            
            librarianService.resendOtp(email.trim().toLowerCase(), type);
            
            log.info("✅ OTP đã được gửi lại đến: {}", email);
            
            // ✅ Response JSON format
            Map<String, String> response = new HashMap<>();
            response.put("message", "Mã OTP mới đã được gửi đến email của bạn");
            response.put("email", email);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("❌ Lỗi gửi lại OTP: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * API Đặt lại mật khẩu mới
     * POST /api/librarian/update-password  ✅ ĐỔI TỪ /reset-password → /update-password
     * Header: Authorization: Bearer <Token_nhan_duoc_tu_verify>
     * Body: { "password": "newpassword123" }
     */
    @PostMapping("/update-password")  // ✅ THAY ĐỔI ENDPOINT
    public ResponseEntity<?> updatePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> request) {
        
        // ✅ Validate Authorization header
        if (authHeader == null || authHeader.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Token xác thực không được cung cấp");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        
        String newPassword = request.get("password");
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Mật khẩu không được để trống");
            return ResponseEntity.badRequest().body(error);
        }
        
        if (newPassword.length() < 6) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Mật khẩu phải có ít nhất 6 ký tự");
            return ResponseEntity.badRequest().body(error);
        }

        // Lấy token từ Header (Cắt bỏ chữ "Bearer ")
        String userToken = authHeader.replace("Bearer ", "").trim();

        try {
            log.info("🟡 Librarian đặt lại mật khẩu mới");
            
            String result = librarianService.updatePassword(userToken, newPassword);
            
            log.info("✅ Đổi mật khẩu thành công");
            
            // ✅ Response JSON format
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đặt lại mật khẩu thành công");
            response.put("result", result);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("❌ Lỗi đổi mật khẩu: {}", e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}