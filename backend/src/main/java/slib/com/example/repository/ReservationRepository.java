package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.ReservationEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {
    
    // === Mobile App Methods ===
    List<ReservationEntity> findByUserId(UUID userId);

    List<ReservationEntity> findBySeat_SeatId(Integer seatId);

    List<ReservationEntity> findByEndTimeBeforeAndStatus(LocalDateTime time, String status);

    List<ReservationEntity> findByCreatedAtBeforeAndStatus(LocalDateTime time, String status);

    // === Web Admin Methods ===
    List<ReservationEntity> findByStatus(String status);
    
    /**
     * Tìm các reservation bị overlap về thời gian cho 1 ghế cụ thể
     * Dùng cho validation khi đặt ghế (tránh double booking)
     */
    @Query("SELECT r FROM ReservationEntity r JOIN FETCH r.seat WHERE r.seat.seatId = :seatId " +
           "AND r.status IN ('BOOKED', 'PROCESSING') " +
           "AND r.startTime < :endTime AND r.endTime > :startTime")
    List<ReservationEntity> findOverlappingReservations(
        @Param("seatId") Integer seatId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * Lấy tất cả reservations trong khoảng thời gian
     * Dùng cho web admin view reservations by time range
     */
    @Query("SELECT r FROM ReservationEntity r JOIN FETCH r.seat WHERE " +
           "r.status IN ('BOOKED', 'PROCESSING') " +
           "AND r.startTime < :endTime AND r.endTime > :startTime")
    List<ReservationEntity> findReservationsInTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * Đếm số ghế đang được đặt trong khoảng thời gian
     * Dùng cho statistics & analytics
     */
    @Query("SELECT COUNT(DISTINCT r.seat.seatId) FROM ReservationEntity r WHERE " +
           "r.status IN ('BOOKED', 'PROCESSING') " +
           "AND r.startTime < :endTime AND r.endTime > :startTime")
    Long countOccupiedSeatsInTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * Lấy các reservation ĐANG ACTIVE tại thời điểm hiện tại (real-time)
     * Dùng cho màn hình quản lý chỗ ngồi real-time của web admin
     */
    @Query("SELECT r FROM ReservationEntity r JOIN FETCH r.seat WHERE " +
           "r.status IN ('BOOKED', 'PROCESSING') " +
           "AND r.startTime <= :currentTime " +
           "AND r.endTime > :currentTime")
    List<ReservationEntity> findActiveReservationsAtTime(@Param("currentTime") LocalDateTime currentTime);
}
