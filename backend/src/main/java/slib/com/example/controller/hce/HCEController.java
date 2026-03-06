package slib.com.example.controller.hce;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import slib.com.example.dto.hce.AccessLogDTO;
import slib.com.example.dto.hce.AccessLogStatsDTO;
import slib.com.example.dto.hce.CheckInRequest;
import slib.com.example.service.CheckInService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import slib.com.example.dto.hce.StudentDetailDTO;

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

            if (requestKey == null || !requestKey.equals(gateSecretKey)) {
                return ResponseEntity.status(403).body(Map.of(
                        "status", "FORBIDDEN",
                        "message", "Truy cập bị từ chối: Sai API Key bảo mật"));
            }

            Map<String, String> result = checkInService.processCheckIn(request);

            // Trả về 200 OK
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            // Trả về lỗi 400 Bad Request kèm lý do
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()));
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

    /**
     * Lấy danh sách tất cả access logs
     */
    @GetMapping("/access-logs")
    public ResponseEntity<List<AccessLogDTO>> getAllAccessLogs() {
        try {
            List<AccessLogDTO> logs = checkInService.getAllAccessLogs();
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy danh sách access logs hôm nay
     */
    @GetMapping("/access-logs/today")
    public ResponseEntity<List<AccessLogDTO>> getTodayAccessLogs() {
        try {
            List<AccessLogDTO> logs = checkInService.getTodayAccessLogs();
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy thống kê access logs hôm nay
     */
    @GetMapping("/access-logs/stats")
    public ResponseEntity<AccessLogStatsDTO> getTodayStats() {
        try {
            AccessLogStatsDTO stats = checkInService.getTodayStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy danh sách access logs theo khoảng thời gian
     * 
     * @param startDate Ngày bắt đầu (format: yyyy-MM-dd)
     * @param endDate   Ngày kết thúc (format: yyyy-MM-dd)
     */
    @GetMapping("/access-logs/filter")
    public ResponseEntity<List<AccessLogDTO>> getAccessLogsByDateRange(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            java.time.LocalDate start = startDate != null ? java.time.LocalDate.parse(startDate) : null;
            java.time.LocalDate end = endDate != null ? java.time.LocalDate.parse(endDate) : null;

            // If no dates provided, return all logs
            if (start == null && end == null) {
                return ResponseEntity.ok(checkInService.getAllAccessLogs());
            }

            // If only start date, set end date to today
            if (end == null) {
                end = java.time.LocalDate.now();
            }

            // If only end date, set start date to 30 days before end date
            if (start == null) {
                start = end.minusDays(30);
            }

            List<AccessLogDTO> logs = checkInService.getAccessLogsByDateRange(start, end);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Xuất báo cáo Excel
     * 
     * @param startDate Ngày bắt đầu (format: yyyy-MM-dd)
     * @param endDate   Ngày kết thúc (format: yyyy-MM-dd)
     */
    @GetMapping("/access-logs/export")
    public ResponseEntity<byte[]> exportAccessLogsToExcel(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;

            byte[] excelContent = checkInService.exportAccessLogsToExcel(start, end);

            // Tạo tên file với ngày hiện tại
            String filename = "BaoCao_CheckIn_CheckOut_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) +
                    ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelContent.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelContent);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error exporting to Excel: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy chi tiết sinh viên cho thủ thư (chỉ đọc)
     */
    @GetMapping("/student-detail/{userId}")
    public ResponseEntity<StudentDetailDTO> getStudentDetail(@PathVariable String userId) {
        try {
            UUID id = UUID.fromString(userId);
            StudentDetailDTO detail = checkInService.getStudentDetail(id);
            return ResponseEntity.ok(detail);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}