package slib.com.example.controller.hce;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import slib.com.example.dto.hce.CheckInRequest;
import slib.com.example.service.CheckInService;

import java.util.Map;

@RestController
@RequestMapping("/slib/hce")
@CrossOrigin(origins = "*")
public class HCEController {
@Autowired
    CheckInService checkInService;

    @Value("${gate.secret}")
    String gateSecretKey;

    
    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(@RequestBody CheckInRequest request, HttpServletRequest httpRequest) {
        try {
            String requestKey = httpRequest.getHeader("X-API-KEY");

            if(requestKey == null || !requestKey.equals(gateSecretKey)) {
                return ResponseEntity.status(403).body(Map.of(
                    "status", "FORBIDDEN",
                    "message", "Truy cập bị từ chối: Sai API Key bảo mật"
                ));
            }    
            
            Map<String, String> result = checkInService.processCheckIn(request);
            
            // Trả về 200 OK
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            // Trả về lỗi 400 Bad Request kèm lý do
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", e.getMessage()
            ));
        }
    }


    // Lấy 10 bản ghi mới nhất để hiển thị
    @GetMapping("/latest-logs")
    public ResponseEntity<?> getLatestLogs() {
        try {
            return ResponseEntity.ok(checkInService.getLatest10Logs());
        } catch (Exception e) {
            // Trả về empty array thay vì error nếu có exception
            return ResponseEntity.ok(new java.util.ArrayList<>());
        }
    }

}