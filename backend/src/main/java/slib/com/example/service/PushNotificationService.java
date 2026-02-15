package slib.com.example.service;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.notification.NotificationEntity;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.users.User;
import slib.com.example.repository.NotificationRepository;
import slib.com.example.repository.UserRepository;

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
            return null;
        }
    }

    /**
     * Send push notification to a user by userId
     * Also saves notification to database
     */
    @Transactional
    public void sendToUser(UUID userId, String title, String body, NotificationType type, UUID referenceId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found: {}", userId);
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

        sendToDeviceWithBadge(user.getNotiDevice(), title, body, data, badgeCount);
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
     * Check if notification should be sent based on user settings
     */
    private boolean shouldSendNotification(User user, NotificationType type) {
        return switch (type) {
            case BOOKING -> user.getNotifyBooking() == null || user.getNotifyBooking();
            case REMINDER -> user.getNotifyReminder() == null || user.getNotifyReminder();
            case NEWS -> user.getNotifyNews() == null || user.getNotifyNews();
            case VIOLATION, SYSTEM, SUPPORT_REQUEST -> true; // Always send violations, system, and support request
                                                             // notifications
        };
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
