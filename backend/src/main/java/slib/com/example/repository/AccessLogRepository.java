package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import slib.com.example.entity.hce.AccessLog;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, UUID> {
    @Query("SELECT a FROM AccessLog a WHERE a.user.id = :userId AND a.checkOutTime IS NULL ORDER BY a.checkInTime DESC")
    Optional<AccessLog> checkInUser(UUID userId);

    // Đếm số lần check-in của user
    @Query("SELECT COUNT(a) FROM AccessLog a WHERE a.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    // Tính tổng số phút học từ các phiên đã check-out (native query for PostgreSQL)
    @Query(value = "SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (check_out_time - check_in_time)) / 60), 0) FROM access_logs WHERE user_id = :userId AND check_out_time IS NOT NULL", nativeQuery = true)
    long getTotalStudyMinutes(@Param("userId") UUID userId);

    // Lấy danh sách access logs mới nhất, sắp xếp theo thời gian check-in giảm dần
    @Query("SELECT a FROM AccessLog a LEFT JOIN FETCH a.user ORDER BY a.checkInTime DESC")
    java.util.List<AccessLog> findAllOrderByCheckInTimeDesc();

    // Lấy danh sách access logs theo ngày
    @Query("SELECT a FROM AccessLog a LEFT JOIN FETCH a.user WHERE CAST(a.checkInTime AS date) = CURRENT_DATE ORDER BY a.checkInTime DESC")
    java.util.List<AccessLog> findTodayLogs();

    // Lấy danh sách access logs theo khoảng thời gian (date range)
    @Query("SELECT a FROM AccessLog a LEFT JOIN FETCH a.user WHERE CAST(a.checkInTime AS date) BETWEEN :startDate AND :endDate ORDER BY a.checkInTime DESC")
    java.util.List<AccessLog> findLogsByDateRange(@Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

    // Delete all access logs by user ID (for cascade delete when user is deleted)
    void deleteByUser_Id(UUID userId);
}