package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.ReservationResponse;
import slib.com.example.entity.*;
import slib.com.example.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserReservationService {
    
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    
    /**
     * Tạo reservation mới cho user
     * Tự động chuyển seat status sang BOOKED
     */
    @Transactional
    public ReservationResponse createReservation(UUID userId, String seatCode, 
                                                 LocalDateTime startTime, LocalDateTime endTime) {
        // 1. Tìm user
        slib.com.example.entity.users.User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 2. Tìm seat
        SeatEntity seat = seatRepository.findBySeatCode(seatCode)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatCode));
        
        // 3. Kiểm tra seat không bị hạn chế
        if (seat.getSeatStatus() == SeatStatus.UNAVAILABLE) {
            throw new RuntimeException("Ghế đang bị hạn chế, không thể đặt");
        }
        
        // 4. Kiểm tra không có reservation trùng giờ (QUAN TRỌNG)
        // Đây là validation chính để tránh double booking
        List<ReservationEntity> overlapping = reservationRepository.findOverlappingReservations(
                seat.getSeatId(), startTime, endTime);
        
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Ghế đã có người đặt trong khung giờ này");
        }
        
        // 6. Tạo reservation
        ReservationEntity reservation = ReservationEntity.builder()
                .reservationId(UUID.randomUUID())
                .user(user)
                .seat(seat)
                .startTime(startTime)
                .endTime(endTime)
                .status("BOOKED")
                .build();
        
        ReservationEntity saved = reservationRepository.save(reservation);
        
        // ✅ Không cần update seat_status trong DB
        // Status BOOKED sẽ được tính động dựa trên reservation + time range
        System.out.println("✅ Created reservation for seat " + seatCode + " from " + startTime + " to " + endTime);
        
        return convertToResponse(saved);
    }
    
    /**
     * Hủy reservation
     * Tự động trả lại seat status = AVAILABLE
     */
    @Transactional
    public void cancelReservation(UUID reservationId, UUID userId) {
        // 1. Tìm reservation
        ReservationEntity reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        
        // 2. Kiểm tra quyền hủy
        if (!reservation.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy reservation này");
        }
        
        // 3. Hủy reservation
        reservation.setStatus("CANCEL");
        reservationRepository.save(reservation);
        
        // ✅ Không cần update seat_status trong DB
        // Ghế sẽ tự động available khi không có reservation trong khung giờ
        System.out.println("✅ Cancelled reservation for seat " + reservation.getSeat().getSeatCode());
    }
    
    /**
     * Lấy tất cả reservation của user
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(UUID userId) {
        List<ReservationEntity> reservations = reservationRepository.findByUserId(userId);
        return reservations.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy chi tiết 1 reservation
     */
    @Transactional(readOnly = true)
    public ReservationResponse getReservationDetails(UUID reservationId, UUID userId) {
        ReservationEntity reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        
        // Kiểm tra quyền xem
        if (!reservation.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xem reservation này");
        }
        
        return convertToResponse(reservation);
    }
    
    /**
     * Convert Reservation entity sang DTO
     */
    private ReservationResponse convertToResponse(ReservationEntity reservation) {
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
