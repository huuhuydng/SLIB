package slib.com.example.controller;

<<<<<<< HEAD
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
=======
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import slib.com.example.dto.SeatDTO;
import slib.com.example.service.BookingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/slib/seats")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SeatController {
    @Autowired
    private final BookingService bookingService;

    public SeatController(BookingService bookingService) {
        this.bookingService = bookingService;
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

}
>>>>>>> 9e7981680528c51139544e478f7f9919199c239c
