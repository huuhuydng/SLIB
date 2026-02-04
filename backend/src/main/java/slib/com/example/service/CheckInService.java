package slib.com.example.service;

import slib.com.example.dto.hce.CheckInRequest;
import slib.com.example.entity.activity.ActivityLogEntity;
import slib.com.example.entity.hce.AccessLog;
import slib.com.example.entity.users.User;
import slib.com.example.repository.AccessLogRepository;
import slib.com.example.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CheckInService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccessLogRepository accessLogRepository;

    @Autowired
    private ActivityService activityService;

    public Map<String, String> processCheckIn(CheckInRequest request) {
        Map<String, String> response = new HashMap<>();

        try {
            UUID userId = UUID.fromString(request.getToken());

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy User ID: " + userId));

            if (Boolean.FALSE.equals(user.getIsActive())) {
                throw new RuntimeException("Tài khoản đã bị khóa, vui lòng liên hệ quản trị viên để được hỗ trợ.");
            }

            Optional<AccessLog> currentSession = accessLogRepository
                    .checkInUser(user.getId());

            if (currentSession.isPresent()) {
                // CHECK-OUT flow
                AccessLog log = currentSession.get();
                LocalDateTime checkOutTime = LocalDateTime.now();
                log.setCheckOutTime(checkOutTime);
                accessLogRepository.save(log);

                // Calculate duration in minutes
                int durationMinutes = (int) ChronoUnit.MINUTES.between(log.getCheckInTime(), checkOutTime);

                // Log activity for CHECK_OUT
                activityService.logActivity(ActivityLogEntity.builder()
                        .userId(userId)
                        .activityType(ActivityLogEntity.TYPE_CHECK_OUT)
                        .title("Check-out thành công")
                        .description("Bạn đã rời thư viện sau " + formatDuration(durationMinutes))
                        .durationMinutes(durationMinutes)
                        .build());

                response.put("status", "SUCCESS");
                response.put("type", "CHECK_OUT");
                response.put("message", "Tạm biệt, " + user.getFullName());

            } else {
                // CHECK-IN flow
                AccessLog newLog = new AccessLog();
                newLog.setUser(user);
                newLog.setDeviceId(request.getGateId());
                newLog.setCheckInTime(LocalDateTime.now());

                accessLogRepository.save(newLog);

                // Log activity for CHECK_IN
                activityService.logActivity(ActivityLogEntity.builder()
                        .userId(userId)
                        .activityType(ActivityLogEntity.TYPE_CHECK_IN)
                        .title("Check-in thành công")
                        .description("Bạn đã vào thư viện")
                        .build());

                response.put("status", "SUCCESS");
                response.put("type", "CHECK_IN");
                response.put("message", "Xin chào, " + user.getFullName());
            }

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Token không đúng định dạng UUID");
        }

        return response;
    }

    private String formatDuration(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (hours > 0) {
            return hours + " giờ " + mins + " phút";
        }
        return mins + " phút";
    }
}