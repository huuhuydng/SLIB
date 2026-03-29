package slib.com.example.controller.zone_config;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import slib.com.example.dto.zone_config.ZoneOccupancyDTO;
import slib.com.example.dto.zone_config.ZoneResponse;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.service.booking.BookingService;
import slib.com.example.service.zone_config.ZoneService;

@RestController
@RequestMapping("/slib/zones")
public class ZoneController {
    private final BookingService bookingService;
    private final ZoneService zoneService;

    public ZoneController(BookingService bookingService, ZoneService zoneService) {
        this.bookingService = bookingService;
        this.zoneService = zoneService;
    }

    @GetMapping("/getAllZones")
    public List<ZoneEntity> getAllZones() {
        return bookingService.getAllZones();
    }

    @GetMapping
    public ResponseEntity<List<ZoneResponse>> getZones(
            @RequestParam(required = false) Long areaId) {
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
            @RequestBody ZoneResponse request) {
        return ResponseEntity.ok(zoneService.updateZoneFull(id, request));
    }

    // update vị trí kéo thả
    @PutMapping("/{id}/position")
    public ResponseEntity<ZoneResponse> updateZonePosition(
            @PathVariable Integer id,
            @RequestBody ZoneResponse request) {
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
            @RequestBody ZoneResponse request) {
        return ResponseEntity.ok(zoneService.updateZoneDimensions(id, request));
    }

    // update cả vị trí và kích thước (resize + move)
    @PutMapping("/{id}/position-and-dimensions")
    public ResponseEntity<ZoneResponse> updateZonePositionAndDimensions(
            @PathVariable Integer id,
            @RequestBody ZoneResponse request) {
        return ResponseEntity.ok(zoneService.updateZonePositionAndDimensions(id, request));
    }

    /**
     * Get zone occupancy for mobile app
     * Returns zones with their occupancy rate for density coloring
     */
    @GetMapping("/occupancy/{areaId}")
    public ResponseEntity<List<ZoneOccupancyDTO>> getZoneOccupancy(@PathVariable Long areaId) {
        return ResponseEntity.ok(zoneService.getZoneOccupancy(areaId));
    }
}
