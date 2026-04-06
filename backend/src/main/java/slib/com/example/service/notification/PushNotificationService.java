package slib.com.example.service.notification;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import slib.com.example.dto.notification.NotificationDTO;
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

import java.time.Duration;
import java.time.LocalDateTime;
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
    public static final String DELIVERY_KEY_CHECKIN_REMINDER = "CHECKIN_REMINDER";
    public static final String DELIVERY_KEY_TIME_EXPIRY = "TIME_EXPIRY";

    private final FirebaseMessaging firebaseMessaging;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SystemLogService systemLogService;
    private final LibrarySettingService librarySettingService;
    private final PlatformTransactionManager transactionManager;

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
    public void sendToUser(UUID userId, String title, String body, NotificationType type, UUID referenceId) {
        sendToUser(userId, title, body, type, referenceId, type != null ? type.name() : null, null, null);
    }

    public void sendToUser(UUID userId, String title, String body, NotificationType type, UUID referenceId,
            String deliveryKey) {
        sendToUser(userId, title, body, type, referenceId, type != null ? type.name() : null, null, deliveryKey);
    }

    public void sendToUser(UUID userId, String title, String body, NotificationType type, UUID referenceId,
            String referenceType, String category) {
        sendToUser(userId, title, body, type, referenceId, referenceType, category, null);
    }

    public void sendToUser(UUID userId, String title, String body, NotificationType type, UUID referenceId,
            String referenceType, String category, String deliveryKey) {
        NotificationRequest request = new NotificationRequest(userId, title, body, type, referenceId, referenceType,
                category, deliveryKey);

        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchNotification(request);
                }
            });
            return;
        }

        dispatchNotification(request);
    }

    private void dispatchNotification(NotificationRequest request) {
        NotificationDispatchResult result = persistNotification(request);
        if (result == null) {
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", result.type().name());
        data.put("category", result.category());
        data.put("categoryLabel", result.categoryLabel());
        data.put("notificationId", result.notificationId().toString());
        data.put("badgeCount", String.valueOf(result.badgeCount()));
        if (result.referenceType() != null) {
            data.put("referenceType", result.referenceType());
        }
        if (result.referenceId() != null) {
            data.put("referenceId", result.referenceId().toString());
        }

        if (result.type() == NotificationType.CHAT_MESSAGE) {
            sendDataOnly(result.fcmToken(), result.title(), result.body(), data, result.badgeCount());
        } else {
            sendToDeviceWithBadge(result.fcmToken(), result.title(), result.body(), data, result.badgeCount());
        }

        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("id", result.notificationId().toString());
        wsPayload.put("title", result.title());
        wsPayload.put("content", result.body());
        wsPayload.put("notificationType", result.type().name());
        wsPayload.put("category", result.category());
        wsPayload.put("categoryLabel", result.categoryLabel());
        wsPayload.put("referenceType", result.referenceType());
        wsPayload.put("referenceId", result.referenceId() != null ? result.referenceId().toString() : null);
        wsPayload.put("isRead", false);
        wsPayload.put("unreadCount", result.badgeCount());
        messagingTemplate.convertAndSend("/topic/notifications/" + result.userId(), wsPayload);
    }

    private NotificationDispatchResult persistNotification(NotificationRequest request) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return template.execute(status -> {
            User user = userRepository.findById(request.userId()).orElse(null);
            if (user == null) {
                log.warn("User not found: {}", request.userId());
                return null;
            }

            if (!isNotificationEnabledGlobally(request.type(), request.deliveryKey())) {
                log.info("Loai thong bao {} da bi tat boi admin trong cau hinh he thong", request.type());
                return null;
            }

            if (!shouldSendNotification(user, request.type())) {
                log.info("User {} has disabled notifications for type {}", request.userId(), request.type());
                return null;
            }

            if (shouldSuppressDuplicate(request)) {
                log.info(
                        "Skip duplicate notification for user={} type={} referenceType={} referenceId={}",
                        request.userId(),
                        request.type(),
                        request.referenceType(),
                        request.referenceId());
                return null;
            }

            NotificationEntity notification = NotificationEntity.builder()
                    .user(user)
                    .title(request.title())
                    .content(request.body())
                    .notificationType(request.type())
                    .referenceType(request.referenceType())
                    .referenceId(request.referenceId())
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);

            int badgeCount = (int) notificationRepository.countUnreadByUserId(request.userId());
            String resolvedCategory = request.category() != null && !request.category().isBlank()
                    ? request.category()
                    : resolveCategory(request.type(), request.title(), request.body());
            String categoryLabel = resolveCategoryLabel(resolvedCategory);

            return new NotificationDispatchResult(
                    notification.getId(),
                    request.userId(),
                    request.title(),
                    request.body(),
                    request.type(),
                    request.referenceId(),
                    request.referenceType(),
                    resolvedCategory,
                    categoryLabel,
                    badgeCount,
                    user.getNotiDevice());
        });
    }

    private boolean shouldSuppressDuplicate(NotificationRequest request) {
        Duration cooldown = resolveDuplicateCooldown(request);
        if (cooldown == null || cooldown.isZero() || cooldown.isNegative()) {
            return false;
        }

        LocalDateTime since = LocalDateTime.now().minus(cooldown);
        String referenceType = request.referenceType();
        UUID referenceId = request.referenceId();

        if (referenceType == null && referenceId == null) {
            return notificationRepository.existsRecentDuplicateWithoutReference(
                    request.userId(),
                    request.type(),
                    request.title(),
                    request.body(),
                    since);
        }

        if (referenceType != null && referenceId == null) {
            return notificationRepository.existsRecentDuplicateWithReferenceType(
                    request.userId(),
                    request.type(),
                    request.title(),
                    request.body(),
                    referenceType,
                    since);
        }

        if (referenceType == null) {
            return notificationRepository.existsRecentDuplicateWithReferenceId(
                    request.userId(),
                    request.type(),
                    request.title(),
                    request.body(),
                    referenceId,
                    since);
        }

        return notificationRepository.existsRecentDuplicateWithReference(
                request.userId(),
                request.type(),
                request.title(),
                request.body(),
                referenceType,
                referenceId,
                since);
    }

    private Duration resolveDuplicateCooldown(NotificationRequest request) {
        if (request.type() == null || request.type() == NotificationType.CHAT_MESSAGE) {
            return null;
        }

        return switch (request.type()) {
            case SYSTEM -> Duration.ofMinutes(2);
            case NEWS -> Duration.ofMinutes(5);
            case BOOKING, REMINDER, VIOLATION, VIOLATION_REPORT, REPUTATION, SUPPORT_REQUEST, COMPLAINT,
                    SEAT_STATUS_REPORT -> Duration.ofMinutes(1);
            case CHAT_MESSAGE -> null;
        };
    }

    private record NotificationRequest(
            UUID userId,
            String title,
            String body,
            NotificationType type,
            UUID referenceId,
            String referenceType,
            String category,
            String deliveryKey) {
    }

    private record NotificationDispatchResult(
            UUID notificationId,
            UUID userId,
            String title,
            String body,
            NotificationType type,
            UUID referenceId,
            String referenceType,
            String category,
            String categoryLabel,
            int badgeCount,
            String fcmToken) {
    }

    /**
     * Send push notification to all users (for news, announcements)
     */
    public void sendToAll(String title, String body, NotificationType type, UUID referenceId) {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            sendToUser(user.getId(), title, body, type, referenceId);
        }

        log.info("Sent notification to {} users", users.size());
    }

    /**
     * Send notification only to users with specific role
     */
    public void sendToRole(String role, String title, String body, NotificationType type, UUID referenceId) {
        // Find users with specific role
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && role.equalsIgnoreCase(u.getRole().name()))
                .toList();

        for (User user : users) {
            sendToUser(user.getId(), title, body, type, referenceId);
        }

        log.info("Sent notification to {} users with role {}", users.size(), role);
    }

    /**
     * Send notification to all patron users (STUDENT + TEACHER).
     */
    public void sendToPatrons(String title, String body, NotificationType type, UUID referenceId) {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && u.getRole().isPatron())
                .toList();

        for (User user : users) {
            sendToUser(user.getId(), title, body, type, referenceId);
        }

        log.info("Sent notification to {} patron users", users.size());
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

        return switch (type) {
            case BOOKING -> !Boolean.FALSE.equals(user.getNotifyBooking());
            case REMINDER -> !Boolean.FALSE.equals(user.getNotifyReminder());
            case NEWS -> !Boolean.FALSE.equals(user.getNotifyNews());
            case VIOLATION, VIOLATION_REPORT, REPUTATION, SYSTEM, SUPPORT_REQUEST, COMPLAINT,
                    SEAT_STATUS_REPORT, CHAT_MESSAGE -> true;
        };
    }

    /**
     * Kiem tra loai thong bao co duoc bat o cap do he thong (admin config) hay khong.
     * Cac loai SYSTEM, NEWS, CHAT_MESSAGE, SUPPORT_REQUEST luon duoc gui.
     */
    private boolean isNotificationEnabledGlobally(NotificationType type, String deliveryKey) {
        try {
            LibrarySetting settings = librarySettingService.getSettings();
            return switch (type) {
                case BOOKING -> Boolean.TRUE.equals(settings.getNotifyBookingSuccess());
                case REMINDER -> DELIVERY_KEY_TIME_EXPIRY.equals(deliveryKey)
                        ? Boolean.TRUE.equals(settings.getNotifyTimeExpiry())
                        : Boolean.TRUE.equals(settings.getNotifyCheckinReminder());
                case VIOLATION -> Boolean.TRUE.equals(settings.getNotifyViolation());
                case VIOLATION_REPORT, REPUTATION, SYSTEM, NEWS, CHAT_MESSAGE, SUPPORT_REQUEST, COMPLAINT,
                        SEAT_STATUS_REPORT -> true;
            };
        } catch (Exception e) {
            log.warn("Khong the kiem tra cau hinh thong bao he thong, cho phep gui mac dinh: {}", e.getMessage());
            return true;
        }
    }

    public NotificationDTO toDTO(NotificationEntity notification) {
        String category = resolveCategory(notification.getNotificationType(), notification.getTitle(),
                notification.getContent());

        return NotificationDTO.builder()
                .id(notification.getId())
                .userId(notification.getUser() != null ? notification.getUser().getId() : null)
                .title(notification.getTitle())
                .content(notification.getContent())
                .notificationType(notification.getNotificationType() != null ? notification.getNotificationType().name() : null)
                .category(category)
                .categoryLabel(resolveCategoryLabel(category))
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public String resolveCategory(NotificationType type, String title, String body) {
        if (type == null) {
            return "SYSTEM";
        }

        return switch (type) {
            case CHAT_MESSAGE -> "MESSAGE";
            case BOOKING, REMINDER -> "BOOKING";
            case SUPPORT_REQUEST, COMPLAINT, SEAT_STATUS_REPORT, VIOLATION_REPORT -> "PROCESSING";
            case REPUTATION -> "REPUTATION";
            case NEWS -> "NEWS";
            case VIOLATION -> containsPointKeywords(title, body) ? "REPUTATION" : "PROCESSING";
            case SYSTEM -> containsProcessingKeywords(title, body) ? "PROCESSING" : "SYSTEM";
        };
    }

    public String resolveCategoryLabel(String category) {
        return switch (category) {
            case "MESSAGE" -> "Tin nhắn";
            case "PROCESSING" -> "Xử lý";
            case "REPUTATION" -> "Điểm uy tín";
            case "BOOKING" -> "Đặt chỗ";
            case "NEWS" -> "Tin tức";
            default -> "Hệ thống";
        };
    }

    private boolean containsPointKeywords(String title, String body) {
        String combined = normalizeForMatching(title) + " " + normalizeForMatching(body);
        return combined.contains("diem")
                || combined.contains("uy tin")
                || combined.contains("thuong")
                || combined.contains("phat")
                || combined.contains("tru")
                || combined.contains("cong")
                || combined.contains("hoan");
    }

    private boolean containsProcessingKeywords(String title, String body) {
        String combined = normalizeForMatching(title) + " " + normalizeForMatching(body);
        return combined.contains("xu ly")
                || combined.contains("tiep nhan")
                || combined.contains("giai quyet")
                || combined.contains("tu choi")
                || combined.contains("xac minh")
                || combined.contains("khieu nai")
                || combined.contains("bao cao")
                || combined.contains("yeu cau");
    }

    private String normalizeForMatching(String value) {
        if (value == null) {
            return "";
        }

        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    /**
     * Get notifications for a user
     */
    @Transactional(readOnly = true)
    public List<NotificationEntity> getUserNotifications(UUID userId, int limit) {
        if (limit > 0) {
            return notificationRepository.findByUserIdWithLimit(userId, limit);
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getUserNotificationDTOs(UUID userId, int limit) {
        return getUserNotifications(userId, limit).stream()
                .map(this::toDTO)
                .toList();
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
    public void markAsRead(UUID notificationId, UUID userId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getUser() == null || !notification.getUser().getId().equals(userId)) {
                throw new RuntimeException("Bạn không có quyền đánh dấu thông báo của người khác.");
            }
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

    @Transactional
    public int markAllAsReadByCategory(UUID userId, String category) {
        String normalizedCategory = category == null ? "" : category.trim().toUpperCase();
        if (normalizedCategory.isEmpty()) {
            return 0;
        }

        List<NotificationEntity> unreadNotifications = notificationRepository.findUnreadByUserId(userId);
        List<NotificationEntity> matchingNotifications = unreadNotifications.stream()
                .filter(notification -> normalizedCategory.equals(
                        resolveCategory(
                                notification.getNotificationType(),
                                notification.getTitle(),
                                notification.getContent())))
                .peek(notification -> notification.setIsRead(true))
                .toList();

        if (matchingNotifications.isEmpty()) {
            return 0;
        }

        notificationRepository.saveAll(matchingNotifications);
        return matchingNotifications.size();
    }

    /**
     * Delete a notification (only if it belongs to the user)
     */
    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getUser() == null || !notification.getUser().getId().equals(userId)) {
                throw new RuntimeException("Bạn không có quyền xoá thông báo của người khác.");
            }
            notificationRepository.delete(notification);
        });
    }
}
