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
@CrossOrigin(origins = "*", allowedHeaders = "*") // cho phép Flutter gọi vào
public class UserController {

    private final UserService userService;

    // Constructor Injection (Thay cho @Autowired)
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 1. NHÓM CHỨC NĂNG AUTH (Đăng ký / Đăng nhập)

    // API: Đăng ký thành viên mới
    // URL: POST http://localhost:8080/slib/users/register
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        try {
            String response = userService.registerUser(request);
            return ResponseEntity.ok(response); // Trả về JSON user từ Supabase
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API: Đăng nhập
    // URL: POST http://localhost:8080/slib/users/login
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> loginData) {
        String email = loginData.get("email");
        String password = loginData.get("password");

        try {
            // Lấy Token JWT từ Supabase
            String token = userService.login(email, password);
            return ResponseEntity.ok(token);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Đăng nhập thất bại: " + e.getMessage());
        }
    }

    // API: Xác thực OTP email
    // URL: POST http://localhost:8080/slib/users/verify
    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String token = request.get("token");

        // Lấy type từ Flutter gửi lên (ví dụ: "signup", "recovery", "magiclink")
        // Nếu Flutter không gửi, mặc định là "signup"
        String type = request.getOrDefault("type", "signup");

        try {
            // Truyền type vào Service
            String result = userService.verifyEmailOtp(email, token, type);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API: Gửi lại OTP
    // URL: POST http://localhost:8080/slib/users/resend-otp
    // Body: { "email": "...", "type": "signup" }
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String type = request.get("type"); // Có thể null

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng cung cấp Email.");
        }

        try {
            // Gọi service
            userService.resendOtp(email, type);
            // Supabase trả về JSON rỗng {} nếu thành công, nên ta tự trả về message cho dễ
            // hiểu
            return ResponseEntity.ok("Đã gửi lại mã OTP thành công! Vui lòng kiểm tra email.");
        } catch (RuntimeException e) {
            // Xử lý lỗi (ví dụ: gửi quá nhiều lần trong 1 phút)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Quên mật khẩu (Public) -> Gửi OTP
    // POST /slib/users/forgot-password
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

    // Đặt lại mật khẩu mới (Private - Cần Token)
    // POST /slib/users/reset-password
    // Header: Authorization: Bearer <Token_nhan_duoc_tu_verify>
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        String newPassword = request.get("password");

        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("Mật khẩu mới phải từ 6 ký tự.");
        }

        // Lấy token từ Header (Cắt bỏ chữ "Bearer ")
        String userToken = authHeader.replace("Bearer ", "");

        try {
            userService.updatePassword(userToken, newPassword);
            return ResponseEntity.ok("Đổi mật khẩu thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // 2. NHÓM CHỨC NĂNG LẤY DỮ LIỆU (GET)

    // Lấy tất cả user
    // URL: GET http://localhost:8080/slib/users/getall
    @GetMapping("/getall")
    public List<UserEntity> getAllUsers() {
        return userService.getAllUsers();
    }

    // Tìm theo ID
    // URL: GET http://localhost:8080/slib/users/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable UUID id) {
        try {
            UserEntity user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // Tìm theo Email
    // URL: GET http://localhost:8080/slib/users/find-by-email?email=...
    @GetMapping("/find-by-email")
    public ResponseEntity<?> getUserByEmail(@RequestParam String email) {
        try {
            UserEntity user = userService.getUserByEmail(email);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // Tìm theo MSSV
    // URL: GET http://localhost:8080/slib/users/find-by-code?studentCode=...
    @GetMapping("/find-by-code")
    public ResponseEntity<?> getUserByStudentCode(@RequestParam String studentCode) {
        try {
            UserEntity user = userService.getUserByStudentCode(studentCode);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // Lấy profile của chính mình từ token
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Token không hợp lệ hoặc hết hạn");
        }
        // Lấy email từ token đã giải mã
        String email = userDetails.getUsername();
        try {
            UserProfileResponse profile = userService.getMyProfile(email);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Không tìm thấy user: " + e.getMessage());
        }
    }

    // 3. NHÓM CHỨC NĂNG SỬA / XÓA

    // Cập nhật User
    // URL: PUT http://localhost:8080/slib/users/update/{id}
    // Body: { "fullName": "Tên Mới", "notiDevice": "Token Mới" }
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody UserEntity userDetails) {
        try {
            UserEntity updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Xóa User (Chỉ xóa ở bảng Public, user login vẫn còn)
    // URL: DELETE http://localhost:8080/slib/users/delete/{id}
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        boolean isDeleted = userService.deleteUser(id);
        if (isDeleted) {
            return ResponseEntity.ok("Đã xóa hồ sơ thành công.");
        } else {
            return ResponseEntity.status(404).body("Không tìm thấy ID để xóa.");
        }
    }

    // API: Đăng nhập bằng Google
    // URL: POST http://localhost:8080/slib/users/login-google
    @PostMapping("/login-google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody Map<String, String> request) {
        System.out.println("Nhận request /login-google: " + request);

        String idToken = request.get("id_token");
        if (idToken == null || idToken.isEmpty()) {
            return ResponseEntity.badRequest().body("Thiếu Google ID Token");
        }

        try {
            String response = userService.loginWithGoogle(idToken);
            System.out.println("Supabase trả về: " + response);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.err.println("Lỗi xác thực Google: " + e.getMessage());
            return ResponseEntity.status(401).body("Xác thực Google thất bại: " + e.getMessage());
        }
    }

}