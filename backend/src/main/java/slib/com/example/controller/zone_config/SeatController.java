package slib.com.example.controller.zone_config;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import slib.com.example.dto.zone_config.SeatDTO;
import slib.com.example.dto.zone_config.SeatResponse;
import slib.com.example.service.BookingService;
import slib.com.example.service.SeatService;

@RestController
@RequestMapping("/slib/seats")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SeatController {

    @Autowired
    private final BookingService bookingService;
    private final SeatService seatService;

    public SeatController(BookingService bookingService, SeatService seatService) {
        this.bookingService = bookingService;
        this.seatService = seatService;
    }

    // Create new seat
    @PostMapping
    public ResponseEntity<SeatResponse> createSeat(@RequestBody SeatResponse request) {
        return ResponseEntity.ok(seatService.createSeat(request));
    }

    @GetMapping
    public ResponseEntity<?> getSeats(
            @RequestParam(required = false) Integer zoneId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            if (startTime != null && endTime != null) {
                return ResponseEntity.ok(seatService.getSeatsByTimeRange(startTime, endTime, zoneId));
            }

            if (zoneId != null) {
                return ResponseEntity.ok(seatService.getSeatsByZoneId(zoneId));
            }

            return ResponseEntity.ok(seatService.getAllSeats());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "error", "Invalid request",
                    "message", ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatResponse> getSeatById(@PathVariable Integer id) {
        return ResponseEntity.ok(seatService.getSeatById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeatResponse> updateSeat(
            @PathVariable Integer id,
            @RequestBody SeatResponse request) {
        return ResponseEntity.ok(seatService.updateSeat(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSeat(@PathVariable Integer id) {
        seatService.deleteSeat(id);
        return ResponseEntity.ok("Deleted seat with id = " + id);
    }

    @GetMapping("/getAvailableSeat/{zoneId}")
    public ResponseEntity<Long> getAvailableSeats(@PathVariable Integer zoneId) {
        long count = bookingService.countAvailableSeats(zoneId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/getAllSeat/{zoneId}")
    public ResponseEntity<List<SeatDTO>> getAllSeats(@PathVariable Integer zoneId) {
        List<SeatDTO> seats = bookingService.getAllSeatsDTO(zoneId);
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/getSeatsByTime/{zoneId}")
    public ResponseEntity<List<SeatDTO>> getSeatsByTime(
            @PathVariable Integer zoneId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime end) {

        List<SeatDTO> seats = bookingService.getSeatsByTime(zoneId, date, start, end);
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/getSeatsByDate/{zoneId}")
    public List<SeatDTO> getSeatsByDate(
            @PathVariable Integer zoneId,
            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        return bookingService.getSeatsByDate(zoneId, localDate);
    }

    @PostMapping("/{seatId}/restrict")
    public ResponseEntity<?> restrictSeat(@PathVariable Integer seatId) {
        try {
            SeatResponse response = seatService.restrictSeatById(seatId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{seatId}/restrict")
    public ResponseEntity<?> unrestrictSeat(@PathVariable Integer seatId) {
        try {
            seatService.unrestrictSeatById(seatId);
            return ResponseEntity.ok(java.util.Map.of("message", "Đã bỏ hạn chế ghế với id: " + seatId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/area/{areaId}/all-seats")
    public ResponseEntity<java.util.Map<Integer, List<SeatDTO>>> getAllSeatsByArea(
            @PathVariable Integer areaId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime end) {

        java.util.Map<Integer, List<SeatDTO>> result =
                bookingService.getAllSeatsByArea(areaId, date, start, end);
        return ResponseEntity.ok(result);
    }
}
