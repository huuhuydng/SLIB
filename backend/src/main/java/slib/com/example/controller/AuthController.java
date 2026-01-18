package slib.com.example.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.users.AuthResponse;
import slib.com.example.service.AuthService;

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
