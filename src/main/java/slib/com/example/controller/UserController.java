package slib.com.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import slib.com.example.entity.UserEntity;
import slib.com.example.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import slib.com.example.service.VerificationService;


@RestController
@RequestMapping("/slib/users") // Đặt đường dẫn chung cho gọn (VD: http://host/slib/users/...)
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserController {

    @Autowired
    UserService userService;
    @Autowired
    VerificationService verificationService;

    // 1. Lấy tất cả user
    // URL: GET http://localhost:8080/slib/users/getall
    @GetMapping("/getall")
    public List<UserEntity> getAllUsers() {
        return userService.getAllUsers();
    }

    // 2. Tìm user theo Email
    // URL: GET http://localhost:8080/slib/users/find-by-email?email=abc@test.com
    @GetMapping("/find-by-email")
    public ResponseEntity<?> getUserByEmail(@RequestParam String email) {
        UserEntity user = userService.getUserByEmail(email);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(404).body("Không tìm thấy email này");
        }
    }

    // 3. Tìm user theo MSSV
    // URL: GET http://localhost:8080/slib/users/find-by-code?studentCode=SE123456
    @GetMapping("/find-by-code")
    public ResponseEntity<?> getUserByStudentCode(@RequestParam String studentCode) {
        UserEntity user = userService.getUserByStudentCode(studentCode);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(404).body("Không tìm thấy mã sinh viên này");
        }
    }

    // 4. Tạo User mới (QUAN TRỌNG: Sửa @RequestParam -> @RequestBody)
    // URL: POST http://localhost:8080/slib/users/create
    // Body (JSON): { "studentCode": "...", "email": "..." }
    @PostMapping("/create")
    public ResponseEntity<?> createUser(@RequestBody UserEntity user) {
        boolean success = userService.createUser(user);
        if (success) {
            return ResponseEntity.ok("Tạo user thành công!");
        } else {
            return ResponseEntity.badRequest().body("Lỗi: Email hoặc Mã SV đã tồn tại.");
        }
    }

    // 5. Cập nhật User
    // URL: PUT http://localhost:8080/slib/users/update
    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody UserEntity user) {
        boolean success = userService.updateUser(user);
        if (success) {
            return ResponseEntity.ok("Cập nhật thành công!");
        } else {
            return ResponseEntity.badRequest().body("Lỗi: Không tìm thấy User để cập nhật (Thiếu ID).");
        }
    }

    // 6. Xóa User (Sửa logic: Xóa theo ID thay vì gửi cả object)
    // URL: DELETE http://localhost:8080/slib/users/delete?id=...
    // Lưu ý: Bạn cần update UserService hoặc dùng Repository để xóa theo ID
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(@RequestParam UUID id) {
        // Tạm thời tạo 1 object giả chỉ chứa ID để gọi hàm deleteUser của bạn
        UserEntity userToDelete = new UserEntity();
        userToDelete.setUserId(id);

        boolean success = userService.deleteUser(userToDelete);
        if (success) {
            return ResponseEntity.ok("Đã xóa user.");
        } else {
            return ResponseEntity.status(404).body("Xóa thất bại.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserEntity user) {
        boolean success = userService.checkUserAuth(
                user.getEmail(),
                user.getPassword()
        );
        if (success) {
            //ko trả mk ra ngoài
            UserEntity userEntity = userService.getUserByEmail(user.getEmail());
            userEntity.setPassword(null);
            return ResponseEntity.ok(userEntity);
        } else {
            return ResponseEntity.status(401).body("Đăng nhập thất bại. Sai email hoặc mật khẩu.");
        }
    }


    // Bước 1: gửi mã xác nhận
    @PostMapping("/register-request")
    public ResponseEntity<?> registerRequest(@RequestBody UserEntity user) {
        if (user.getEmail() == null || !user.getEmail().toLowerCase().endsWith("@fpt.edu.vn")) {
            return ResponseEntity.badRequest().body("Chỉ chấp nhận email FPT (...@fpt.edu.vn)");
        }
        if (userService.getUserByEmail(user.getEmail()) != null) {
            return ResponseEntity.badRequest().body("Email đã tồn tại.");
        }

        String code = verificationService.generateCode();
        verificationService.saveCode(user.getEmail(), code);
        verificationService.sendVerificationEmail(user.getEmail(), code);

        return ResponseEntity.ok("Mã xác nhận đã được gửi tới email của bạn.");
    }
    // Bước 2: xác nhận mã và tạo user
    @PostMapping("/register-confirm")
    public ResponseEntity<?> registerConfirm(@RequestBody Map<String, String> load) {
        String email = load.get("email");
        String code = load.get("code");
        String fullname = load.get("fullName");
        String studentCode = load.get("studentCode");
        String password = load.get("password");

        if (!verificationService.verifyCode(email, code)) {
            return ResponseEntity.status(400).body("Mã xác nhận không đúng hoặc đã hết hạn.");
        }

        UserEntity newUser = UserEntity.builder()
                .email(email)
                .fullName(fullname)
                .studentCode(studentCode)
                .password(password)
                .role("student")
                .reputationScore(100)
                .build();

        boolean success = userService.createUser(newUser);
        if (success) {
            verificationService.removeCode(email);
            newUser.setPassword(null);
            return ResponseEntity.ok(newUser);
        } else {
            return ResponseEntity.badRequest().body("Tạo user thất bại. Vui lòng thử lại.");
        }

    }


    @GetMapping("/check_user_auth")
    public String getMethodName(@RequestParam String param) {
        return new String();
    }

}