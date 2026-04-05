package slib.com.example.service.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import slib.com.example.entity.hce.HceDeviceEntity;
import slib.com.example.entity.library.LibrarySetting;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.system.SystemLogEntity.LogLevel;
import slib.com.example.repository.hce.HceDeviceRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import slib.com.example.service.notification.PushNotificationService;

/**
 * Giám sát tình trạng trạm quét NFC.
 * Khi thiết bị mất kết nối quá ngưỡng cấu hình,
 * gửi cảnh báo hệ thống tới quản trị viên.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceHealthMonitor {

    private final HceDeviceRepository hceDeviceRepository;
    private final LibrarySettingService librarySettingService;
    private final PushNotificationService pushNotificationService;
    private final SystemLogService systemLogService;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Value("${slib.hce.offline-threshold-seconds:300}")
    private int offlineThresholdSeconds;

    // Tập hợp các deviceId đã gửi cảnh báo, tránh gửi trùng lặp
    private final Set<String> alertedDevices = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedRate = 120000) // Mỗi 2 phút
    public void checkDeviceHealth() {
        try {
            LibrarySetting settings = librarySettingService.getSettings();
            if (!Boolean.TRUE.equals(settings.getNotifyDeviceAlert())) {
                return;
            }

            LocalDateTime threshold = LocalDateTime.now(VIETNAM_ZONE)
                    .minusSeconds(offlineThresholdSeconds);

            List<HceDeviceEntity> activeDevices = hceDeviceRepository
                    .findByStatus(HceDeviceEntity.DeviceStatus.ACTIVE);

            for (HceDeviceEntity device : activeDevices) {
                boolean isOffline = device.getLastHeartbeat() == null
                        || device.getLastHeartbeat().isBefore(threshold);

                if (isOffline && !alertedDevices.contains(device.getDeviceId())) {
                    // Thiết bị vừa mất kết nối, gửi cảnh báo
                    alertedDevices.add(device.getDeviceId());

                    String title = "Cảnh báo trạm quét NFC";
                    String body = String.format(
                            "Trạm quét %s (%s) đã mất kết nối. Vui lòng kiểm tra thiết bị.",
                            device.getDeviceName(), device.getDeviceId());

                    pushNotificationService.sendToRole(
                            "ADMIN", title, body, NotificationType.SYSTEM, null);

                    log.warn("Tram quet {} ({}) da offline, da gui canh bao",
                            device.getDeviceName(), device.getDeviceId());

                    systemLogService.logJobEvent(LogLevel.WARN,
                            "DeviceHealthMonitor",
                            "Thiết bị ngoại tuyến: " + device.getDeviceName()
                                    + " (" + device.getDeviceId() + ")");

                } else if (!isOffline && alertedDevices.contains(device.getDeviceId())) {
                    // Thiết bị hoạt động trở lại, xóa khỏi danh sách đã cảnh báo
                    alertedDevices.remove(device.getDeviceId());

                    log.info("Tram quet {} ({}) da hoat dong tro lai",
                            device.getDeviceName(), device.getDeviceId());
                }
            }

        } catch (Exception e) {
            log.error("Lỗi kiểm tra tình trạng thiết bị: {}", e.getMessage());
        }
    }
}
