package slib.com.example.controller.zone_config;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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
    public ResponseEntity<List<SeatResponse>> getSeats(
            @RequestParam(required = false) Integer zoneId) {
        if (zoneId != null) {
            return ResponseEntity.ok(seatService.getSeatsByZoneId(zoneId));
        }
        return ResponseEntity.ok(seatService.getAllSeats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatResponse> getSeatById(@PathVariable Integer id) {
        return ResponseEntity.ok(seatService.getSeatById(id));
    }

    // Update seat (seatCode, seatStatus, rowNumber, columnNumber)
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
    public List<SeatDTO> getSeatsByDate(@PathVariable Integer zoneId,
            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        return bookingService.getSeatsByDate(zoneId, localDate);
    }

    /**
     * Lấy tất cả seats của 1 area theo time slot - tối ưu performance
     * Trả về Map<zoneId, List<SeatDTO>> để mobile chỉ cần gọi 1 API
     */
    @GetMapping("/area/{areaId}/all-seats")
    public ResponseEntity<java.util.Map<Integer, List<SeatDTO>>> getAllSeatsByArea(
            @PathVariable Integer areaId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime end) {

        java.util.Map<Integer, List<SeatDTO>> result = bookingService.getAllSeatsByArea(areaId, date, start, end);
        return ResponseEntity.ok(result);
    }
}
