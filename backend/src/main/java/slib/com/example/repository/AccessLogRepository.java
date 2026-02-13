package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime; // THÊM DÒNG NÀY

import slib.com.example.entity.hce.AccessLog;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, UUID> {

    // Lấy 10 bản ghi mới nhất (dùng để fallback hoặc debug)
    List<AccessLog> findTop10ByOrderByCheckInTimeDesc();

    // LẤY DỮ LIỆU TRONG NGÀY: 
    @Query("SELECT a FROM AccessLog a WHERE a.checkInTime >= :startOfDay")
    List<AccessLog> findLogsFromStartOfDay(@Param("startOfDay") LocalDateTime startOfDay);

    // Tìm phiên đang hoạt động (chưa quẹt thẻ ra)
    @Query("SELECT a FROM AccessLog a WHERE a.user.id = :userId AND a.checkOutTime IS NULL ORDER BY a.checkInTime DESC")
    Optional<AccessLog> checkInUser(UUID userId);

    // Đếm số lần check-in của user
    @Query("SELECT COUNT(a) FROM AccessLog a WHERE a.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    // Tính tổng số phút học (native query PostgreSQL)
    @Query(value = "SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (check_out_time - check_in_time)) / 60), 0) FROM access_logs WHERE user_id = :userId AND check_out_time IS NOT NULL", nativeQuery = true)
    long getTotalStudyMinutes(@Param("userId") UUID userId);

    // Xóa log khi xóa user
    void deleteByUser_Id(UUID userId);
}