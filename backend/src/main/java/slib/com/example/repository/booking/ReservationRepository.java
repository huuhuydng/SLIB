package slib.com.example.repository.booking;

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

        List<ReservationEntity> findByStatusAndEndTimeBetween(String status, LocalDateTime start, LocalDateTime end);

        List<ReservationEntity> findByCreatedAtBeforeAndStatus(LocalDateTime time, String status);

        List<ReservationEntity> findByStatus(String status);

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

        /**
         * Find BOOKED reservations that need to be checked for late check-in penalty.
         * Returns reservations that are BOOKED status and have started.
         */
        @Query("SELECT r FROM ReservationEntity r WHERE r.status = 'BOOKED' " +
                        "AND r.startTime <= :currentTime")
        List<ReservationEntity> findBookedReservationsStarted(@Param("currentTime") LocalDateTime currentTime);

        // Dashboard queries
        long countByStatus(String status);

        long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

        long countByStatusAndCreatedAtBetween(String status, LocalDateTime start, LocalDateTime end);

        List<ReservationEntity> findTop9ByOrderByCreatedAtDesc();

        List<ReservationEntity> findByStartTimeBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

        List<ReservationEntity> findTop7ByStatusOrderByCreatedAtDesc(String status);

        // Đếm tổng đặt chỗ trong ngày (tất cả trừ CANCELLED - không giảm khi đổi
        // status)
        @Query("SELECT COUNT(r) FROM ReservationEntity r WHERE r.status NOT IN ('CANCELLED', 'CANCEL') " +
                        "AND r.createdAt BETWEEN :start AND :end")
        long countConfirmedBookingsToday(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        // Tìm reservations CONFIRMED (đã check-in) cho một ghế trong khoảng thời gian
        @Query("SELECT r FROM ReservationEntity r WHERE r.seat.seatId = :seatId " +
                        "AND r.status = 'CONFIRMED' " +
                        "AND r.startTime < :endTime AND r.endTime > :startTime")
        List<ReservationEntity> findConfirmedReservationsForSeat(
                        @Param("seatId") Integer seatId,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        // Top 5 sinh viên có thời gian sử dụng thực tế nhiều nhất
        @Query(value = "SELECT r.user_id, u.full_name, u.user_code, COUNT(*) as visit_count, " +
                        "COALESCE(SUM(EXTRACT(EPOCH FROM (" +
                        "CASE " +
                        "WHEN r.status = 'CONFIRMED' THEN LEAST(CURRENT_TIMESTAMP, COALESCE(r.actual_end_time, r.end_time)) " +
                        "ELSE COALESCE(r.actual_end_time, r.end_time) " +
                        "END - COALESCE(r.confirmed_at, r.start_time))) / 60), 0) as total_minutes, " +
                        "u.avt_url " +
                        "FROM reservations r JOIN users u ON r.user_id = u.id " +
                        "WHERE COALESCE(r.confirmed_at, r.start_time) >= :startDate " +
                        "AND r.status IN ('CONFIRMED', 'COMPLETED') " +
                        "GROUP BY r.user_id, u.full_name, u.user_code, u.avt_url " +
                        "ORDER BY total_minutes DESC LIMIT 5", nativeQuery = true)
        List<Object[]> findTopStudentsByReservationTime(@Param("startDate") LocalDateTime startDate);

        // Dashboard: đếm đặt chỗ đã xác nhận theo từng ngày (BOOKED + CONFIRMED +
        // COMPLETED)
        @Query(value = "SELECT CAST(start_time AS date) as booking_date, COUNT(*) as cnt " +
                        "FROM reservations WHERE start_time >= :startDate AND status IN ('PROCESSING', 'BOOKED', 'CONFIRMED', 'COMPLETED', 'EXPIRED') "
                        +
                        "GROUP BY CAST(start_time AS date) ORDER BY booking_date", nativeQuery = true)
        List<Object[]> countBookingsByDay(@Param("startDate") LocalDateTime startDate);

        @Query(value = "SELECT EXTRACT(HOUR FROM start_time) as hour_of_day, COUNT(*) as cnt " +
                        "FROM reservations WHERE start_time >= :startDate " +
                        "AND status IN ('PROCESSING', 'BOOKED', 'CONFIRMED', 'COMPLETED', 'EXPIRED') " +
                        "GROUP BY EXTRACT(HOUR FROM start_time) ORDER BY hour_of_day", nativeQuery = true)
        List<Object[]> countBookingsByHour(@Param("startDate") LocalDateTime startDate);

        // Tính tổng số phút học từ reservation COMPLETED (endTime - startTime)
        @Query(value = "SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (COALESCE(actual_end_time, end_time) - COALESCE(confirmed_at, start_time))) / 60), 0) " +
                        "FROM reservations WHERE user_id = :userId AND status = 'COMPLETED'", nativeQuery = true)
        long getTotalStudyMinutesByUser(@Param("userId") UUID userId);

        // Statistic: đếm booking theo status group trong range
        @Query(value = "SELECT status, COUNT(*) as cnt FROM reservations " +
                        "WHERE created_at >= :startDate GROUP BY status", nativeQuery = true)
        List<Object[]> countBookingsGroupByStatus(@Param("startDate") LocalDateTime startDate);

        // Statistic: đếm booking cho mỗi zone trong range
        @Query(value = "SELECT s.zone_id, z.zone_name, a.area_name, COUNT(r.reservation_id) as booking_count, " +
                        "(SELECT COUNT(*) FROM seats s2 WHERE s2.zone_id = s.zone_id) as total_seats " +
                        "FROM reservations r " +
                        "JOIN seats s ON r.seat_id = s.seat_id " +
                        "JOIN zones z ON s.zone_id = z.zone_id " +
                        "JOIN areas a ON z.area_id = a.area_id " +
                        "WHERE r.created_at >= :startDate AND r.status IN ('BOOKED', 'CONFIRMED', 'COMPLETED', 'EXPIRED') "
                        +
                        "GROUP BY s.zone_id, z.zone_name, a.area_name " +
                        "ORDER BY booking_count DESC", nativeQuery = true)
        List<Object[]> countBookingsByZone(@Param("startDate") LocalDateTime startDate);

        // Performance: query methods thay cho findAll + stream filter
        List<ReservationEntity> findByStatusAndStartTimeBetween(String status, LocalDateTime start, LocalDateTime end);

        List<ReservationEntity> findByStatusIn(List<String> statuses);

        long countByStartTimeBetweenAndStatusIn(LocalDateTime start, LocalDateTime end, List<String> statuses);

        @Query("SELECT COUNT(r) FROM ReservationEntity r " +
                        "WHERE r.status IN :statuses " +
                        "AND r.startTime <= :now " +
                        "AND r.endTime >= :now")
        long countActiveReservationsAtTime(
                        @Param("now") LocalDateTime now,
                        @Param("statuses") List<String> statuses);

        @Query("SELECT COUNT(r) FROM ReservationEntity r " +
                        "WHERE r.status IN :statuses " +
                        "AND r.startTime > :now " +
                        "AND r.startTime <= :endTime")
        long countUpcomingReservationsBetween(
                        @Param("now") LocalDateTime now,
                        @Param("endTime") LocalDateTime endTime,
                        @Param("statuses") List<String> statuses);

        @Query(value = """
                        SELECT
                            z.zone_id,
                            z.zone_name,
                            a.area_name,
                            COUNT(s.seat_id) AS total_seats,
                            COUNT(DISTINCT CASE WHEN r.reservation_id IS NOT NULL THEN s.seat_id END) AS occupied_seats
                        FROM zones z
                        JOIN areas a ON a.area_id = z.area_id
                        LEFT JOIN seats s
                            ON s.zone_id = z.zone_id
                           AND s.is_active = true
                        LEFT JOIN reservations r
                            ON r.seat_id = s.seat_id
                           AND r.status = 'CONFIRMED'
                           AND r.start_time <= :now
                           AND r.end_time >= :now
                        GROUP BY z.zone_id, z.zone_name, a.area_name
                        ORDER BY z.zone_id
                        """, nativeQuery = true)
        List<Object[]> getZoneOccupancySnapshot(@Param("now") LocalDateTime now);

        long countByConfirmedAtBetween(LocalDateTime start, LocalDateTime end);

        long countByUserIdAndStartTimeBetweenAndStatusIn(UUID userId, LocalDateTime start, LocalDateTime end, List<String> statuses);

        long countByUser_IdAndStatusInAndEndTimeAfter(
                        UUID userId,
                        List<String> statuses,
                        LocalDateTime now);

        List<ReservationEntity> findTop1ByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

        @Query("""
                        SELECT r FROM ReservationEntity r
                        WHERE r.user.id = :userId
                        AND r.confirmedAt IS NOT NULL
                        AND r.status IN :statuses
                        AND COALESCE(r.actualEndTime, r.endTime) <= :effectiveEndTime
                        ORDER BY COALESCE(r.actualEndTime, r.endTime) DESC
                        """)
        List<ReservationEntity> findCompletedReservationsEligibleForFeedback(
                        @Param("userId") UUID userId,
                        @Param("effectiveEndTime") LocalDateTime effectiveEndTime,
                        @Param("statuses") List<String> statuses);
}
