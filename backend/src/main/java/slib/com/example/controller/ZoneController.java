package slib.com.example.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import slib.com.example.entity.ZoneEntity;
import slib.com.example.service.BookingService;

@RestController
@RequestMapping("/slib/zones")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ZoneController {
    private final BookingService bookingService;

    public ZoneController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/getAllZones")
    public List<ZoneEntity> getAllZones() {
        return bookingService.getAllZones();
    }
}
