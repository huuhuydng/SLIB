package slib.com.example.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.AmenityResponse;
import slib.com.example.service.AmenityService;

import java.util.List;

@RestController
@RequestMapping("/slib/zone_amenities") 
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AmenityController {

    private final AmenityService amenityService;

    @GetMapping
    public ResponseEntity<List<AmenityResponse>> getAmenities(
            @RequestParam(required = false) Integer zoneId
    ) {
        if (zoneId != null) {
            return ResponseEntity.ok(amenityService.getAmenitiesByZoneId(zoneId));
        }
        return ResponseEntity.ok(amenityService.getAllAmenities());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AmenityResponse> getAmenityById(@PathVariable Integer id) {
        return ResponseEntity.ok(amenityService.getAmenityById(id));
    }

    // create mới
    @PostMapping
    public ResponseEntity<AmenityResponse> createAmenity(@RequestBody AmenityResponse request) {
        return ResponseEntity.ok(amenityService.createAmenity(request));
    }

    // update
    @PutMapping("/{id}")
    public ResponseEntity<AmenityResponse> updateAmenity(
            @PathVariable Integer id,
            @RequestBody AmenityResponse request
    ) {
        return ResponseEntity.ok(amenityService.updateAmenity(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAmenity(@PathVariable Integer id) {
        amenityService.deleteAmenity(id);
        return ResponseEntity.ok("Deleted amenity with id = " + id);
    }
}