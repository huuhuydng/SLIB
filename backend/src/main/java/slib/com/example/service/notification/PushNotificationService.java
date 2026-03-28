package slib.com.example.service.notification;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.entity.library.LibrarySetting;
import slib.com.example.entity.notification.NotificationEntity;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.users.User;
import slib.com.example.entity.users.UserSetting;
import slib.com.example.repository.notification.NotificationRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.repository.users.UserSettingRepository;
import slib.com.example.service.system.LibrarySettingService;
import slib.com.example.service.system.SystemLogService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for sending push notifications via Firebase Cloud Messaging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SystemLogService systemLogService;
    private final LibrarySettingService librarySettingService;

    /**
     * Send push notification to a specific device
     * 
     * @param fcmToken Firebase Cloud Messaging token
     * @param title    Notification title
     * @param body     Notification body
     * @param data     Additional data payload
     * @return Message ID if successful, null otherwise
     */
    public String sendToDevice(String fcmToken, String title, String body, Map<String, String> data) {
        return sendToDeviceWithBadge(fcmToken, title, body, data, 1);
    }

    /**
     * Send push notification to a specific device with custom badge count
     */
    public String sendToDeviceWithBadge(String fcmToken, String title, String body, Map<String, String> data,
            int badgeCount) {
        if (firebaseMessaging == null) {
            log.warn("Firebase Messaging not initialized, skipping notification");
            return null;
        }

        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("FCM token is null or empty, skipping notification");
            return null;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data != null ? data : new HashMap<>())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .setBadge(badgeCount)
                                    .build())
                            .build())
                    .build();

            String response = firebaseMessaging.send(message);
            log.info("Notification sent successfully: {}", response);
            return response;
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to device: {}", e.getMessage());
            systemLogService.logIntegrationError("PushNotificationService", "Failed to send push notification", e.getMessage());
            return null;
        }
    }

    /**
     * Send data-only push notification (no notification payload)
     * Mobile app controls notification display entirely
     * Used for CHAT_MESSAGE to avoid Android auto-displaying duplicate
     * notifications
     */
    public String sendDataOnly(String fcmToken, String title, String body, Map<String, String> data,
            int badgeCount) {
        if (firebaseMessaging == null) {
            log.warn("Firebase Messaging not initialized, skipping notification");
            return null;
        }

        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("FCM token is null or empty, skipping notification");
            return null;
        }

        try {
            // Include title and body in data so mobile can show notification
            Map<String, String> fullData = new HashMap<>(data != null ? data : new HashMap<>());
            fullData.put("title", title);
            fullData.put("body", body);

            Message message = Message.builder()
                    .setToken(fcmToken)
                    .putAllData(fullData)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setContentAvailable(true)
                                    .setSound("default")
                                    .setBadge(badgeCount)
                                    .build())
                            .build())
                    .build();

            String response = firebaseMessaging.send(message);
            log.info("Data-only notification sent successfully: {}", response);
            return response;
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send data-only notification: {}", e.getMessage());
            systemLogService.logIntegrationError("PushNotificationService", "Failed to send data-only push", e.getMessage());
            return null;
        }
    }

    /**
     * Send push notification to a user by userId
     * Also saves notification to database
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void sendToUser(UUID userId, String title, String body, NotificationType type, UUID referenceId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found: {}", userId);
            return;
        }

        // Check global notification settings (admin config)
        if (!isNotificationEnabledGlobally(type)) {
            log.info("Loai thong bao {} da bi tat boi admin trong cau hinh he thong", type);
            return;
        }

        // Check user notification settings
        if (!shouldSendNotification(user, type)) {
            log.info("User {} has disabled notifications for type {}", userId, type);
            return;
        }

        // Save notification to database
        NotificationEntity notification = NotificationEntity.builder()
                .user(user)
                .title(title)
                .content(body)
                .notificationType(type)
                .referenceType(type.name())
                .referenceId(referenceId)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        // Get unread count for badge (after saving new notification)
        int badgeCount = (int) notificationRepository.countUnreadByUserId(userId);

        // Send push notification to device
        Map<String, String> data = new HashMap<>();
        data.put("type", type.name());
        data.put("notificationId", notification.getId().toString());
        data.put("badgeCount", String.valueOf(badgeCount));
        if (referenceId != null) {
            data.put("referenceId", referenceId.toString());
        }

        // CHAT_MESSAGE: gửi data-only để tránh Android tự hiện notification (duplicate)
        // Các loại khác: gửi có notification payload để Android hiện tự động
        if (type == NotificationType.CHAT_MESSAGE) {
            sendDataOnly(user.getNotiDevice(), title, body, data, badgeCount);
        } else {
            sendToDeviceWithBadge(user.getNotiDevice(), title, body, data, badgeCount);
        }

        // Broadcast qua WebSocket → mobile nhận real-time (0ms delay)
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("id", notification.getId().toString());
        wsPayload.put("title", title);
        wsPayload.put("content", body);
        wsPayload.put("notificationType", type.name());
        wsPayload.put("referenceId", referenceId != null ? referenceId.toString() : null);
        wsPayload.put("isRead", false);
        wsPayload.put("unreadCount", badgeCount);
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, wsPayload);
    }

    /**
     * Send push notification to all users (for news, announcements)
     */
    @Transactional
    public void sendToAll(String title, String body, NotificationType type, UUID referenceId) {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            if (user.getNotiDevice() != null && !user.getNotiDevice().isEmpty()) {
                sendToUser(user.getId(), title, body, type, referenceId);
            }
        }

        log.info("Sent notification to {} users", users.size());
    }

    /**
     * Send notification only to users with specific role
     */
    @Transactional
    public void sendToRole(String role, String title, String body, NotificationType type, UUID referenceId) {
        // Find users with specific role
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && role.equalsIgnoreCase(u.getRole().name()))
                .filter(u -> u.getNotiDevice() != null && !u.getNotiDevice().isEmpty())
                .toList();

        for (User user : users) {
            sendToUser(user.getId(), title, body, type, referenceId);
        }

        log.info("Sent notification to {} users with role {}", users.size(), role);
    }

    /**
     * Check if notification should be sent based on user settings.
     * Kiểm tra cấu hình "Thông báo đẩy" trong UserSetting.
     * Khi user tắt toggle này, chặn TẤT CẢ notification.
     */
    private boolean shouldSendNotification(User user, NotificationType type) {
        UserSetting setting = userSettingRepository.findById(user.getId()).orElse(null);
        if (setting != null && Boolean.FALSE.equals(setting.getIsBookingRemindEnabled())) {
            return false;
        }
        return true;
    }

    /**
     * Kiem tra loai thong bao co duoc bat o cap do he thong (admin config) hay khong.
     * Cac loai SYSTEM, NEWS, CHAT_MESSAGE, SUPPORT_REQUEST luon duoc gui.
     */
    private boolean isNotificationEnabledGlobally(NotificationType type) {
        try {
            LibrarySetting settings = librarySettingService.getSettings();
            return switch (type) {
                case BOOKING -> Boolean.TRUE.equals(settings.getNotifyBookingSuccess());
                case REMINDER -> Boolean.TRUE.equals(settings.getNotifyCheckinReminder());
                case VIOLATION -> Boolean.TRUE.equals(settings.getNotifyViolation());
                case SYSTEM, NEWS, CHAT_MESSAGE, SUPPORT_REQUEST -> true;
            };
        } catch (Exception e) {
            log.warn("Khong the kiem tra cau hinh thong bao he thong, cho phep gui mac dinh: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Get notifications for a user
     */
    public List<NotificationEntity> getUserNotifications(UUID userId, int limit) {
        if (limit > 0) {
            return notificationRepository.findByUserIdWithLimit(userId, limit);
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get unread notification count
     */
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
