package slib.com.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/slib/hce")
public class HCEController {

    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(@RequestBody Map<String, String> payload) {
        // 1. Lấy dữ liệu từ JSON gửi lên
        String token = payload.get("token");
        String gateId = payload.get("gateId");

        // 2. In ra màn hình Console (Terminal) của IntelliJ/Eclipse
        System.out.println("========================================");
        System.out.println("⚡ CÓ TÍN HIỆU TỪ CỔNG HCE!");
        System.out.println("📍 Cổng số: " + gateId);
        System.out.println("🔑 Token nhận được: " + token);
        System.out.println("========================================");

        // 3. Trả về OK (200) để Raspberry Pi kêu bíp và hiện đèn xanh
        return ResponseEntity.ok().body("Server đã nhận được tin nhắn!");
    }
}