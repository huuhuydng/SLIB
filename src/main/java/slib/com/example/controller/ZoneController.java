package slib.com.example.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.ZoneResponse;
import slib.com.example.service.ZoneService;

import java.util.List;


@RestController
@RequestMapping("/slib/zones")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;

    @GetMapping
    public ResponseEntity<List<ZoneResponse>> getZones(
            @RequestParam(required = false) Long areaId
    ) {
        if (areaId != null) {
            return ResponseEntity.ok(zoneService.getZonesByAreaId(areaId));
        }
        return ResponseEntity.ok(zoneService.getAllZones());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ZoneResponse> getZoneById(@PathVariable Integer id) {
        return ResponseEntity.ok(zoneService.getZoneById(id));
    }


    // create mới
    @PostMapping
    public ResponseEntity<ZoneResponse> createZone(@RequestBody ZoneResponse request) {
        return ResponseEntity.ok(zoneService.createZone(request));
    }


    // update toàn bộ thông tin
    @PutMapping("/{id}")
    public ResponseEntity<ZoneResponse> updateZone(
            @PathVariable Integer id,
            @RequestBody ZoneResponse request
    ) {
        return ResponseEntity.ok(zoneService.updateZoneFull(id, request));
    }


    // update vị trí kéo thả
    @PutMapping("/{id}/position")
    public ResponseEntity<ZoneResponse> updateZonePosition(
            @PathVariable Integer id,
            @RequestBody ZoneResponse request
    ) {
        return ResponseEntity.ok(zoneService.updateZonePosition(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteZone(@PathVariable Integer id) {
        zoneService.deleteZone(id);
        return ResponseEntity.ok("Deleted zone with id = " + id);
    }

    // update chiều dài và chiều rộng (resize only)
    @PutMapping("/{id}/dimensions")
    public ResponseEntity<ZoneResponse> updateZoneDimensions(
            @PathVariable Integer id,
            @RequestBody ZoneResponse request
    ) {
        return ResponseEntity.ok(zoneService.updateZoneDimensions(id, request));
    }

    // update cả vị trí và kích thước (resize + move)
    @PutMapping("/{id}/position-and-dimensions")
    public ResponseEntity<ZoneResponse> updateZonePositionAndDimensions(
            @PathVariable Integer id,
            @RequestBody ZoneResponse request
    ) {
        return ResponseEntity.ok(zoneService.updateZonePositionAndDimensions(id, request));
    }


}
