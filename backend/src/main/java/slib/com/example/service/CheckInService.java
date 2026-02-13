package slib.com.example.service;

import slib.com.example.dto.hce.CheckInRequest;
import slib.com.example.entity.activity.ActivityLogEntity;
import slib.com.example.entity.hce.AccessLog;
import slib.com.example.entity.users.User;
import slib.com.example.repository.AccessLogRepository;
import slib.com.example.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.List;

@Service
public class CheckInService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccessLogRepository accessLogRepository;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public Map<String, String> processCheckIn(CheckInRequest request) {
        Map<String, String> response = new HashMap<>();

        try {
            UUID userId = UUID.fromString(request.getToken());

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy User ID: " + userId));

            if (Boolean.FALSE.equals(user.getIsActive())) {
                throw new RuntimeException("Tài khoản đã bị khóa, vui lòng liên hệ quản trị viên để được hỗ trợ.");
            }

            LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);

            Optional<AccessLog> currentSession = accessLogRepository.checkInUser(user.getId());

            // Chuẩn bị dữ liệu chung cho Real-time WebSocket
            response.put("fullName", user.getFullName());
            response.put("userCode", user.getUserCode());   
            response.put("deviceId", request.getGateId());      
            response.put("time", now.toString()); // Thời gian thực để Frontend format

            if (currentSession.isPresent()) {
                // --- LUỒNG CHECK-OUT ---
                AccessLog log = currentSession.get();
                log.setCheckOutTime(now);
                accessLogRepository.save(log);

                // Tính toán thời gian sử dụng
                int durationMinutes = (int) ChronoUnit.MINUTES.between(log.getCheckInTime(), now);

                // Ghi log hoạt động
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
                response.put("checkInTime", log.getCheckInTime().toString());
                response.put("checkOutTime", now.toString());

            } else {
                // --- LUỒNG CHECK-IN ---
                AccessLog newLog = new AccessLog();
                newLog.setUser(user);
                newLog.setDeviceId(request.getGateId());
                newLog.setCheckInTime(now);

                accessLogRepository.save(newLog);

                // Ghi log hoạt động
                activityService.logActivity(ActivityLogEntity.builder()
                        .userId(userId)
                        .activityType(ActivityLogEntity.TYPE_CHECK_IN)
                        .title("Check-in thành công")
                        .description("Bạn đã vào thư viện tại " + request.getGateId())
                        .build());

                response.put("status", "SUCCESS");
                response.put("type", "CHECK_IN");
                response.put("message", "Xin chào, " + user.getFullName());
                response.put("checkInTime", now.toString());
            }

        // Gửi dữ liệu qua WebSocket
        messagingTemplate.convertAndSend("/topic/access-logs", response);

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Token không đúng định dạng UUID");
        } catch (Exception e) {
            throw new RuntimeException("Lỗi hệ thống: " + e.getMessage());
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

   public List<Map<String, Object>> getLatest10Logs() {
    List<Map<String, Object>> allActions = new ArrayList<>();
    
    // 1. Xác định mốc 00:00:00 của ngày hiện tại theo múi giờ Việt Nam
    LocalDateTime startOfDay = LocalDateTime.now(VIETNAM_ZONE)
                                            .toLocalDate()
                                            .atStartOfDay();

    // 2. Lấy danh sách logs phát sinh từ đầu ngày (Tối ưu hơn dùng findAll)
    List<AccessLog> logsInDay = accessLogRepository.findLogsFromStartOfDay(startOfDay);

    // 3. Duyệt qua từng bản ghi để tách thành các hành động riêng biệt
    for (AccessLog log : logsInDay) {
        // Luôn có hành động CHECK-IN
        Map<String, Object> inAction = new HashMap<>();
        inAction.put("fullName", log.getUser().getFullName());
        inAction.put("userCode", log.getUser().getUserCode());
        inAction.put("deviceId", log.getDeviceId());
        inAction.put("type", "CHECK_IN");
        inAction.put("time", log.getCheckInTime().toString());
        inAction.put("sortTime", log.getCheckInTime()); // Dùng để sắp xếp
        allActions.add(inAction);

        // Chỉ thêm hành động CHECK-OUT nếu bản ghi đã có giờ ra
        if (log.getCheckOutTime() != null) {
            Map<String, Object> outAction = new HashMap<>();
            outAction.put("fullName", log.getUser().getFullName());
            outAction.put("userCode", log.getUser().getUserCode());
            outAction.put("deviceId", log.getDeviceId());
            outAction.put("type", "CHECK_OUT");
            outAction.put("time", log.getCheckOutTime().toString());
            outAction.put("sortTime", log.getCheckOutTime()); // Dùng để sắp xếp
            allActions.add(outAction);
        }
    }

    // 4. Sắp xếp tất cả hành động theo thời gian mới nhất và lấy tối đa 10 mục
    return allActions.stream()
            .sorted((a, b) -> ((LocalDateTime)b.get("sortTime")).compareTo((LocalDateTime)a.get("sortTime")))
            .limit(10)
            .peek(action -> action.remove("sortTime")) // Xóa dữ liệu trung gian trước khi trả về
            .collect(Collectors.toList());
    }
}