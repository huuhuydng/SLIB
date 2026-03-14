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
import slib.com.example.service.booking.BookingService;
import slib.com.example.service.zone_config.SeatService;

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
            // Nếu truyền startTime + endTime → tính trạng thái động theo khoảng thời gian
            if (startTime != null && endTime != null) {
                return ResponseEntity.ok(seatService.getSeatsByTimeRange(startTime, endTime, zoneId));
            }

            // Nếu chỉ filter theo zone
            if (zoneId != null) {
                return ResponseEntity.ok(seatService.getSeatsByZoneId(zoneId));
            }

            // Mặc định: tất cả seats
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
            return ResponseEntity.ok(java.util.Map.of("message", "Da bo han che ghe voi id: " + seatId));
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

        java.util.Map<Integer, List<SeatDTO>> result = bookingService.getAllSeatsByArea(areaId, date, start, end);
        return ResponseEntity.ok(result);
    }

    // ==================== NFC UID MAPPING ENDPOINTS ====================

    /**
     * Update NFC tag UID for a seat (Admin - UID Mapping Strategy)
     * PUT /slib/seats/{seatId}/nfc-uid
     * Body: { "nfcTagUid": "04A23C91" }
     */
    @PutMapping("/{seatId}/nfc-uid")
    public ResponseEntity<?> updateSeatNfcUid(
            @PathVariable Integer seatId,
            @RequestBody(required = false) java.util.Map<String, String> body,
            @RequestParam(value = "nfcTagUid", required = false) String nfcTagUidParam) {
        try {
            String nfcTagUid = nfcTagUidParam;
            if ((nfcTagUid == null || nfcTagUid.isBlank()) && body != null) {
                nfcTagUid = body.get("nfcTagUid");
            }
            SeatResponse response = seatService.updateNfcTagUid(seatId, nfcTagUid);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Clear NFC tag UID from a seat
     * DELETE /slib/seats/{seatId}/nfc-uid
     */
    @DeleteMapping("/{seatId}/nfc-uid")
    public ResponseEntity<?> clearSeatNfcUid(@PathVariable Integer seatId) {
        try {
            SeatResponse response = seatService.clearNfcTagUid(seatId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Find seat by NFC tag UID (Mobile - for check-in via NFC)
     * GET /slib/seats/by-nfc-uid/{nfcTagUid}
     * Now always expects raw UID — backend hashes server-side.
     */
    @GetMapping("/by-nfc-uid/{nfcTagUid}")
    public ResponseEntity<?> getSeatByNfcUid(@PathVariable String nfcTagUid) {
        try {
            SeatResponse response = seatService.getSeatByNfcTagUid(nfcTagUid);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * List all seats with NFC mapping status (Admin - FE-48).
     * GET /slib/seats/nfc-mappings?zoneId=&areaId=&hasNfc=&search=
     */
    @GetMapping("/nfc-mappings")
    public ResponseEntity<?> getNfcMappings(
            @RequestParam(required = false) Integer zoneId,
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false) Boolean hasNfc,
            @RequestParam(required = false) String search) {
        try {
            var mappings = seatService.getNfcMappings(zoneId, areaId, hasNfc, search);
            return ResponseEntity.ok(mappings);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get NFC info for a specific seat (Admin - FE-49).
     * GET /slib/seats/{seatId}/nfc-info
     */
    @GetMapping("/{seatId}/nfc-info")
    public ResponseEntity<?> getNfcInfo(@PathVariable Integer seatId) {
        try {
            var info = seatService.getNfcInfo(seatId);
            return ResponseEntity.ok(info);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
