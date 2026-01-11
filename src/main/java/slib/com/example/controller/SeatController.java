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
