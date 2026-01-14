package slib.com.example.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.SeatResponse;
import slib.com.example.service.SeatService;

import java.util.List;

@RestController
@RequestMapping("/slib/seats")
@CrossOrigin(origins = "http://localhost:5173") 
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping
    public ResponseEntity<List<SeatResponse>> getSeats(
            @RequestParam(required = false) Integer zoneId
    ) {
        if (zoneId != null) {
            return ResponseEntity.ok(seatService.getSeatsByZoneId(zoneId));
        }
        return ResponseEntity.ok(seatService.getAllSeats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatResponse> getSeatById(@PathVariable Integer id) {
        return ResponseEntity.ok(seatService.getSeatById(id));
    }

    // create mới
    @PostMapping
    public ResponseEntity<SeatResponse> createSeat(@RequestBody SeatResponse request) {
        return ResponseEntity.ok(seatService.createSeat(request));
    }

    // update toàn bộ thông tin
    @PutMapping("/{id}")
    public ResponseEntity<SeatResponse> updateSeat(
            @PathVariable Integer id,
            @RequestBody SeatResponse request
    ) {
        return ResponseEntity.ok(seatService.updateSeatFull(id, request));
    }

    // update vị trí kéo thả
    @PutMapping("/{id}/position")
    public ResponseEntity<SeatResponse> updateSeatPosition(
            @PathVariable Integer id,
            @RequestBody SeatResponse request
    ) {
        return ResponseEntity.ok(seatService.updateSeatPosition(id, request));
    }

    // update chiều dài và chiều rộng (resize only)
    @PutMapping("/{id}/dimensions")
    public ResponseEntity<SeatResponse> updateSeatDimensions(
            @PathVariable Integer id,
            @RequestBody SeatResponse request
    ) {
        return ResponseEntity.ok(seatService.updateSeatDimensions(id, request));
    }

    // update cả vị trí và kích thước (resize + move)
    @PutMapping("/{id}/position-and-dimensions")
    public ResponseEntity<SeatResponse> updateSeatPositionAndDimensions(
            @PathVariable Integer id,
            @RequestBody SeatResponse request
    ) {
        return ResponseEntity.ok(seatService.updateSeatPositionAndDimensions(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSeat(@PathVariable Integer id) {
        seatService.deleteSeat(id);
        return ResponseEntity.ok("Deleted seat with id = " + id);
    }
}