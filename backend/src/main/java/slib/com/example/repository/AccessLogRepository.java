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

        // Lấy danh sách access logs mới nhất, sắp xếp theo thời gian check-in giảm dần
        @Query("SELECT a FROM AccessLog a LEFT JOIN FETCH a.user ORDER BY a.checkInTime DESC")
        java.util.List<AccessLog> findAllOrderByCheckInTimeDesc();

        // Lấy danh sách access logs theo ngày
        @Query("SELECT a FROM AccessLog a LEFT JOIN FETCH a.user WHERE CAST(a.checkInTime AS date) = CURRENT_DATE ORDER BY a.checkInTime DESC")
        java.util.List<AccessLog> findTodayLogs();

        // Lấy danh sách access logs theo khoảng thời gian (date range)
        @Query("SELECT a FROM AccessLog a LEFT JOIN FETCH a.user WHERE CAST(a.checkInTime AS date) BETWEEN :startDate AND :endDate ORDER BY a.checkInTime DESC")
        java.util.List<AccessLog> findLogsByDateRange(@Param("startDate") java.time.LocalDate startDate,
                        @Param("endDate") java.time.LocalDate endDate);

        // Xóa log khi xóa user
        void deleteByUser_Id(UUID userId);

        // Dashboard: đếm check-in theo từng ngày trong khoảng thời gian
        @Query(value = "SELECT CAST(check_in_time AS date) as log_date, COUNT(*) as cnt " +
                        "FROM access_logs WHERE check_in_time >= :startDate " +
                        "GROUP BY CAST(check_in_time AS date) ORDER BY log_date", nativeQuery = true)
        List<Object[]> countCheckInsByDay(@Param("startDate") LocalDateTime startDate);

        // Dashboard: top 5 sinh viên có thời gian học nhiều nhất (trong 30 ngày gần
        // đây)
        @Query(value = "SELECT a.user_id, u.full_name, u.user_code, COUNT(*) as visit_count, " +
                        "COALESCE(SUM(EXTRACT(EPOCH FROM (a.check_out_time - a.check_in_time)) / 60), 0) as total_minutes "
                        +
                        "FROM access_logs a JOIN users u ON a.user_id = u.id " +
                        "WHERE a.check_in_time >= :startDate AND a.check_out_time IS NOT NULL " +
                        "GROUP BY a.user_id, u.full_name, u.user_code " +
                        "ORDER BY total_minutes DESC LIMIT 5", nativeQuery = true)
        List<Object[]> findTopStudentsByStudyTime(@Param("startDate") LocalDateTime startDate);
}