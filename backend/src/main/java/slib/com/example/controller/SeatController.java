package slib.com.example.controller;

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
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime end) {

        // Use defaults if not provided
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        LocalTime queryStart = (start != null) ? start : LocalTime.of(0, 0);
        LocalTime queryEnd = (end != null) ? end : LocalTime.of(23, 59);

        List<SeatDTO> seats = bookingService.getSeatsByTime(zoneId, queryDate, queryStart, queryEnd);
        return ResponseEntity.ok(seats);
    }

    @GetMapping("/getSeatsByDate/{zoneId}")
    public List<SeatDTO> getSeatsByDate(
            @PathVariable Integer zoneId,
            @RequestParam(required = false) String date) {
        // If no date provided, use today
        LocalDate localDate = (date != null && !date.isEmpty()) 
            ? LocalDate.parse(date) 
            : LocalDate.now();
        return bookingService.getSeatsByDate(zoneId, localDate);
    }

}
