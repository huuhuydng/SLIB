package slib.com.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import slib.com.example.dto.UserProfileResponse;
import slib.com.example.entity.users.User;
import slib.com.example.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/slib/users")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // --- TEST ENDPOINT: Kiểm tra kết nối ---
    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        System.out.println("🟢 ============================================");
        System.out.println("🟢 PING RECEIVED FROM CLIENT!");
        System.out.println("🟢 ============================================");
        return ResponseEntity.ok(Map.of(
            "message", "Backend is running!",
            "timestamp", System.currentTimeMillis(),
            "ip", "192.168.19.112:8081"
        ));
    }

    // --- API QUAN TRỌNG NHẤT: LOGIN GOOGLE (hỗ trợ cả Mobile & Web) ---
    @PostMapping("/login-google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody Map<String, String> request) {
        System.out.println("🔵 ============================================");
        System.out.println("🔵 [UserController] LOGIN REQUEST RECEIVED!");
        System.out.println("🔵 Request body: " + request);
        System.out.println("🔵 ============================================");
        
        String idToken = request.get("id_token");
        String fullName = request.get("full_name");
        String fcmToken = request.get("noti_device"); // Cho mobile app (optional)

        System.out.println("🔵 idToken: " + (idToken != null ? idToken.substring(0, Math.min(50, idToken.length())) + "..." : "NULL"));
        System.out.println("🔵 fullName: " + fullName);
        System.out.println("🔵 fcmToken: " + fcmToken);

        if (idToken == null || idToken.isEmpty()) {
            System.err.println("❌ Missing id_token!");
            return ResponseEntity.badRequest().body(Map.of("error", "Thiếu Google ID Token"));
        }

        try {
            // Hàm này giờ trả về Map (token + user)
            Map<String, Object> response = userService.loginWithGoogle(idToken, fullName, fcmToken);
            System.out.println("✅ Login successful!");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Trả về JSON với message lỗi rõ ràng (cho web & mobile)
            System.err.println("❌ Login failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(401).body(Map.of(
                "error", e.getMessage(),
                "message", e.getMessage()
            ));
        }
    }

    // --- LẤY THÔNG TIN CÁ NHÂN (dùng chung cho Mobile & Web) ---
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Token không hợp lệ hoặc hết hạn");
        }
        try {
            // userDetails.getUsername() trả về email (do config trong User entity)
            UserProfileResponse profile = userService.getMyProfile(userDetails.getUsername());
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Không tìm thấy user: " + e.getMessage());
        }
    }

    // --- UPDATE PROFILE (cho Mobile app - dùng token) ---
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

    // --- UPDATE USER BY ID (cho Web admin - dùng ID cụ thể) ---
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- ADMIN: Lấy tất cả users (Optional) ---
    @GetMapping("/getall")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }
}