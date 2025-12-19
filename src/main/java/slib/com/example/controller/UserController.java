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

        try {
            String result = userService.verifyEmailOtp(email, token);
            return ResponseEntity.ok(result); // Trả về Token để Flutter tự lưu và login luôn
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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

    
}