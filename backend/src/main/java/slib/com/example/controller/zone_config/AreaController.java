package slib.com.example.controller.zone_config;

import slib.com.example.dto.zone_config.AreaResponse;
import slib.com.example.service.AreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/slib/areas")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AreaController {

        private final AreaService areaService;

        @GetMapping
        public ResponseEntity<List<AreaResponse>> getAllAreas() {
                return ResponseEntity.ok(areaService.getAllAreas());
        }

        @GetMapping("/{id}")
        public ResponseEntity<AreaResponse> getAreaById(@PathVariable Long id) {
                return ResponseEntity.ok(areaService.getAreaById(id));
        }

        // update vị trí kéo thả
        @PutMapping("/{id}/position")
        public ResponseEntity<AreaResponse> updateAreaPosition(
                        @PathVariable Long id,
                        @RequestBody AreaResponse request) {
                return ResponseEntity.ok(areaService.updateAreaPosition(id, request));
        }

        // update chiều dài và chiều rộng (resize only)
        @PutMapping("/{id}/dimensions")
        public ResponseEntity<AreaResponse> updateAreaDimensions(
                        @PathVariable Long id,
                        @RequestBody AreaResponse request) {
                return ResponseEntity.ok(
                                areaService.updateAreaDimensions(id, request));
        }

        // update cả vị trí và kích thước (resize + move)
        @PutMapping("/{id}/position-and-dimensions")
        public ResponseEntity<AreaResponse> updateAreaPositionAndDimensions(
                        @PathVariable Long id,
                        @RequestBody AreaResponse request) {
                return ResponseEntity.ok(
                                areaService.updateAreaPositionAndDimensions(id, request));
        }

        // Tạo Area mới
        @PostMapping
        public ResponseEntity<AreaResponse> createArea(
                        @Valid @RequestBody AreaResponse request) {
                return ResponseEntity.status(HttpStatus.CREATED).body(
                                areaService.createArea(request));
        }

        // Cập nhật toàn bộ Area
        @PutMapping("/{id}")
        public ResponseEntity<AreaResponse> updateArea(
                        @PathVariable Long id,
                        @Valid @RequestBody AreaResponse request // Thêm @Valid ở đây
        ) {
                return ResponseEntity.ok(
                                areaService.updateAreaFull(id, request));
        }

        // Cập nhật lock status
        @PutMapping("/{id}/locked")
        public ResponseEntity<AreaResponse> updateAreaLocked(
                        @PathVariable Long id,
                        @RequestBody AreaResponse request) {
                return ResponseEntity.ok(
                                areaService.updateAreaLocked(id, request));
        }

        // Cập nhật active status
        @PutMapping("/{id}/active")
        public ResponseEntity<AreaResponse> updateAreaIsActive(
                        @PathVariable Long id,
                        @RequestBody AreaResponse request) {
                return ResponseEntity.ok(
                                areaService.updateAreaIsActive(id, request));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteArea(@PathVariable Long id) {
                areaService.deleteArea(id);
                return ResponseEntity.noContent().build();
        }
}
