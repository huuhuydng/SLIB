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
public class HCEController {
@Autowired
    private CheckInService checkInService;

    @Value("${gate.secret}")
    private String gateSecretKey;

    
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

}