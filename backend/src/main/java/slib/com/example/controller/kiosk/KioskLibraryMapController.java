package slib.com.example.controller.kiosk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.kiosk.KioskLibraryMapDTO;
import slib.com.example.dto.kiosk.KioskZoneMapDTO;
import slib.com.example.service.kiosk.KioskLibraryMapService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for Library Map Management
 * API endpoints: /api/library-map
 */
@RestController
@RequestMapping("/api/library-map")
@RequiredArgsConstructor
@Slf4j
public class KioskLibraryMapController {

    private final KioskLibraryMapService libraryMapService;

    /**
     * GET /api/library-map/active
     * Get the active/current library map (accessible to all - for kiosk display)
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveMap() {
        log.info("📍 GET /api/library-map/active");

        Optional<KioskLibraryMapDTO> map = libraryMapService.getActiveMap();
        if (map.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No active library map found"));
        }

        return ResponseEntity.ok(map.get());
    }

    /**
     * GET /api/library-map/{id}
     * Get library map by ID (admin only)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getMapById(@PathVariable Integer id) {
        log.info("📍 GET /api/library-map/{}", id);

        Optional<KioskLibraryMapDTO> map = libraryMapService.getMapById(id);
        if (map.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Library map not found with ID: " + id));
        }

        return ResponseEntity.ok(map.get());
    }

    /**
     * GET /api/library-map
     * Get all library maps (admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllMaps() {
        log.info("📍 GET /api/library-map");

        List<KioskLibraryMapDTO> maps = libraryMapService.getAllMaps();
        return ResponseEntity.ok(maps);
    }

    /**
     * POST /api/library-map
     * Create a new library map (admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createMap(@RequestBody KioskLibraryMapDTO dto) {
        log.info("📍 POST /api/library-map - Creating: {}", dto.getMapName());

        try {
            KioskLibraryMapDTO created = libraryMapService.createMap(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("❌ Error creating library map:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error creating library map: " + e.getMessage()));
        }
    }

    /**
     * PATCH /api/library-map/{id}
     * Update library map (admin only)
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateMap(@PathVariable Integer id, @RequestBody KioskLibraryMapDTO dto) {
        log.info("📍 PATCH /api/library-map/{} - Updating", id);

        try {
            KioskLibraryMapDTO updated = libraryMapService.updateMap(id, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("❌ Error updating library map:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error updating library map: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/library-map/{id}
     * Delete library map (admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteMap(@PathVariable Integer id) {
        log.info("📍 DELETE /api/library-map/{}", id);

        try {
            libraryMapService.deleteMap(id);
            return ResponseEntity.ok(Map.of("message", "Library map deleted successfully"));
        } catch (Exception e) {
            log.error("❌ Error deleting library map:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error deleting library map: " + e.getMessage()));
        }
    }

    // ==================== Zone Management ====================

    /**
     * POST /api/library-map/{mapId}/zones
     * Add a zone to library map (admin only)
     */
    @PostMapping("/{mapId}/zones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addZone(@PathVariable Integer mapId, @RequestBody KioskZoneMapDTO dto) {
        log.info("📍 POST /api/library-map/{}/zones - Adding: {}", mapId, dto.getZoneName());

        try {
            KioskZoneMapDTO created = libraryMapService.addZoneToMap(mapId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("❌ Error adding zone:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error adding zone: " + e.getMessage()));
        }
    }

    /**
     * PATCH /api/library-map/zones/{zoneId}
     * Update a zone (admin only)
     */
    @PatchMapping("/zones/{zoneId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateZone(@PathVariable Integer zoneId, @RequestBody KioskZoneMapDTO dto) {
        log.info("📍 PATCH /api/library-map/zones/{} - Updating", zoneId);

        try {
            KioskZoneMapDTO updated = libraryMapService.updateZone(zoneId, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("❌ Error updating zone:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error updating zone: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/library-map/zones/{zoneId}
     * Delete a zone (admin only)
     */
    @DeleteMapping("/zones/{zoneId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteZone(@PathVariable Integer zoneId) {
        log.info("📍 DELETE /api/library-map/zones/{}", zoneId);

        try {
            libraryMapService.deleteZone(zoneId);
            return ResponseEntity.ok(Map.of("message", "Zone deleted successfully"));
        } catch (Exception e) {
            log.error("❌ Error deleting zone:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error deleting zone: " + e.getMessage()));
        }
    }
}
