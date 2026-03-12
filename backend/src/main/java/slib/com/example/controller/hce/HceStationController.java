package slib.com.example.controller.hce;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import slib.com.example.dto.hce.HceStationRequest;
import slib.com.example.dto.hce.HceStationResponse;
import slib.com.example.dto.hce.HceStationStatusRequest;
import slib.com.example.service.HceStationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slib/hce/stations")
@CrossOrigin(origins = "*")
public class HceStationController {

    @Autowired
    private HceStationService hceStationService;

    @Value("${gate.secret}")
    private String gateSecretKey;

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
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()));
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
     * Tạo trạm quét mới
     * POST /slib/hce/stations
     */
    @PostMapping
    public ResponseEntity<?> createStation(@RequestBody HceStationRequest request) {
        try {
            HceStationResponse station = hceStationService.createStation(request);
            return ResponseEntity.status(201).body(station);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()));
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
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()));
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
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()));
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
     * Heartbeat từ Raspberry Pi
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
                    "message", "Heartbeat received",
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
