package slib.com.example.service.hce;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import slib.com.example.dto.hce.HceStationRequest;
import slib.com.example.dto.hce.HceStationResponse;
import slib.com.example.dto.hce.HceStationStatusRequest;
import slib.com.example.entity.hce.HceDeviceEntity;
import slib.com.example.entity.zone_config.AreaEntity;
import slib.com.example.repository.hce.AccessLogRepository;
import slib.com.example.repository.zone_config.AreaRepository;
import slib.com.example.repository.hce.HceDeviceRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HceStationService {

    @Autowired
    private HceDeviceRepository hceDeviceRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private AccessLogRepository accessLogRepository;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Value("${slib.hce.offline-threshold-seconds:300}")
    private int offlineThresholdSeconds;

    /**
     * Lấy danh sách tất cả trạm quét với bộ lọc
     */
    public List<HceStationResponse> getAllStations(String search, String status, String deviceType) {
        HceDeviceEntity.DeviceStatus statusEnum = null;
        HceDeviceEntity.DeviceType typeEnum = null;

        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = HceDeviceEntity.DeviceStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // ignore invalid status filter
            }
        }

        if (deviceType != null && !deviceType.isEmpty()) {
            try {
                typeEnum = HceDeviceEntity.DeviceType.valueOf(deviceType);
            } catch (IllegalArgumentException e) {
                // ignore invalid type filter
            }
        }

        String normalizedSearch = normalizeSearch(search);
        LocalDateTime startOfDay = LocalDateTime.now(VIETNAM_ZONE).toLocalDate().atStartOfDay();
        Map<String, Long> todayScanCounts = accessLogRepository.countTodayScansByDevice(startOfDay).stream()
                .collect(Collectors.toMap(
                        row -> String.valueOf(row[0]),
                        row -> ((Number) row[1]).longValue()));
        Map<String, LocalDateTime> lastAccessTimes = accessLogRepository.findLastAccessTimeByDevice().stream()
                .filter(row -> row[0] != null && row[1] != null)
                .collect(Collectors.toMap(
                        row -> String.valueOf(row[0]),
                        row -> ((java.sql.Timestamp) row[1]).toLocalDateTime()));

        List<HceDeviceEntity> stations = hceDeviceRepository.findAllWithArea().stream()
                .filter(station -> statusEnum == null || station.getStatus() == statusEnum)
                .filter(station -> typeEnum == null || station.getDeviceType() == typeEnum)
                .filter(station -> matchesSearch(station, normalizedSearch))
                .toList();

        return stations.stream()
                .map(station -> toResponse(station, todayScanCounts, lastAccessTimes))
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết một trạm quét theo ID
     */
    public HceStationResponse getStationById(Integer id) {
        HceDeviceEntity station = hceDeviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trạm quét với ID: " + id));
        return toResponse(
                station,
                Map.of(station.getDeviceId(), getTodayScanCount(station.getDeviceId())),
                Map.of(station.getDeviceId(), getLastAccessTime(station.getDeviceId())));
    }

    /**
     * Tạo trạm quét mới
     */
    public HceStationResponse createStation(HceStationRequest request) {
        // Validate required fields
        if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
            throw new RuntimeException("Mã trạm (deviceId) không được để trống");
        }
        if (request.getDeviceName() == null || request.getDeviceName().trim().isEmpty()) {
            throw new RuntimeException("Tên trạm không được để trống");
        }
        if (request.getDeviceType() == null || request.getDeviceType().trim().isEmpty()) {
            throw new RuntimeException("Loại trạm không được để trống");
        }

        // Check unique deviceId
        if (hceDeviceRepository.existsByDeviceId(request.getDeviceId().trim())) {
            throw new RuntimeException("Mã trạm '" + request.getDeviceId() + "' đã tồn tại trong hệ thống");
        }

        // Validate enums
        HceDeviceEntity.DeviceType deviceType;
        try {
            deviceType = HceDeviceEntity.DeviceType.valueOf(request.getDeviceType());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Loại trạm không hợp lệ: " + request.getDeviceType()
                    + ". Giá trị hợp lệ: ENTRY_GATE, EXIT_GATE, SEAT_READER");
        }

        HceDeviceEntity.DeviceStatus status = HceDeviceEntity.DeviceStatus.ACTIVE;
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            try {
                status = HceDeviceEntity.DeviceStatus.valueOf(request.getStatus());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Trạng thái không hợp lệ: " + request.getStatus()
                        + ". Giá trị hợp lệ: ACTIVE, INACTIVE, MAINTENANCE");
            }
        }

        // Validate area if provided
        AreaEntity area = null;
        if (request.getAreaId() != null && request.getAreaId() > 0) {
            area = areaRepository.findById(request.getAreaId())
                    .orElseThrow(
                            () -> new RuntimeException("Khu vực với ID " + request.getAreaId() + " không tồn tại"));
        }

        HceDeviceEntity station = HceDeviceEntity.builder()
                .deviceId(request.getDeviceId().trim())
                .deviceName(request.getDeviceName().trim())
                .location(request.getLocation())
                .deviceType(deviceType)
                .status(status)
                .area(area)
                .build();

        HceDeviceEntity saved = hceDeviceRepository.save(station);
        return toResponse(saved);
    }

    /**
     * Cập nhật trạm quét
     */
    public HceStationResponse updateStation(Integer id, HceStationRequest request) {
        HceDeviceEntity station = hceDeviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trạm quét với ID: " + id));

        // Validate deviceId uniqueness if changed
        if (request.getDeviceId() != null && !request.getDeviceId().trim().isEmpty()
                && !request.getDeviceId().trim().equals(station.getDeviceId())) {
            if (hceDeviceRepository.existsByDeviceId(request.getDeviceId().trim())) {
                throw new RuntimeException("Mã trạm '" + request.getDeviceId() + "' đã tồn tại trong hệ thống");
            }
            station.setDeviceId(request.getDeviceId().trim());
        }

        if (request.getDeviceName() != null && !request.getDeviceName().trim().isEmpty()) {
            station.setDeviceName(request.getDeviceName().trim());
        }

        if (request.getLocation() != null) {
            station.setLocation(request.getLocation());
        }

        if (request.getDeviceType() != null && !request.getDeviceType().isEmpty()) {
            try {
                station.setDeviceType(HceDeviceEntity.DeviceType.valueOf(request.getDeviceType()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Loại trạm không hợp lệ: " + request.getDeviceType());
            }
        }

        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            try {
                station.setStatus(HceDeviceEntity.DeviceStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Trạng thái không hợp lệ: " + request.getStatus());
            }
        }

        if (request.getAreaId() != null && request.getAreaId() > 0) {
            AreaEntity area = areaRepository.findById(request.getAreaId())
                    .orElseThrow(
                            () -> new RuntimeException("Khu vực với ID " + request.getAreaId() + " không tồn tại"));
            station.setArea(area);
        } else if (request.getAreaId() != null && request.getAreaId() == 0) {
            station.setArea(null);
        }

        HceDeviceEntity saved = hceDeviceRepository.save(station);
        return toResponse(saved);
    }

    /**
     * Cập nhật trạng thái trạm quét
     */
    public HceStationResponse updateStationStatus(Integer id, HceStationStatusRequest request) {
        HceDeviceEntity station = hceDeviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trạm quét với ID: " + id));

        if (request.getStatus() == null || request.getStatus().isEmpty()) {
            throw new RuntimeException("Trạng thái không được để trống");
        }

        try {
            station.setStatus(HceDeviceEntity.DeviceStatus.valueOf(request.getStatus()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + request.getStatus()
                    + ". Giá trị hợp lệ: ACTIVE, INACTIVE, MAINTENANCE");
        }

        HceDeviceEntity saved = hceDeviceRepository.save(station);
        return toResponse(saved);
    }

    /**
     * Xóa trạm quét
     */
    public void deleteStation(Integer id) {
        HceDeviceEntity station = hceDeviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trạm quét với ID: " + id));
        hceDeviceRepository.delete(station);
    }

    /**
     * Xử lý heartbeat từ Raspberry Pi
     */
    public void processHeartbeat(String deviceId) {
        HceDeviceEntity station = hceDeviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException(
                        "Trạm quét '" + deviceId + "' chưa được đăng ký trong hệ thống. "
                                + "Vui lòng liên hệ quản trị viên để đăng ký trạm quét."));

        if (station.getStatus() == HceDeviceEntity.DeviceStatus.INACTIVE) {
            throw new RuntimeException("Trạm quét '" + deviceId + "' đang bị vô hiệu hóa (INACTIVE)");
        }

        station.setLastHeartbeat(LocalDateTime.now(VIETNAM_ZONE));
        hceDeviceRepository.save(station);
    }

    /**
     * Validate trạm quét cho check-in flow
     * Được gọi từ CheckInService
     */
    public HceDeviceEntity validateStationForCheckIn(String gateId) {
        HceDeviceEntity station = hceDeviceRepository.findByDeviceId(gateId)
                .orElseThrow(() -> new RuntimeException(
                        "Trạm quét không tồn tại: " + gateId
                                + ". Vui lòng đăng ký trạm quét trên hệ thống quản trị."));

        if (station.getStatus() == HceDeviceEntity.DeviceStatus.INACTIVE) {
            throw new RuntimeException("Trạm quét '" + gateId + "' đang bị vô hiệu hóa. Liên hệ quản trị viên.");
        }

        if (station.getStatus() == HceDeviceEntity.DeviceStatus.MAINTENANCE) {
            throw new RuntimeException("Trạm quét '" + gateId + "' đang trong chế độ bảo trì.");
        }

        // Cập nhật lastHeartbeat (implicit heartbeat khi check-in)
        station.setLastHeartbeat(LocalDateTime.now(VIETNAM_ZONE));
        hceDeviceRepository.save(station);

        return station;
    }

    /**
     * Kiểm tra trạm quét có đang online không
     */
    public boolean isOnline(HceDeviceEntity station) {
        if (station.getLastHeartbeat() == null) {
            return false;
        }
        LocalDateTime threshold = LocalDateTime.now(VIETNAM_ZONE).minusSeconds(offlineThresholdSeconds);
        return station.getLastHeartbeat().isAfter(threshold);
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String normalized = search.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean matchesSearch(HceDeviceEntity station, String normalizedSearch) {
        if (normalizedSearch == null) {
            return true;
        }
        return containsIgnoreCase(station.getDeviceId(), normalizedSearch)
                || containsIgnoreCase(station.getDeviceName(), normalizedSearch)
                || containsIgnoreCase(station.getLocation(), normalizedSearch);
    }

    private boolean containsIgnoreCase(String source, String normalizedSearch) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(normalizedSearch);
    }

    /**
     * Đếm số lượt quét hôm nay theo deviceId
     */
    private long getTodayScanCount(String deviceId) {
        try {
            LocalDateTime startOfDay = LocalDateTime.now(VIETNAM_ZONE).toLocalDate().atStartOfDay();
            List<slib.com.example.entity.hce.AccessLog> logs = accessLogRepository.findLogsFromStartOfDay(startOfDay);
            return logs.stream()
                    .filter(log -> deviceId.equals(log.getDeviceId()))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Lấy thời gian truy cập cuối cùng theo deviceId
     */
    private LocalDateTime getLastAccessTime(String deviceId) {
        try {
            List<slib.com.example.entity.hce.AccessLog> logs = accessLogRepository.findAllOrderByCheckInTimeDesc();
            return logs.stream()
                    .filter(log -> deviceId.equals(log.getDeviceId()))
                    .map(log -> log.getCheckOutTime() != null ? log.getCheckOutTime() : log.getCheckInTime())
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convert entity to response DTO
     */
    private HceStationResponse toResponse(HceDeviceEntity station,
            Map<String, Long> todayScanCounts,
            Map<String, LocalDateTime> lastAccessTimes) {
        return HceStationResponse.builder()
                .id(station.getId())
                .deviceId(station.getDeviceId())
                .deviceName(station.getDeviceName())
                .location(station.getLocation())
                .deviceType(station.getDeviceType() != null ? station.getDeviceType().name() : null)
                .status(station.getStatus() != null ? station.getStatus().name() : null)
                .lastHeartbeat(station.getLastHeartbeat())
                .online(isOnline(station))
                .areaId(station.getArea() != null ? station.getArea().getAreaId() : null)
                .areaName(station.getArea() != null ? station.getArea().getAreaName() : null)
                .createdAt(station.getCreatedAt())
                .updatedAt(station.getUpdatedAt())
                .todayScanCount(todayScanCounts.getOrDefault(station.getDeviceId(), 0L))
                .lastAccessTime(lastAccessTimes.computeIfAbsent(station.getDeviceId(), this::getLastAccessTime))
                .build();
    }
}
