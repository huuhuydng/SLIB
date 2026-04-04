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
 * Giam sat tinh trang thiet bi NFC (HCE Station).
 * Khi thiet bi mat ket noi (khong gui heartbeat trong 5 phut),
 * gui canh bao SYSTEM toi tat ca ADMIN.
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

    // Tap hop cac deviceId da gui canh bao, tranh gui trung lap
    private final Set<String> alertedDevices = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedRate = 120000) // Moi 2 phut
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
                    // Thiet bi vua mat ket noi - gui canh bao
                    alertedDevices.add(device.getDeviceId());

                    String title = "Canh bao thiet bi NFC";
                    String body = String.format(
                            "Tram quet %s (%s) da mat ket noi. Vui long kiem tra thiet bi.",
                            device.getDeviceName(), device.getDeviceId());

                    pushNotificationService.sendToRole(
                            "ADMIN", title, body, NotificationType.SYSTEM, null);

                    log.warn("Tram quet {} ({}) da offline, da gui canh bao",
                            device.getDeviceName(), device.getDeviceId());

                    systemLogService.logJobEvent(LogLevel.WARN,
                            "DeviceHealthMonitor",
                            "Device offline: " + device.getDeviceName()
                                    + " (" + device.getDeviceId() + ")");

                } else if (!isOffline && alertedDevices.contains(device.getDeviceId())) {
                    // Thiet bi hoat dong tro lai - xoa khoi danh sach da canh bao
                    alertedDevices.remove(device.getDeviceId());

                    log.info("Tram quet {} ({}) da hoat dong tro lai",
                            device.getDeviceName(), device.getDeviceId());
                }
            }

        } catch (Exception e) {
            log.error("Loi kiem tra tinh trang thiet bi: {}", e.getMessage());
        }
    }
}
