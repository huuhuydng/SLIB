package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import slib.com.example.entity.booking.ReservationEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {
        List<ReservationEntity> findByUserId(UUID userId);

        List<ReservationEntity> findBySeat_SeatId(Integer seatId);

        List<ReservationEntity> findByEndTimeBeforeAndStatus(LocalDateTime time, String status);

        List<ReservationEntity> findByCreatedAtBeforeAndStatus(LocalDateTime time, String status);

        // Delete all reservations by seat ID (for cascade delete when seat is deleted)
        void deleteBySeat_SeatId(Integer seatId);

        // Delete all reservations by user ID (for cascade delete when user is deleted)
        void deleteByUser_Id(UUID userId);

        // Count total bookings by user ID
        long countByUserId(UUID userId);

        /**
         * Find overlapping active reservations for a seat in a time range.
         * Overlap logic: (start_time < query_end) AND (end_time > query_start)
         * Only includes PROCESSING, BOOKED, or CONFIRMED statuses.
         */
        @Query("SELECT r FROM ReservationEntity r WHERE r.seat.seatId = :seatId " +
                        "AND r.status IN ('PROCESSING', 'BOOKED', 'CONFIRMED') " +
                        "AND r.startTime < :endTime AND r.endTime > :startTime")
        List<ReservationEntity> findOverlappingReservations(
                        @Param("seatId") Integer seatId,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        // Dashboard queries
        long countByStatus(String status);

        long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

        long countByStatusAndCreatedAtBetween(String status, LocalDateTime start, LocalDateTime end);

        List<ReservationEntity> findTop9ByOrderByCreatedAtDesc();

        List<ReservationEntity> findTop7ByStatusOrderByCreatedAtDesc(String status);

        // Đếm booking đã xác nhận trong ngày (BOOKED + CONFIRMED + COMPLETED, không
        // giảm khi đổi status)
        @Query("SELECT COUNT(r) FROM ReservationEntity r WHERE r.status IN ('BOOKED', 'CONFIRMED', 'COMPLETED') " +
                        "AND r.createdAt BETWEEN :start AND :end")
        long countConfirmedBookingsToday(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        // Dashboard: đếm đặt chỗ đã xác nhận theo từng ngày (BOOKED + CONFIRMED +
        // COMPLETED)
        @Query(value = "SELECT CAST(created_at AS date) as booking_date, COUNT(*) as cnt " +
                        "FROM reservations WHERE created_at >= :startDate AND status IN ('BOOKED', 'CONFIRMED', 'COMPLETED') "
                        +
                        "GROUP BY CAST(created_at AS date) ORDER BY booking_date", nativeQuery = true)
        List<Object[]> countBookingsByDay(@Param("startDate") LocalDateTime startDate);

        // Tính tổng số phút học từ reservation EXPIRED (endTime - startTime)
        @Query(value = "SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (end_time - start_time)) / 60), 0) " +
                        "FROM reservations WHERE user_id = :userId AND status = 'EXPIRED'", nativeQuery = true)
        long getTotalStudyMinutesByUser(@Param("userId") UUID userId);
}
