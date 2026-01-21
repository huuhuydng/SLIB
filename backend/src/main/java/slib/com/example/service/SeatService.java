package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import slib.com.example.dto.zone_config.SeatResponse;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.SeatStatus;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.repository.SeatRepository;
import slib.com.example.repository.ZoneRepository;

import java.time.LocalDateTime;
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

    public List<SeatResponse> getSeatsByTimeRange(String startTimeStr, String endTimeStr, Integer zoneId) {
        // Parse ISO 8601 time strings (e.g., "2026-01-20T13:00:00")
        LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
        LocalDateTime endTime = LocalDateTime.parse(endTimeStr);

        List<SeatEntity> seats;
        if (zoneId != null) {
            seats = seatRepository.findByZone_ZoneId(zoneId);
        } else {
            seats = seatRepository.findAll();
        }

        return seats.stream()
                .map(seat -> {
                    SeatResponse response = toResponse(seat);

                    // Tính toán status động dựa trên reservations trong time range
                    boolean isBookedInTimeRange = seat.getReservation().stream()
                            .anyMatch(r -> {
                                String status = r.getStatus();
                                boolean isActiveStatus = "BOOKED".equalsIgnoreCase(status)
                                        || "PROCESSING".equalsIgnoreCase(status)
                                        || "CONFIRMED".equalsIgnoreCase(status);

                                if (!isActiveStatus) {
                                    return false;
                                }

                                // Kiểm tra overlap: reservation có giao với time range không
                                LocalDateTime resStart = r.getStartTime();
                                LocalDateTime resEnd = r.getEndTime();

                                return resStart.isBefore(endTime) && resEnd.isAfter(startTime);
                            });

                    // Nếu seat có status UNAVAILABLE trong DB, ưu tiên status này
                    if (seat.getSeatStatus() == SeatStatus.UNAVAILABLE) {
                        response.setSeatStatus(SeatStatus.UNAVAILABLE);
                    } else if (isBookedInTimeRange) {
                        response.setSeatStatus(SeatStatus.BOOKED);
                    } else {
                        response.setSeatStatus(SeatStatus.AVAILABLE);
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    public SeatResponse restrictSeat(String seatCode) {
        SeatEntity seat = seatRepository.findBySeatCode(seatCode)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatCode));

        seat.setSeatStatus(SeatStatus.UNAVAILABLE);
        return toResponse(seatRepository.save(seat));
    }

    /**
     * Bỏ hạn chế ghế (set status back to AVAILABLE)
     */
    public void unrestrictSeat(String seatCode) {
        SeatEntity seat = seatRepository.findBySeatCode(seatCode)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatCode));

        seat.setSeatStatus(SeatStatus.AVAILABLE);
        seatRepository.save(seat);
    }
}
