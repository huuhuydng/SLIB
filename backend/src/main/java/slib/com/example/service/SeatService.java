package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import slib.com.example.dto.zone_config.SeatResponse;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.SeatStatus;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.repository.SeatRepository;
import slib.com.example.repository.ZoneRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final ZoneRepository zoneRepository;

    // ================= GET =================

    public List<SeatResponse> getSeatsByZoneId(Integer zoneId) {
        return seatRepository.findByZone_ZoneId(zoneId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<SeatResponse> getAllSeats() {
        return seatRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public SeatResponse getSeatById(Integer id) {
        SeatEntity seat = seatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seat not found"));
        return toResponse(seat);
    }

    // ================= CREATE =================
    // FE gửi: zoneId, rowNumber, columnNumber, seatCode, seatStatus
    public SeatResponse createSeat(SeatResponse req) {
        ZoneEntity zone = zoneRepository.findById(req.getZoneId())
                .orElseThrow(() -> new RuntimeException("Zone not found"));

        int rowNumber = req.getRowNumber();
        int columnNumber = req.getColumnNumber() != null ? req.getColumnNumber() : 1;

        // Generate seatCode if not provided
        String seatCode = req.getSeatCode();
        if (seatCode == null || seatCode.isEmpty()) {
            seatCode = generateSeatCode(rowNumber, columnNumber);
        }

        SeatEntity seat = SeatEntity.builder()
                .zone(zone)
                .rowNumber(rowNumber)
                .columnNumber(columnNumber)
                .seatCode(seatCode)
                .seatStatus(req.getSeatStatus() != null ? req.getSeatStatus() : SeatStatus.AVAILABLE)
                .build();

        return toResponse(seatRepository.save(seat));
    }

    // ================= UPDATE =================
    public SeatResponse updateSeat(Integer id, SeatResponse req) {
        SeatEntity seat = seatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        if (req.getSeatCode() != null) {
            seat.setSeatCode(req.getSeatCode());
        }
        if (req.getSeatStatus() != null) {
            seat.setSeatStatus(req.getSeatStatus());
        }
        if (req.getColumnNumber() != null) {
            seat.setColumnNumber(req.getColumnNumber());
        }
        if (req.getRowNumber() != null) {
            seat.setRowNumber(req.getRowNumber());
        }

        return toResponse(seatRepository.save(seat));
    }

    // ================= DELETE =================
    public void deleteSeat(Integer id) {
        if (!seatRepository.existsById(id)) {
            throw new RuntimeException("Seat not found");
        }
        seatRepository.deleteById(id);
    }

    // ================= UTIL =================
    private String generateSeatCode(int row, int column) {
        char rowChar = (char) ('A' + row - 1);
        return rowChar + String.valueOf(column);
    }

    // ================= MAP ENTITY -> DTO =================
    private SeatResponse toResponse(SeatEntity seat) {
        SeatResponse res = new SeatResponse();
        res.setSeatId(seat.getSeatId());
        res.setZoneId(seat.getZone().getZoneId());
        res.setSeatCode(seat.getSeatCode());
        res.setSeatStatus(seat.getSeatStatus());
        res.setRowNumber(seat.getRowNumber());
        res.setColumnNumber(seat.getColumnNumber());
        return res;
    }
}
