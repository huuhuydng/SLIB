package slib.com.example.controller.hce;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import slib.com.example.dto.hce.HceStationRequest;
import slib.com.example.dto.hce.HceStationResponse;
import slib.com.example.dto.hce.HceStationStatusRequest;
import slib.com.example.service.hce.HceStationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slib/hce/stations")
public class HceStationController {

    @Autowired
    private HceStationService hceStationService;

    @Value("${gate.secret}")
    private String gateSecretKey;

    private Map<String, Object> errorBody(String status, Exception e, String fallbackMessage) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("message", e.getMessage() != null ? e.getMessage() : fallbackMessage);
        return body;
    }

    /**
     * Lấy danh sách tất cả trạm quét HCE
     * GET /slib/hce/stations?search=&status=&deviceType=
     */
    @GetMapping
    public ResponseEntity<?> getAllStations(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deviceType) {
        try {
            List<HceStationResponse> stations = hceStationService.getAllStations(search, status, deviceType);
            return ResponseEntity.ok(stations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorBody("ERROR", e, "Không thể tải danh sách trạm quét"));
        }
    }

    /**
     * Lấy chi tiết một trạm quét theo ID
     * GET /slib/hce/stations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getStationById(@PathVariable Integer id) {
        try {
            HceStationResponse station = hceStationService.getStationById(id);
            return ResponseEntity.ok(station);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(errorBody("NOT_FOUND", e, "Không tìm thấy trạm quét"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorBody("ERROR", e, "Không thể lấy chi tiết trạm quét"));
        }
    }

    /**
     * Tạo trạm quét mới
     * POST /slib/hce/stations
     */
    @PostMapping
    public ResponseEntity<?> createStation(@RequestBody HceStationRequest request) {
        try {
            HceStationResponse station = hceStationService.createStation(request);
            return ResponseEntity.status(201).body(station);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorBody("ERROR", e, "Không thể tạo trạm quét"));
        }
    }

    /**
     * Cập nhật trạm quét
     * PUT /slib/hce/stations/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStation(
            @PathVariable Integer id,
            @RequestBody HceStationRequest request) {
        try {
            HceStationResponse station = hceStationService.updateStation(id, request);
            return ResponseEntity.ok(station);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorBody("ERROR", e, "Không thể cập nhật trạm quét"));
        }
    }

    /**
     * Cập nhật trạng thái trạm quét
     * PATCH /slib/hce/stations/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStationStatus(
            @PathVariable Integer id,
            @RequestBody HceStationStatusRequest request) {
        try {
            HceStationResponse station = hceStationService.updateStationStatus(id, request);
            return ResponseEntity.ok(station);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorBody("ERROR", e, "Không thể cập nhật trạng thái trạm quét"));
        }
    }

    /**
     * Xóa trạm quét
     * DELETE /slib/hce/stations/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStation(@PathVariable Integer id) {
        try {
            hceStationService.deleteStation(id);
            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "message", "Đã xóa trạm quét thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "NOT_FOUND",
                    "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()));
        }
    }

    /**
     * Nhịp kết nối từ Raspberry Pi
     * POST /slib/hce/stations/{deviceId}/heartbeat
     * Dùng X-API-KEY giống flow check-in
     */
    @PostMapping("/{deviceId}/heartbeat")
    public ResponseEntity<?> heartbeat(
            @PathVariable String deviceId,
            HttpServletRequest httpRequest) {
        try {
            String requestKey = httpRequest.getHeader("X-API-KEY");

            if (requestKey == null || !requestKey.equals(gateSecretKey)) {
                return ResponseEntity.status(403).body(Map.of(
                        "status", "FORBIDDEN",
                        "message", "Truy cập bị từ chối: Sai API Key bảo mật"));
            }

            hceStationService.processHeartbeat(deviceId);
            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "message", "Đã ghi nhận nhịp kết nối của trạm quét",
                    "deviceId", deviceId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }
}
