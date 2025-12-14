package slib.com.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import slib.com.example.entity.UserEntity;
import slib.com.example.service.UserService;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/slib/users") // Đặt đường dẫn chung cho gọn (VD: http://host/slib/users/...)
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserController {

    @Autowired
    UserService userService;

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

    @GetMapping("/check_user_auth")
    public String getMethodName(@RequestParam String param) {
        return new String();
    }
    
}