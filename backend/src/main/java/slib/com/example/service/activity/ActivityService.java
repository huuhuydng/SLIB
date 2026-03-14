package slib.com.example.service.activity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import slib.com.example.entity.activity.ActivityLogEntity;
import slib.com.example.entity.activity.PointTransactionEntity;
import slib.com.example.repository.hce.AccessLogRepository;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.activity.ActivityLogRepository;
import slib.com.example.repository.activity.PointTransactionRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityLogRepository activityLogRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final AccessLogRepository accessLogRepository;
    private final ReservationRepository reservationRepository;

    // ========== Activity Logs ==========

    public List<ActivityLogEntity> getActivitiesByUser(UUID userId) {
        return activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<ActivityLogEntity> getActivitiesByUser(UUID userId, int limit) {
        return activityLogRepository.findByUserIdWithLimit(userId, limit);
    }

    public ActivityLogEntity logActivity(ActivityLogEntity activity) {
        return activityLogRepository.save(activity);
    }

    /**
     * Get total study hours for user (from reservations COMPLETED only)
     */
    public double getTotalStudyHours(UUID userId) {
        long totalMinutes = reservationRepository.getTotalStudyMinutesByUser(userId);
        return Math.round(totalMinutes / 6.0) / 10.0; // Round to 1 decimal
    }

    /**
     * Get total visits count (from access_logs - actual HCE check-in count)
     */
    public long getTotalVisits(UUID userId) {
        return accessLogRepository.countByUserId(userId);
    }

    // ========== Point Transactions ==========

    public List<PointTransactionEntity> getPointTransactionsByUser(UUID userId) {
        return pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<PointTransactionEntity> getPointTransactionsByUser(UUID userId, int limit) {
        return pointTransactionRepository.findByUserIdWithLimit(userId, limit);
    }

    public PointTransactionEntity addPointTransaction(PointTransactionEntity transaction) {
        return pointTransactionRepository.save(transaction);
    }

    public int getTotalEarnedPoints(UUID userId) {
        return pointTransactionRepository.getTotalEarnedPoints(userId);
    }

    public int getTotalLostPoints(UUID userId) {
        return Math.abs(pointTransactionRepository.getTotalLostPoints(userId));
    }

    // ========== Seed Sample Data ==========

    public void seedSampleData(UUID userId) {
        ZonedDateTime now = ZonedDateTime.now();

        // Sample Activity Logs
        activityLogRepository.save(ActivityLogEntity.builder()
                .userId(userId)
                .activityType(ActivityLogEntity.TYPE_CHECK_OUT)
                .title("Check-out thành công")
                .description("Khu yên tỉnh - Ghế A15")
                .seatCode("A15")
                .zoneName("Khu yên tỉnh")
                .durationMinutes(125)
                .createdAt(now.minusHours(1))
                .build());

        activityLogRepository.save(ActivityLogEntity.builder()
                .userId(userId)
                .activityType(ActivityLogEntity.TYPE_CHECK_IN)
                .title("Check-in thành công")
                .description("Khu yên tỉnh - Ghế A15")
                .seatCode("A15")
                .zoneName("Khu yên tỉnh")
                .createdAt(now.minusHours(3))
                .build());

        activityLogRepository.save(ActivityLogEntity.builder()
                .userId(userId)
                .activityType(ActivityLogEntity.TYPE_BOOKING_SUCCESS)
                .title("Đặt chỗ thành công")
                .description("Đã đặt ghế A15 (09:00 - 11:00)")
                .seatCode("A15")
                .zoneName("Khu yên tỉnh")
                .createdAt(now.minusHours(5))
                .build());

        activityLogRepository.save(ActivityLogEntity.builder()
                .userId(userId)
                .activityType(ActivityLogEntity.TYPE_CHECK_OUT)
                .title("Check-out thành công")
                .description("Khu thảo luận - Ghế B02")
                .seatCode("B02")
                .zoneName("Khu thảo luận")
                .durationMinutes(209)
                .createdAt(now.minusDays(1))
                .build());

        activityLogRepository.save(ActivityLogEntity.builder()
                .userId(userId)
                .activityType(ActivityLogEntity.TYPE_GATE_ENTRY)
                .title("Check-in vào cửa")
                .description("Khu thảo luận - Ghế B02")
                .seatCode("B02")
                .zoneName("Khu thảo luận")
                .createdAt(now.minusDays(1).minusHours(4))
                .build());

        activityLogRepository.save(ActivityLogEntity.builder()
                .userId(userId)
                .activityType(ActivityLogEntity.TYPE_NO_SHOW)
                .title("Không check-in (No-show)")
                .description("Đã đặt ghế C05 nhưng không đến check-in")
                .seatCode("C05")
                .zoneName("Khu máy tính")
                .createdAt(now.minusDays(3))
                .build());

        // Sample Point Transactions
        pointTransactionRepository.save(PointTransactionEntity.builder()
                .userId(userId)
                .points(-10)
                .transactionType(PointTransactionEntity.TYPE_NO_SHOW_PENALTY)
                .title("Không check-in (No-show)")
                .description("Bạn đã đặt ghế A15 nhưng không đến check-in trong thời gian quy định.")
                .balanceAfter(90)
                .createdAt(now.minusDays(3))
                .build());

        pointTransactionRepository.save(PointTransactionEntity.builder()
                .userId(userId)
                .points(5)
                .transactionType(PointTransactionEntity.TYPE_WEEKLY_BONUS)
                .title("Thưởng: Tuần học chăm chỉ")
                .description("Hoàn thành 10 giờ học trong tuần.")
                .balanceAfter(100)
                .createdAt(now.minusDays(7))
                .build());

        pointTransactionRepository.save(PointTransactionEntity.builder()
                .userId(userId)
                .points(-5)
                .transactionType(PointTransactionEntity.TYPE_CHECK_OUT_LATE_PENALTY)
                .title("Check-out trễ")
                .description("Bạn rời khỏi thư viện nhưng quên check-out quá 30 phút.")
                .balanceAfter(95)
                .createdAt(now.minusDays(10))
                .build());

        pointTransactionRepository.save(PointTransactionEntity.builder()
                .userId(userId)
                .points(10)
                .transactionType(PointTransactionEntity.TYPE_WEEKLY_BONUS)
                .title("Thưởng: Tuần học xuất sắc")
                .description("Hoàn thành 20 giờ học trong tuần.")
                .balanceAfter(100)
                .createdAt(now.minusDays(14))
                .build());
    }
}
