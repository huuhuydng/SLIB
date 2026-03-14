package slib.com.example.controller.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.users.AuthResponse;
import slib.com.example.dto.users.ChangePasswordRequest;
import slib.com.example.dto.users.LoginRequest;
import slib.com.example.service.auth.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/slib/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Login with Google ID token
     * 
     * Request body:
     * {
     * "idToken": "Google ID token from frontend",
     * "fullName": "optional full name from client",
     * "fcmToken": "optional FCM token for push notifications",
     * "deviceInfo": "optional device info"
     * }
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");
        String fullName = request.get("fullName");
        String fcmToken = request.get("fcmToken");
        String deviceInfo = request.get("deviceInfo");

        if (idToken == null || idToken.isEmpty()) {
            throw new RuntimeException("ID token is required");
        }

        AuthResponse response = authService.loginWithGoogle(idToken, fullName, fcmToken, deviceInfo);
        return ResponseEntity.ok(response);
    }

    /**
     * Login with email/username/MSSV and password
     * 
     * Request body:
     * {
     * "identifier": "email hoặc username hoặc MSSV",
     * "password": "password123"
     * }
     * 
     * Hoặc (backward compatible):
     * {
     * "email": "user@example.com",
     * "password": "password123"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginWithPassword(
            @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Device-Info", required = false) String deviceInfo) {

        String identifier = request.getLoginIdentifier();
        if (identifier == null || identifier.isEmpty()) {
            throw new RuntimeException("Email hoặc tên đăng nhập là bắt buộc");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new RuntimeException("Mật khẩu là bắt buộc");
        }

        AuthResponse response = authService.loginWithPassword(
                identifier,
                request.getPassword(),
                deviceInfo);
        return ResponseEntity.ok(response);
    }

    /**
     * Change password for authenticated user
     * 
     * Request body:
     * {
     * "currentPassword": "old password",
     * "newPassword": "new password"
     * }
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordRequest request) {

        if (userDetails == null) {
            throw new RuntimeException("Không có quyền truy cập");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            throw new RuntimeException("Mật khẩu mới là bắt buộc");
        }

        authService.changePassword(
                userDetails.getUsername(),
                request.getCurrentPassword(),
                request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }

    /**
     * Admin reset password for user (sets to default password)
     */
    @PostMapping("/admin-reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminResetPassword(@RequestBody Map<String, String> request) {
        String userEmail = request.get("email");

        if (userEmail == null || userEmail.isEmpty()) {
            throw new RuntimeException("Email là bắt buộc");
        }

        authService.adminResetPassword(userEmail);
        return ResponseEntity.ok(Map.of("message", "Đã reset mật khẩu về mặc định cho user: " + userEmail));
    }

    /**
     * Refresh access token
     * 
     * Request body:
     * {
     * "refreshToken": "refresh token from previous login"
     * }
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new RuntimeException("Refresh token is required");
        }

        AuthResponse response = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout - revoke current refresh token
     * 
     * Request body:
     * {
     * "refreshToken": "refresh token to revoke"
     * }
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken != null && !refreshToken.isEmpty()) {
            authService.logout(refreshToken);
        }

        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
    }

    /**
     * Logout all devices - revoke all refresh tokens for current user
     */
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new RuntimeException("Không có quyền truy cập");
        }

        authService.logoutAllDevices(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Đã đăng xuất khỏi tất cả thiết bị"));
    }
}
