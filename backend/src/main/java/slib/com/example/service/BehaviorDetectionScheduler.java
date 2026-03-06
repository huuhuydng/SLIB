package slib.com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import slib.com.example.entity.analytics.StudentBehaviorEntity;
import slib.com.example.entity.hce.AccessLog;
import slib.com.example.repository.AccessLogRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Scheduler để phát hiện các hành vi bất thường của sinh viên
 * Như: giữ chỗ quá lâu mà không sử dụng
 */
@Service
public class BehaviorDetectionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BehaviorDetectionScheduler.class);

    private final AccessLogRepository accessLogRepository;
    private final StudentBehaviorService studentBehaviorService;

    // Ngưỡng thời gian để phát hiện giữ chỗ bất thường (phút)
    private static final int SEAT_HOLDING_THRESHOLD_MINUTES = 30;

    public BehaviorDetectionScheduler(AccessLogRepository accessLogRepository,
                                      StudentBehaviorService studentBehaviorService) {
        this.accessLogRepository = accessLogRepository;
        this.studentBehaviorService = studentBehaviorService;
    }

    /**
     * Chạy mỗi 5 phút để phát hiện hành vi giữ chỗ bất thường
     * Phát hiện: đã check-in nhưng không check-out quá lâu
     */
    @Scheduled(fixedRate = 300000) // 5 phút
    public void detectSeatHoldingBehavior() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threshold = now.minusMinutes(SEAT_HOLDING_THRESHOLD_MINUTES);

            // Tìm tất cả logs chưa checkout và đã check-in quá lâu
            List<AccessLog> uncheckedLogs = accessLogRepository.findAllOrderByCheckInTimeDesc()
                    .stream()
                    .filter(log -> log.getCheckOutTime() == null)
                    .filter(log -> log.getCheckInTime().isBefore(threshold))
                    .toList();

            for (AccessLog log : uncheckedLogs) {
                try {
                    UUID userId = log.getUserId();
                    LocalDateTime checkInTime = log.getCheckInTime();

                    long minutesAway = ChronoUnit.MINUTES.between(checkInTime, now);

                    // Ghi nhận hành vi giữ chỗ bất thường
                    studentBehaviorService.recordBehavior(
                            userId,
                            StudentBehaviorEntity.BehaviorType.SEAT_HOLDING,
                            "Phát hiện giữ chỗ bất thường: đã rời " + minutesAway + " phút không checkout",
                            null,
                            null,
                            null,
                            -5, // Trừ điểm
                            String.format("{\"checkInTime\": \"%s\", \"minutesAway\": %d}", checkInTime.toString(), minutesAway)
                    );

                    logger.warn("Phát hiện hành vi giữ chỗ bất thường: userId={}, minutesAway={}",
                            userId, minutesAway);

                } catch (Exception e) {
                    logger.error("Lỗi khi xử lý log: {}", e.getMessage());
                }
            }

            if (!uncheckedLogs.isEmpty()) {
                logger.info("Đã phát hiện {} trường hợp giữ chỗ bất thường", uncheckedLogs.size());
            }

        } catch (Exception e) {
            logger.error("Lỗi trong detectSeatHoldingBehavior: {}", e.getMessage());
        }
    }
}
