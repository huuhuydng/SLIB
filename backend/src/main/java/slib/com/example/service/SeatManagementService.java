package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.*;
import slib.com.example.entity.*;
import slib.com.example.repository.*;
import slib.com.example.entity.SeatStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatManagementService {
    
    private final SeatRepository seatRepository;
    private final ZoneRepository zoneRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    
    /**
     * Lấy tất cả ghế với trạng thái trong khoảng thời gian
     */
    @Transactional(readOnly = true)
    public SeatManagementResponse getAllSeatsWithStatus(LocalDateTime startTime, LocalDateTime endTime, String zoneFilter) {
        System.out.println("=== getAllSeatsWithStatus called ===");
        System.out.println("startTime: " + startTime);
        System.out.println("endTime: " + endTime);
        System.out.println("zoneFilter: " + zoneFilter);
        
        List<SeatEntity> seats;
        
        if (zoneFilter != null && !zoneFilter.equals("Tất cả khu vực")) {
            seats = seatRepository.findByZone_ZoneName(zoneFilter);
        } else {
            seats = seatRepository.findAll();
        }
        
        // ✅ Xử lý 2 cases:
        // 1. startTime/endTime = null → Real-time (hiện tại)
        // 2. startTime/endTime có giá trị → Khung giờ cụ thể
        List<ReservationEntity> reservations;
        
        if (startTime == null || endTime == null) {
            // Case 1: Real-time - lấy reservations đang active hiện tại
            LocalDateTime now = LocalDateTime.now();
            System.out.println("🔴 REAL-TIME MODE: now = " + now);
            reservations = reservationRepository.findActiveReservationsAtTime(now);
        } else {
            // Case 2: Khung giờ cụ thể - lấy reservations trong khoảng thời gian
            System.out.println("🟢 TIME-RANGE MODE: " + startTime + " to " + endTime);
            reservations = reservationRepository.findReservationsInTimeRange(startTime, endTime);
        }
        
        System.out.println("Found " + reservations.size() + " reservations");
        reservations.forEach(r -> System.out.println("  - Seat: " + r.getSeat().getSeatCode() + 
            ", Time: " + r.getStartTime() + " to " + r.getEndTime()));
        
        List<Integer> occupiedSeatIds = reservations.stream()
                .map(r -> r.getSeat().getSeatId())
                .distinct()
                .collect(Collectors.toList());
        
        System.out.println("Occupied seat IDs: " + occupiedSeatIds);
        
        List<Integer> restrictedSeatIds = seats.stream()
                .filter(s -> s.getSeatStatus() == SeatStatus.UNAVAILABLE)
                .map(SeatEntity::getSeatId)
                .collect(Collectors.toList());
        
        List<SeatResponse> seatResponses = seats.stream()
                .map(seat -> {
                    SeatResponse response = new SeatResponse();
                    response.setSeatId(seat.getSeatId());
                    response.setSeatCode(seat.getSeatCode());
                    response.setZoneName(seat.getZone().getZoneName());
                    response.setZoneId(seat.getZone().getZoneId());
                    
                    // ✅ XÁC ĐỊNH STATUS ĐỘNG DựA VÀO RESERVATION
                    String dynamicStatus;
                    if (seat.getSeatStatus() == SeatStatus.UNAVAILABLE) {
                        // Ghế bị hạn chế bởi thủ thư → luôn là UNAVAILABLE
                        dynamicStatus = "UNAVAILABLE";
                    } else if (occupiedSeatIds.contains(seat.getSeatId())) {
                        // Ghế có reservation trong khung giờ này → BOOKED
                        dynamicStatus = "BOOKED";
                    } else {
                        // Ghế trống, không bị hạn chế → AVAILABLE
                        dynamicStatus = "AVAILABLE";
                    }
                    
                    response.setStatus(dynamicStatus);
                    response.setIsOccupied(occupiedSeatIds.contains(seat.getSeatId()));
                    response.setPositionX(seat.getPositionX());
                    response.setPositionY(seat.getPositionY());
                    
                    // Log chi tiết cho ghế A6
                    if ("A6".equals(seat.getSeatCode())) {
                        System.out.println("🔍 [A6 Response] Status: " + dynamicStatus 
                            + " | IsOccupied: " + occupiedSeatIds.contains(seat.getSeatId())
                            + " | SeatId: " + seat.getSeatId()
                            + " | InOccupiedList: " + occupiedSeatIds.contains(seat.getSeatId()));
                    }
                    
                    return response;
                })
                .collect(Collectors.toList());
        
        SeatOccupancyStats stats = calculateStats(seats.size(), occupiedSeatIds.size(), restrictedSeatIds.size());
        
        List<String> restrictedCodes = seats.stream()
                .filter(s -> s.getSeatStatus() == SeatStatus.UNAVAILABLE)
                .map(SeatEntity::getSeatCode)
                .collect(Collectors.toList());
        
        SeatManagementResponse response = new SeatManagementResponse(seatResponses, stats, restrictedCodes);
        
        // Thêm thông tin về query mode để frontend biết đang dùng mode nào
        if (startTime == null || endTime == null) {
            response.setQueryMode("REAL_TIME");
            response.setQueryStartTime(null);
            response.setQueryEndTime(null);
        } else {
            response.setQueryMode("TIME_RANGE");
            response.setQueryStartTime(startTime);
            response.setQueryEndTime(endTime);
        }
        
        // Log response summary
        System.out.println("=== RESPONSE SUMMARY ===");
        System.out.println("Query Mode: " + response.getQueryMode());
        System.out.println("Total seats in response: " + seatResponses.size());
        System.out.println("Stats - Booked: " + stats.getOccupiedSeats() + ", Available: " + stats.getAvailableSeats());
        
        return response;
    }
    
    /**
     * Lấy thông tin chi tiết 1 ghế
     */
    @Transactional(readOnly = true)
    public SeatResponse getSeatDetails(String seatCode, LocalDateTime checkTime) {
        SeatEntity seat = seatRepository.findBySeatCode(seatCode)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatCode));
        
        LocalDateTime endTime = checkTime.plusHours(2);
        List<ReservationEntity> reservations = reservationRepository.findOverlappingReservations(
                seat.getSeatId(), checkTime, endTime);
        
        SeatResponse response = new SeatResponse();
        response.setSeatId(seat.getSeatId());
        response.setSeatCode(seat.getSeatCode());
        response.setZoneName(seat.getZone().getZoneName());
        response.setZoneId(seat.getZone().getZoneId());
        response.setStatus(seat.getSeatStatus().name());
        response.setIsOccupied(!reservations.isEmpty());
        response.setPositionX(seat.getPositionX());
        response.setPositionY(seat.getPositionY());
        
        return response;
    }
    
    /**
     * Hạn chế ghế - Set status = UNAVAILABLE
     */
    @Transactional
    public SeatRestrictionResponse addSeatRestriction(SeatRestrictionRequest request, UUID librarianId) {
        SeatEntity seat = seatRepository.findBySeatCode(request.getSeatCode())
                .orElseThrow(() -> new RuntimeException("Seat not found: " + request.getSeatCode()));
        
        slib.com.example.entity.users.User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new RuntimeException("Librarian not found"));
        
        seat.setSeatStatus(SeatStatus.UNAVAILABLE);
        seatRepository.save(seat);
        
        // Hủy các reservation đang tồn tại cho ghế này
        if (request.getStartTime() != null && request.getEndTime() != null) {
            List<ReservationEntity> overlappingReservations = reservationRepository.findOverlappingReservations(
                    seat.getSeatId(), request.getStartTime(), request.getEndTime());
            
            for (ReservationEntity reservation : overlappingReservations) {
                if ("BOOKED".equals(reservation.getStatus()) || 
                    "PROCESSING".equals(reservation.getStatus())) {
                    reservation.setStatus("CANCEL");
                    reservationRepository.save(reservation);
                }
            }
        }
        
        // Tạo response
        SeatRestrictionResponse response = new SeatRestrictionResponse();
        response.setRestrictionId(UUID.randomUUID());
        response.setSeatId(seat.getSeatId());
        response.setSeatCode(seat.getSeatCode());
        response.setRestrictedByName(librarian.getFullName());
        response.setReason(request.getReason());
        response.setStartTime(request.getStartTime());
        response.setEndTime(request.getEndTime());
        response.setIsActive(true);
        response.setCreatedAt(LocalDateTime.now());
        
        return response;
    }
    
    /**
     * Bỏ hạn chế ghế - Set status = AVAILABLE
     */
    @Transactional
    public void removeSeatRestriction(String seatCode) {
        SeatEntity seat = seatRepository.findBySeatCode(seatCode)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatCode));
        
        if (seat.getSeatStatus() != SeatStatus.UNAVAILABLE) {
            throw new RuntimeException("Ghế " + seatCode + " không có hạn chế nào để gỡ");
        }
        
        seat.setSeatStatus(SeatStatus.AVAILABLE);
        seatRepository.save(seat);
    }
    
    /**
     * Lấy tất cả các hạn chế đang active (ghế có status = UNAVAILABLE)
     */
    @Transactional(readOnly = true)
    public List<SeatRestrictionResponse> getAllActiveRestrictions() {
        List<SeatEntity> restrictedSeats = seatRepository.findBySeatStatus(SeatStatus.UNAVAILABLE);

        return restrictedSeats.stream()
                .map(seat -> {
                    SeatRestrictionResponse response = new SeatRestrictionResponse();
                    response.setRestrictionId(UUID.randomUUID()); // Mock ID
                    response.setSeatId(seat.getSeatId());
                    response.setSeatCode(seat.getSeatCode());
                    response.setRestrictedByName("System");
                    response.setReason("Seat restricted");
                    response.setIsActive(true);
                    response.setCreatedAt(LocalDateTime.now());
                    return response;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy các reservation trong khoảng thời gian
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        List<ReservationEntity> reservations = reservationRepository.findReservationsInTimeRange(startTime, endTime);
        return reservations.stream()
                .map(this::mapToReservationResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy tất cả zones
     */
    @Transactional(readOnly = true)
    public List<ZoneResponse> getAllZones() {
        List<ZoneEntity> zones = zoneRepository.findAll();
        return zones.stream()
                .map(zone -> {
                    List<SeatEntity> zoneSeats = seatRepository.findByZone_ZoneId(zone.getZoneId());
                    long activeSeats = zoneSeats.stream()
                            .filter(s -> s.getSeatStatus() == SeatStatus.AVAILABLE)
                            .count();
                    
                    ZoneResponse response = new ZoneResponse();
                    response.setZoneId(zone.getZoneId());
                    response.setZoneName(zone.getZoneName());
                    response.setZoneDescription(zone.getZoneDes());
                    response.setHasPowerOutlet(zone.getHasPowerOutlet());
                    response.setTotalSeats(zoneSeats.size());
                    response.setActiveSeats((int) activeSeats);
                    return response;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Tính toán thống kê
     */
    private SeatOccupancyStats calculateStats(int totalSeats, int occupiedSeats, int restrictedSeats) {
        int availableSeats = totalSeats - occupiedSeats - restrictedSeats;
        double occupancyRate = totalSeats > 0 ? ((double) occupiedSeats / totalSeats) * 100 : 0.0;
        
        return new SeatOccupancyStats(
                totalSeats,
                occupiedSeats,
                restrictedSeats,
                Math.max(0, availableSeats),
                Math.round(occupancyRate * 100.0) / 100.0
        );
    }
    
    /**
     * Map Reservation sang DTO
     */
    private ReservationResponse mapToReservationResponse(ReservationEntity reservation) {
        ReservationResponse response = new ReservationResponse();
        response.setReservationId(reservation.getReservationId());
        response.setUserId(reservation.getUser().getId());
        response.setUserFullName(reservation.getUser().getFullName());
        response.setStudentCode(reservation.getUser().getStudentCode());
        response.setSeatId(reservation.getSeat().getSeatId());
        response.setSeatCode(reservation.getSeat().getSeatCode());
        response.setZoneName(reservation.getSeat().getZone().getZoneName());
        response.setStartTime(reservation.getStartTime());
        response.setEndTime(reservation.getEndTime());
        response.setStatus(reservation.getStatus());
        response.setCreatedAt(reservation.getCreatedAt());
        return response;
    }
}
