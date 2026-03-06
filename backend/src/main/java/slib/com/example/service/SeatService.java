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
    private final SeatAvailabilityService seatAvailabilityService;
    private final slib.com.example.util.NfcUidHasher nfcUidHasher;

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
    // FE gui: zoneId, rowNumber, columnNumber, seatCode, seatStatus
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
        res.setRowNumber(seat.getRowNumber());
        res.setColumnNumber(seat.getColumnNumber());
        res.setIsActive(seat.getIsActive());
        res.setNfcTagUid(seat.getNfcTagUid());

        // Calculate status dynamically for current moment
        SeatStatus status = seatAvailabilityService.calculateCurrentStatus(seat);
        res.setSeatStatus(status);

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

                    // Ưu tiên 1: Ghế bị hạn chế (is_active=false) → UNAVAILABLE
                    if (seat.getIsActive() == null || !seat.getIsActive()) {
                        response.setSeatStatus(SeatStatus.UNAVAILABLE);
                        return response;
                    }

                    // Ưu tiên 2: Tính status động dựa trên reservations overlap
                    var matchingReservation = seat.getReservation().stream()
                            .filter(r -> {
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
                            })
                            .findFirst();

                    if (matchingReservation.isPresent()) {
                        var reservation = matchingReservation.get();
                        response.setSeatStatus(SeatStatus.BOOKED);
                        response.setReservationEndTime(reservation.getEndTime().toString());
                        response.setReservationStartTime(reservation.getStartTime().toString());

                        // Set booker info
                        var user = reservation.getUser();
                        if (user != null) {
                            response.setBookedByUserName(user.getFullName());
                            response.setBookedByUserCode(user.getUserCode());
                            response.setBookedByAvatarUrl(user.getAvtUrl());
                        }
                    } else {
                        response.setSeatStatus(SeatStatus.AVAILABLE);
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Han che ghe theo seatId (unique ID)
     */
    public SeatResponse restrictSeatById(Integer seatId) {
        SeatEntity seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found with id: " + seatId));

        // Set isActive = false so calculateStatus returns UNAVAILABLE
        seat.setIsActive(false);
        seat.setSeatStatus(SeatStatus.UNAVAILABLE);
        return toResponse(seatRepository.save(seat));
    }

    /**
     * Bo han che ghe theo seatId
     */
    public void unrestrictSeatById(Integer seatId) {
        SeatEntity seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found with id: " + seatId));

        // Set isActive = true so calculateStatus returns AVAILABLE
        seat.setIsActive(true);
        seat.setSeatStatus(SeatStatus.AVAILABLE);
        seatRepository.save(seat);
    }

    /**
     * @deprecated Use restrictSeatById instead (seatCode is not unique across
     *             zones)
     */
    @Deprecated
    public SeatResponse restrictSeat(String seatCode) {
        SeatEntity seat = seatRepository.findBySeatCode(seatCode)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatCode));

        seat.setSeatStatus(SeatStatus.UNAVAILABLE);
        return toResponse(seatRepository.save(seat));
    }

    /**
     * @deprecated Use unrestrictSeatById instead (seatCode is not unique across
     *             zones)
     */
    @Deprecated
    public void unrestrictSeat(String seatCode) {
        SeatEntity seat = seatRepository.findBySeatCode(seatCode)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatCode));

        seat.setSeatStatus(SeatStatus.AVAILABLE);
        seatRepository.save(seat);
    }

    // ================= NFC UID MAPPING =================

    /**
     * Update the NFC tag UID for a seat (UID Mapping Strategy)
     * 
     * @param seatId    The seat to update
     * @param nfcTagUid The NFC tag UID in uppercase HEX format (e.g., "04A23C91")
     * @return Updated seat response
     */
    public SeatResponse updateNfcTagUid(Integer seatId, String nfcTagUid) {
        SeatEntity seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found with id: " + seatId));

        // Hash the UID before storing for security
        String hashedUid = nfcUidHasher.hashUid(nfcTagUid);

        // Check if this UID is already assigned to another seat
        if (hashedUid != null && !hashedUid.isEmpty()) {
            seatRepository.findByNfcTagUid(hashedUid).ifPresent(existingSeat -> {
                if (!existingSeat.getSeatId().equals(seatId)) {
                    throw new RuntimeException(
                            "NFC UID này đã được gán cho ghế " +
                                    existingSeat.getSeatCode() + " (ID: " + existingSeat.getSeatId() + "). " +
                                    "Vui lòng xóa NFC UID khỏi ghế đó trước khi gán cho ghế này.");
                }
            });
        }

        seat.setNfcTagUid(hashedUid);
        return toResponse(seatRepository.save(seat));
    }

    /**
     * Clear the NFC tag UID from a seat
     */
    public SeatResponse clearNfcTagUid(Integer seatId) {
        return updateNfcTagUid(seatId, null);
    }

    /**
     * Find seat by NFC tag UID (supports both raw and hashed UIDs)
     * 
     * - If input is 64 chars hex (SHA-256 hash) from Mobile: use directly
     * - If input is raw UID (e.g., "04A5108A405980") from Admin: hash first
     */
    public SeatResponse getSeatByNfcTagUid(String nfcTagUid) {
        String hashedUid;

        // Check if input is already a SHA-256 hash (64 hex characters)
        if (nfcTagUid != null && nfcTagUid.length() == 64 && nfcTagUid.matches("[A-Fa-f0-9]+")) {
            // Already hashed (from Mobile app)
            hashedUid = nfcTagUid.toUpperCase();
        } else {
            // Raw UID (from Admin NFC Bridge) - need to hash
            hashedUid = nfcUidHasher.hashUid(nfcTagUid);
        }

        SeatEntity seat = seatRepository.findByNfcTagUid(hashedUid)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy ghế với NFC UID này. Có thể thẻ chưa được gán cho ghế nào."));
        return toResponse(seat);
    }
}
