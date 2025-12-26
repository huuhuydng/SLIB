package slib.com.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import slib.com.example.dto.RegisterRequest;
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

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        try {
            String response = userService.registerUser(request);
            return ResponseEntity.ok(response); // Trả về JSON user từ Supabase
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> loginData) {
        String email = loginData.get("email");
        String password = loginData.get("password");
        try {
            String token = userService.login(email, password);
            return ResponseEntity.ok(token);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Đăng nhập thất bại: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String token = request.get("token");
        String type = request.get("type");
        try {
            String result = userService.verifyEmailOtp(email, token, type);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String type = request.get("type");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng cung cấp Email.");
        }
        try {
            userService.resendOtp(email, type);
            return ResponseEntity.ok("Đã gửi lại mã OTP thành công! Vui lòng kiểm tra email.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Cần nhập email.");
        }
        try {
            userService.sendRecoveryEmail(email);
            return ResponseEntity.ok("Mã xác thực đã được gửi đến email của bạn.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestHeader("Authorization") String authHeader, 
                                           @RequestBody Map<String, String> request) {
        String newPassword = request.get("password");
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("Mật khẩu mới phải từ 6 ký tự.");
        }
        String userToken = authHeader.replace("Bearer ", "");
        try {
            userService.updatePassword(userToken, newPassword);
            return ResponseEntity.ok("Đổi mật khẩu thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // 2. NHÓM CHỨC NĂNG LẤY DỮ LIỆU (GET)

    @GetMapping("/getall")
    public List<UserEntity> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable UUID id) {
        try {
            UserEntity user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/find-by-email")
    public ResponseEntity<?> getUserByEmail(@RequestParam String email) {
        try {
            UserEntity user = userService.getUserByEmail(email);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/find-by-code")
    public ResponseEntity<?> getUserByStudentCode(@RequestParam String studentCode) {
        try {
            UserEntity user = userService.getUserByStudentCode(studentCode);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }


    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Token không hợp lệ hoặc hết hạn");
        }
        String email = userDetails.getUsername();
        try {
            UserProfileResponse profile = userService.getMyProfile(email);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Không tìm thấy user: " + e.getMessage());
        }
    }

    // 3. NHÓM CHỨC NĂNG SỬA / XÓA

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody UserEntity userDetails) {
        try {
            UserEntity updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        boolean isDeleted = userService.deleteUser(id);
        if (isDeleted) {
            return ResponseEntity.ok("Đã xóa hồ sơ thành công.");
        } else {
            return ResponseEntity.status(404).body("Không tìm thấy ID để xóa.");
        }
    }

}