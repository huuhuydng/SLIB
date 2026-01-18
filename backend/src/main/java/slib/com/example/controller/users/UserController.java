package slib.com.example.controller.users;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import slib.com.example.dto.users.AuthResponse;
import slib.com.example.dto.users.UserProfileResponse;
import slib.com.example.entity.users.User;
import slib.com.example.service.AuthService;
import slib.com.example.service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slib/users")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    /**
     * Login with Google ID Token
     * Note: This endpoint wraps the new AuthService for backward compatibility.
     * Consider migrating clients to use /slib/auth/google directly.
     */
    @PostMapping("/login-google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody Map<String, String> request) {
        String idToken = request.get("id_token");
        String fullName = request.get("full_name");
        String fcmToken = request.get("noti_device");
        String deviceInfo = request.get("device_info");

        if (idToken == null || idToken.isEmpty()) {
            return ResponseEntity.badRequest().body("Thiếu Google ID Token");
        }
        try {
            AuthResponse response = authService.loginWithGoogle(idToken, fullName, fcmToken, deviceInfo);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Token không hợp lệ hoặc hết hạn");
        }
        try {
            UserProfileResponse profile = userService.getMyProfile(userDetails.getUsername());
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Không tìm thấy user: " + e.getMessage());
        }
    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateMyProfile(@AuthenticationPrincipal UserDetails userDetails,
            @RequestBody User updateRequest) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body("Token không hợp lệ");
        }

        try {
            String email = userDetails.getUsername();
            UserProfileResponse currentProfile = userService.getMyProfile(email);

            User updatedUser = userService.updateUser(currentProfile.getId(), updateRequest);

            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi update: " + e.getMessage());
        }
    }

    @GetMapping("/getall")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }
}