package slib.com.example.service;

import slib.com.example.dto.hce.AccessLogDTO;
import slib.com.example.dto.hce.AccessLogStatsDTO;
import slib.com.example.dto.hce.CheckInRequest;
import slib.com.example.entity.activity.ActivityLogEntity;
import slib.com.example.entity.hce.AccessLog;
import slib.com.example.entity.users.User;
import slib.com.example.repository.AccessLogRepository;
import slib.com.example.repository.UserRepository;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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

                // Broadcast WebSocket notification for real-time update
                Map<String, Object> wsMessage = new HashMap<>();
                wsMessage.put("type", "CHECK_OUT");
                wsMessage.put("userId", userId.toString());
                wsMessage.put("userName", user.getFullName());
                wsMessage.put("userCode", user.getUserCode());
                wsMessage.put("timestamp", checkOutTime.toString());
                messagingTemplate.convertAndSend("/topic/access-logs", wsMessage);

                response.put("status", "SUCCESS");
                response.put("type", "CHECK_OUT");
                response.put("message", "Tạm biệt, " + user.getFullName());

            } else {
                // CHECK-IN flow
                AccessLog newLog = new AccessLog();
                newLog.setUser(user);
                newLog.setDeviceId(request.getGateId());
                LocalDateTime checkInTime = LocalDateTime.now();
                newLog.setCheckInTime(checkInTime);

                accessLogRepository.save(newLog);

                // Log activity for CHECK_IN
                activityService.logActivity(ActivityLogEntity.builder()
                        .userId(userId)
                        .activityType(ActivityLogEntity.TYPE_CHECK_IN)
                        .title("Check-in thành công")
                        .description("Bạn đã vào thư viện")
                        .build());

                // Broadcast WebSocket notification for real-time update
                Map<String, Object> wsMessage = new HashMap<>();
                wsMessage.put("type", "CHECK_IN");
                wsMessage.put("userId", userId.toString());
                wsMessage.put("userName", user.getFullName());
                wsMessage.put("userCode", user.getUserCode());
                wsMessage.put("timestamp", checkInTime.toString());
                messagingTemplate.convertAndSend("/topic/access-logs", wsMessage);

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

    /**
     * Tự động check-out lúc 21:00 cho những người chưa check-out
     */
    private void autoCheckOutAfter5PM() {
        LocalDateTime now = LocalDateTime.now();
        
        // Tìm tất cả logs chưa checkout
        List<AccessLog> uncheckedOutLogs = accessLogRepository.findAllOrderByCheckInTimeDesc()
                .stream()
                .filter(log -> log.getCheckOutTime() == null)
                .collect(Collectors.toList());
        
        for (AccessLog log : uncheckedOutLogs) {
            LocalDateTime checkInTime = log.getCheckInTime();
            // Tạo thời gian 21:00 của ngày check-in
            LocalDateTime autoCheckOutTime = checkInTime.toLocalDate().atTime(21, 0);
            
            // Nếu hiện tại đã qua 21:00 của ngày check-in đó
            if (now.isAfter(autoCheckOutTime)) {
                log.setCheckOutTime(autoCheckOutTime);
                accessLogRepository.save(log);
                
                // Calculate duration in minutes
                int durationMinutes = (int) ChronoUnit.MINUTES.between(log.getCheckInTime(), autoCheckOutTime);
                
                // Log activity for auto CHECK_OUT
                activityService.logActivity(ActivityLogEntity.builder()
                        .userId(log.getUserId())
                        .activityType(ActivityLogEntity.TYPE_CHECK_OUT)
                        .title("Check-out tự động")
                        .description("Hệ thống tự động check-out lúc 21:00 sau " + formatDuration(durationMinutes))
                        .durationMinutes(durationMinutes)
                        .build());
            }
        }
    }

    /**
     * Lấy danh sách tất cả access logs
     */
    public List<AccessLogDTO> getAllAccessLogs() {
        // Auto check-out trước khi lấy danh sách
        autoCheckOutAfter5PM();
        
        List<AccessLog> logs = accessLogRepository.findAllOrderByCheckInTimeDesc();
        return logs.stream()
                .flatMap(this::convertToSeparateDTOs)
                .sorted((a, b) -> {
                    // Sort theo thời gian thực tế của từng hành động (mới nhất trên cùng)
                    LocalDateTime timeA = "CHECK_OUT".equals(a.getAction()) ? a.getCheckOutTime() : a.getCheckInTime();
                    LocalDateTime timeB = "CHECK_OUT".equals(b.getAction()) ? b.getCheckOutTime() : b.getCheckInTime();
                    return timeB.compareTo(timeA); // DESC - mới nhất trước
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách access logs hôm nay
     */
    public List<AccessLogDTO> getTodayAccessLogs() {
        // Auto check-out trước khi lấy danh sách
        autoCheckOutAfter5PM();
        
        List<AccessLog> logs = accessLogRepository.findTodayLogs();
        return logs.stream()
                .flatMap(this::convertToSeparateDTOs)
                .sorted((a, b) -> {
                    // Sort theo thời gian thực tế của từng hành động (mới nhất trên cùng)
                    LocalDateTime timeA = "CHECK_OUT".equals(a.getAction()) ? a.getCheckOutTime() : a.getCheckInTime();
                    LocalDateTime timeB = "CHECK_OUT".equals(b.getAction()) ? b.getCheckOutTime() : b.getCheckInTime();
                    return timeB.compareTo(timeA); // DESC - mới nhất trước
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy thống kê access logs hôm nay
     */
    public AccessLogStatsDTO getTodayStats() {
        List<AccessLog> todayLogs = accessLogRepository.findTodayLogs();
        
        long checkIns = todayLogs.size();
        long checkOuts = todayLogs.stream()
                .filter(log -> log.getCheckOutTime() != null)
                .count();
        long currentlyInLibrary = checkIns - checkOuts;

        return AccessLogStatsDTO.builder()
                .totalCheckInsToday(checkIns)
                .totalCheckOutsToday(checkOuts)
                .currentlyInLibrary(currentlyInLibrary)
                .build();
    }

    /**
     * Lấy danh sách access logs theo khoảng thời gian
     */
    public List<AccessLogDTO> getAccessLogsByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        // Auto check-out trước khi lấy danh sách
        autoCheckOutAfter5PM();
        
        List<AccessLog> logs = accessLogRepository.findLogsByDateRange(startDate, endDate);
        return logs.stream()
                .flatMap(this::convertToSeparateDTOs)
                .sorted((a, b) -> {
                    // Sort theo thời gian thực tế của từng hành động (mới nhất trên cùng)
                    LocalDateTime timeA = "CHECK_OUT".equals(a.getAction()) ? a.getCheckOutTime() : a.getCheckInTime();
                    LocalDateTime timeB = "CHECK_OUT".equals(b.getAction()) ? b.getCheckOutTime() : b.getCheckInTime();
                    return timeB.compareTo(timeA); // DESC - mới nhất trước
                })
                .collect(Collectors.toList());
    }

    /**
     * Convert AccessLog entity to separate DTOs for CHECK_IN and CHECK_OUT
     * Mỗi access log sẽ tạo ra 2 DTO riêng biệt nếu đã có checkout
     * Thứ tự sẽ được sort lại theo thời gian thực tế ở caller
     */
    private java.util.stream.Stream<AccessLogDTO> convertToSeparateDTOs(AccessLog log) {
        User user = log.getUser();
        String userName = user != null ? user.getFullName() : "Unknown";
        String userCode = user != null ? user.getUserCode() : "N/A";
        
        List<AccessLogDTO> dtos = new ArrayList<>();
        
        // Tạo DTO cho CHECK_IN
        dtos.add(AccessLogDTO.builder()
                .logId(log.getLogId())
                .userId(log.getUserId())
                .userName(userName)
                .userCode(userCode)
                .deviceId(log.getDeviceId())
                .checkInTime(log.getCheckInTime())
                .checkOutTime(null)
                .action("CHECK_IN")
                .build());
        
        // Nếu đã checkout, tạo thêm DTO cho CHECK_OUT
        if (log.getCheckOutTime() != null) {
            dtos.add(AccessLogDTO.builder()
                    .logId(log.getLogId())
                    .userId(log.getUserId())
                    .userName(userName)
                    .userCode(userCode)
                    .deviceId(log.getDeviceId())
                    .checkInTime(log.getCheckInTime())
                    .checkOutTime(log.getCheckOutTime())
                    .action("CHECK_OUT")
                    .build());
        }
        
        return dtos.stream();
    }

    /**
     * Convert AccessLog entity to DTO
     */
    private AccessLogDTO convertToDTO(AccessLog log) {
        User user = log.getUser();
        return AccessLogDTO.builder()
                .logId(log.getLogId())
                .userId(log.getUserId())
                .userName(user != null ? user.getFullName() : "Unknown")
                .userCode(user != null ? user.getUserCode() : "N/A")
                .deviceId(log.getDeviceId())
                .checkInTime(log.getCheckInTime())
                .checkOutTime(log.getCheckOutTime())
                .action(log.getCheckOutTime() != null ? "CHECK_OUT" : "CHECK_IN")
                .build();
    }

    /**
     * Xuất báo cáo Excel
     */
    public byte[] exportAccessLogsToExcel(java.time.LocalDate startDate, java.time.LocalDate endDate) throws IOException {
        // Auto check-out trước khi xuất
        autoCheckOutAfter5PM();
        
        // Lấy danh sách logs theo khoảng thời gian
        List<AccessLog> logs;
        if (startDate != null && endDate != null) {
            logs = accessLogRepository.findLogsByDateRange(startDate, endDate);
        } else {
            // Nếu không có date filter, lấy 3 tháng gần đây
            java.time.LocalDate end = java.time.LocalDate.now();
            java.time.LocalDate start = end.minusMonths(3);
            logs = accessLogRepository.findLogsByDateRange(start, end);
        }
        
        // Tạo workbook Excel
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bao cao Check-in Check-out");
            
            // Tạo style cho header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            // Tạo style cho data cells
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            
            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"STT", "Tên sinh viên", "Mã số sinh viên", "Gate ID", "Thời gian Check-in", "Thời gian Check-out"};
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Format datetime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            
            // Điền data
            int rowNum = 1;
            for (AccessLog log : logs) {
                Row row = sheet.createRow(rowNum++);
                User user = log.getUser();
                
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowNum - 1);
                cell0.setCellStyle(dataStyle);
                
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(user != null ? user.getFullName() : "Unknown");
                cell1.setCellStyle(dataStyle);
                
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(user != null ? user.getUserCode() : "N/A");
                cell2.setCellStyle(dataStyle);
                
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(log.getDeviceId() != null ? log.getDeviceId() : "-");
                cell3.setCellStyle(dataStyle);
                
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(log.getCheckInTime() != null ? log.getCheckInTime().format(formatter) : "-");
                cell4.setCellStyle(dataStyle);
                
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(log.getCheckOutTime() != null ? log.getCheckOutTime().format(formatter) : "-");
                cell5.setCellStyle(dataStyle);
            }
            
            // Set column widths manually (avoid autoSizeColumn font issues in Docker)
            sheet.setColumnWidth(0, 2000);   // STT
            sheet.setColumnWidth(1, 8000);   // Tên sinh viên
            sheet.setColumnWidth(2, 5000);   // Mã số sinh viên
            sheet.setColumnWidth(3, 4000);   // Gate ID
            sheet.setColumnWidth(4, 6000);   // Thời gian Check-in
            sheet.setColumnWidth(5, 6000);   // Thời gian Check-out
            
            workbook.write(out);
            return out.toByteArray();
        }
    }
}