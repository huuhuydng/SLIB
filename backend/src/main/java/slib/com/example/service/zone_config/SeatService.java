package slib.com.example.service.zone_config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.dto.zone_config.SeatResponse;
import slib.com.example.entity.booking.ReservationEntity;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.SeatStatus;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.zone_config.SeatRepository;
import slib.com.example.repository.zone_config.ZoneRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import slib.com.example.service.notification.PushNotificationService;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {

    private final SeatRepository seatRepository;
    private final ZoneRepository zoneRepository;
    private final ReservationRepository reservationRepository;
    private final SeatAvailabilityService seatAvailabilityService;
    private final PushNotificationService pushNotificationService;
    private final SeatStatusSyncService seatStatusSyncService;
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
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
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
        if (req.getIsActive() != null) {
            seat.setIsActive(req.getIsActive());
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
                        if ("PROCESSING".equalsIgnoreCase(reservation.getStatus())) {
                            response.setSeatStatus(SeatStatus.HOLDING);
                        } else if ("CONFIRMED".equalsIgnoreCase(reservation.getStatus())) {
                            response.setSeatStatus(SeatStatus.CONFIRMED);
                        } else {
                            response.setSeatStatus(SeatStatus.BOOKED);
                        }
                        response.setReservationId(reservation.getReservationId().toString());
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
     * Han che ghe theo seatId (unique ID).
     * Huy tat ca dat cho dang hoat dong (BOOKED, CONFIRMED) va thong bao cho sinh vien.
     */
    @Transactional
    public SeatResponse restrictSeatById(Integer seatId) {
        SeatEntity seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found with id: " + seatId));

        String seatCode = seat.getSeatCode();
        String zoneName = seat.getZone() != null ? seat.getZone().getZoneName() : "";

        // Tim va huy tat ca dat cho dang hoat dong cho ghe nay
        List<ReservationEntity> activeReservations = reservationRepository.findBySeat_SeatId(seatId)
                .stream()
                .filter(r -> "BOOKED".equalsIgnoreCase(r.getStatus())
                        || "CONFIRMED".equalsIgnoreCase(r.getStatus()))
                .toList();

        for (ReservationEntity reservation : activeReservations) {
            reservation.setStatus("CANCEL");
            reservationRepository.save(reservation);

            // Gui thong bao cho sinh vien bi anh huong
            if (reservation.getUser() != null) {
                try {
                    pushNotificationService.sendToUser(
                            reservation.getUser().getId(),
                            "Ghế đã bị hạn chế",
                            "Ghế " + seatCode + " tại " + zoneName
                                    + " đã bị hạn chế bởi thủ thư do cần bảo trì. Đặt chỗ của bạn đã được hủy tự động.",
                            NotificationType.BOOKING,
                            reservation.getReservationId());
                } catch (Exception e) {
                    log.error("Khong the gui thong bao cho user {} khi han che ghe {}: {}",
                            reservation.getUser().getId(), seatId, e.getMessage());
                }
            }
        }

        // Set isActive = false so calculateStatus returns UNAVAILABLE
        seat.setIsActive(false);
        seat.setSeatStatus(SeatStatus.UNAVAILABLE);
        SeatEntity savedSeat = seatRepository.save(seat);

        // Broadcast thay doi trang thai ghe qua WebSocket
        seatStatusSyncService.broadcastSeatUpdate(savedSeat, "UNAVAILABLE");

        return toResponse(savedSeat);
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
        SeatEntity savedSeat = seatRepository.save(seat);

        // Broadcast thay doi trang thai ghe qua WebSocket
        seatStatusSyncService.broadcastSeatUpdate(savedSeat, "AVAILABLE");
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
     * Update the NFC tag UID for a seat (UID Mapping Strategy).
     * Accepts raw UID from admin bridge — normalizes, validates, hashes before
     * storing.
     */
    public SeatResponse updateNfcTagUid(Integer seatId, String rawNfcTagUid) {
        SeatEntity seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found with id: " + seatId));

        if (rawNfcTagUid == null || rawNfcTagUid.trim().isEmpty()) {
            // Clear mapping
            seat.setNfcTagUid(null);
            seat.setNfcTagUidUpdatedAt(LocalDateTime.now());
            return toResponse(seatRepository.save(seat));
        }

        // Normalize and validate
        String normalized = nfcUidHasher.normalizeUid(rawNfcTagUid);
        if (!nfcUidHasher.isValidUidFormat(normalized)) {
            throw new RuntimeException(
                    "NFC UID không hợp lệ. UID phải là chuỗi HEX (4, 7 hoặc 10 bytes). Nhận được: " + rawNfcTagUid);
        }

        // Hash before storing
        String hashedUid = nfcUidHasher.hashUid(rawNfcTagUid);

        // Check if this UID is already assigned to another seat
        seatRepository.findByNfcTagUid(hashedUid).ifPresent(existingSeat -> {
            if (!existingSeat.getSeatId().equals(seatId)) {
                throw new RuntimeException(
                        "NFC UID này đã được gán cho ghế " +
                                existingSeat.getSeatCode() + " (ID: " + existingSeat.getSeatId() + "). " +
                                "Vui lòng xóa NFC UID khỏi ghế đó trước khi gán cho ghế này.");
            }
        });

        seat.setNfcTagUid(hashedUid);
        seat.setNfcTagUidUpdatedAt(LocalDateTime.now());
        return toResponse(seatRepository.save(seat));
    }

    /**
     * Clear the NFC tag UID from a seat
     */
    public SeatResponse clearNfcTagUid(Integer seatId) {
        SeatEntity seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found with id: " + seatId));
        seat.setNfcTagUid(null);
        seat.setNfcTagUidUpdatedAt(LocalDateTime.now());
        return toResponse(seatRepository.save(seat));
    }

    /**
     * Find seat by raw NFC tag UID.
     * Always hashes the raw UID server-side before looking up.
     * Clients (mobile, admin) must send raw UIDs — backend handles hashing.
     */
    public SeatResponse getSeatByNfcTagUid(String rawNfcTagUid) {
        String hashedUid = nfcUidHasher.hashUid(rawNfcTagUid);

        SeatEntity seat = seatRepository.findByNfcTagUid(hashedUid)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy ghế với NFC UID này. Thẻ chưa được gán cho ghế nào."));
        return toResponse(seat);
    }

    /**
     * Find seat entity by raw NFC tag UID (internal use — returns entity, not DTO).
     */
    public SeatEntity findSeatEntityByNfcUid(String rawNfcTagUid) {
        String hashedUid = nfcUidHasher.hashUid(rawNfcTagUid);
        return seatRepository.findByNfcTagUid(hashedUid)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy ghế với NFC UID này. Thẻ chưa được gán cho ghế nào."));
    }

    /**
     * Get all NFC mappings (FE-48: Danh sách ghế đã gán thẻ NFC).
     * Supports filtering by zoneId, areaId, hasNfc, and search text.
     */
    public List<slib.com.example.dto.zone_config.NfcMappingResponse> getNfcMappings(
            Integer zoneId, Integer areaId, Boolean hasNfc, String search) {

        List<SeatEntity> seats;
        if (zoneId != null) {
            seats = seatRepository.findByZone_ZoneId(zoneId);
        } else if (areaId != null) {
            seats = seatRepository.findByAreaId(areaId);
        } else {
            seats = seatRepository.findAll();
        }

        return seats.stream()
                .filter(seat -> {
                    // Filter by hasNfc
                    if (hasNfc != null) {
                        boolean mapped = seat.getNfcTagUid() != null && !seat.getNfcTagUid().isEmpty();
                        if (hasNfc != mapped)
                            return false;
                    }
                    // Filter by search text (seatCode)
                    if (search != null && !search.trim().isEmpty()) {
                        return seat.getSeatCode().toLowerCase().contains(search.trim().toLowerCase());
                    }
                    return true;
                })
                .map(seat -> {
                    var zone = seat.getZone();
                    var area = zone != null ? zone.getArea() : null;
                    boolean mapped = seat.getNfcTagUid() != null && !seat.getNfcTagUid().isEmpty();

                    return slib.com.example.dto.zone_config.NfcMappingResponse.builder()
                            .seatId(seat.getSeatId())
                            .seatCode(seat.getSeatCode())
                            .zoneId(zone != null ? zone.getZoneId() : null)
                            .zoneName(zone != null ? zone.getZoneName() : null)
                            .areaId(area != null ? area.getAreaId() : null)
                            .areaName(area != null ? area.getAreaName() : null)
                            .hasNfcTag(mapped)
                            .maskedNfcUid(mapped ? nfcUidHasher.maskUid(seat.getNfcTagUid()) : null)
                            .updatedAt(seat.getNfcTagUidUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Get NFC info for a specific seat (FE-49: Chi tiết cấu hình NFC của ghế).
     */
    public slib.com.example.dto.zone_config.NfcInfoResponse getNfcInfo(Integer seatId) {
        SeatEntity seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found with id: " + seatId));

        var zone = seat.getZone();
        var area = zone != null ? zone.getArea() : null;
        boolean mapped = seat.getNfcTagUid() != null && !seat.getNfcTagUid().isEmpty();

        return slib.com.example.dto.zone_config.NfcInfoResponse.builder()
                .seatId(seat.getSeatId())
                .seatCode(seat.getSeatCode())
                .zoneId(zone != null ? zone.getZoneId() : null)
                .zoneName(zone != null ? zone.getZoneName() : null)
                .areaId(area != null ? area.getAreaId() : null)
                .areaName(area != null ? area.getAreaName() : null)
                .nfcMapped(mapped)
                .nfcUidMasked(mapped ? nfcUidHasher.maskUid(seat.getNfcTagUid()) : null)
                .lastUpdated(seat.getNfcTagUidUpdatedAt())
                .build();
    }
}
