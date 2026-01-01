package slib.com.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import slib.com.example.dto.UserProfileResponse;
import slib.com.example.entity.UserEntity;
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

    // --- API QUAN TRỌNG NHẤT: LOGIN GOOGLE ---
    @PostMapping("/login-google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody Map<String, String> request) {
        String idToken = request.get("id_token");
        String fullName = request.get("full_name");

        if (idToken == null || idToken.isEmpty()) {
            return ResponseEntity.badRequest().body("Thiếu Google ID Token");
        }

        try {
            // Hàm này giờ trả về Map (token + user)
            Map<String, Object> response = userService.loginWithGoogle(idToken, fullName);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Lỗi: " + e.getMessage());
        }
    }

    // --- LẤY THÔNG TIN CÁ NHÂN ---
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Token không hợp lệ hoặc hết hạn");
        }
        try {
            // userDetails.getUsername() trả về email (do config trong UserEntity)
            UserProfileResponse profile = userService.getMyProfile(userDetails.getUsername());
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Không tìm thấy user: " + e.getMessage());
        }
    }

    // --- UPDATE (Vd: FCM Token) ---
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody UserEntity userDetails) {
        try {
            UserEntity updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- ADMIN (Optional) ---
    @GetMapping("/getall")
    public List<UserEntity> getAllUsers() {
        return userService.getAllUsers();
    }
}