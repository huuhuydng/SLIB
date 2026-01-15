package slib.com.example.service;
import slib.com.example.dto.CheckInRequest;
import slib.com.example.entity.hce.AccessLog;
import slib.com.example.entity.users.User;
import slib.com.example.repository.AccessLogRepository;
import slib.com.example.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    public Map<String, String> processCheckIn(CheckInRequest request) {
        Map<String, String> response = new HashMap<>();

        try {
            UUID userId = UUID.fromString(request.getToken());

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy User ID: " + userId));

            if (Boolean.FALSE.equals(user.getIsActive())) {
                throw new RuntimeException("Tài khoản đã bị khóa");
            }


            Optional<AccessLog> currentSession = accessLogRepository
                    .checkInUser(user.getId());

            if (currentSession.isPresent()) {
                AccessLog log = currentSession.get();
                log.setCheckOutTime(LocalDateTime.now());
                accessLogRepository.save(log);

                response.put("status", "SUCCESS");
                response.put("type", "CHECK_OUT");
                response.put("message", "Tạm biệt, " + user.getFullName());
            
            } else {
                AccessLog newLog = new AccessLog();
                newLog.setUser(user);
                newLog.setDeviceId(request.getGateId());
                newLog.setCheckInTime(LocalDateTime.now());

                accessLogRepository.save(newLog);

                response.put("status", "SUCCESS");
                response.put("type", "CHECK_IN");
                response.put("message", "Xin chào, " + user.getFullName());
            }

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Token không đúng định dạng UUID");
        }

        return response;
    }
}