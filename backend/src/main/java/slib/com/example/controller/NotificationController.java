package slib.com.example.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import slib.com.example.entity.notification.NotificationEntity;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;
import slib.com.example.service.PushNotificationService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for managing user notifications
 */
@RestController
@RequestMapping("/slib/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final PushNotificationService pushNotificationService;
    private final UserRepository userRepository;

    /**
     * Get notifications for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationEntity>> getUserNotifications(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "50") int limit) {
        List<NotificationEntity> notifications = pushNotificationService.getUserNotifications(userId, limit);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notification count for a user
     */
    @GetMapping("/unread-count/{userId}")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable UUID userId) {
        long count = pushNotificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Mark a notification as read
     */
    @PutMapping("/mark-read/{notificationId}")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID notificationId) {
        pushNotificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all notifications as read for a user
     */
    @PutMapping("/mark-all-read/{userId}")
    public ResponseEntity<Void> markAllAsRead(@PathVariable UUID userId) {
        pushNotificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Update notification settings for a user
     */
    @PutMapping("/settings/{userId}")
    public ResponseEntity<Map<String, Object>> updateSettings(
            @PathVariable UUID userId,
            @RequestBody NotificationSettingsRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.notifyBooking() != null) {
            user.setNotifyBooking(request.notifyBooking());
        }
        if (request.notifyReminder() != null) {
            user.setNotifyReminder(request.notifyReminder());
        }
        if (request.notifyNews() != null) {
            user.setNotifyNews(request.notifyNews());
        }

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "notifyBooking", user.getNotifyBooking(),
                "notifyReminder", user.getNotifyReminder(),
                "notifyNews", user.getNotifyNews()));
    }

    /**
     * Get notification settings for a user
     */
    @GetMapping("/settings/{userId}")
    public ResponseEntity<Map<String, Object>> getSettings(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(Map.of(
                "notifyBooking", user.getNotifyBooking() != null ? user.getNotifyBooking() : true,
                "notifyReminder", user.getNotifyReminder() != null ? user.getNotifyReminder() : true,
                "notifyNews", user.getNotifyNews() != null ? user.getNotifyNews() : true));
    }

    /**
     * TEST ENDPOINT: Send a test notification to a user
     * Use this to verify push notifications are working
     */
    @PostMapping("/test/{userId}")
    public ResponseEntity<Map<String, Object>> sendTestNotification(
            @PathVariable UUID userId,
            @RequestBody(required = false) TestNotificationRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String title = request != null && request.title() != null
                ? request.title()
                : "Test Notification";
        String body = request != null && request.body() != null
                ? request.body()
                : "Day la thong bao test tu SLIB. Neu ban nhan duoc, he thong hoat dong tot!";

        try {
            pushNotificationService.sendToUser(
                    userId,
                    title,
                    body,
                    NotificationType.SYSTEM,
                    null);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Test notification sent to " + user.getFullName(),
                    "fcmToken", user.getNotiDevice() != null ? user.getNotiDevice().substring(0, 20) + "..." : "N/A"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Failed to send: " + e.getMessage()));
        }
    }

    /**
     * TEST ENDPOINT: Send test notification to all users
     */
    @PostMapping("/test/broadcast")
    public ResponseEntity<Map<String, Object>> sendBroadcastTest(
            @RequestBody(required = false) TestNotificationRequest request) {

        String title = request != null && request.title() != null
                ? request.title()
                : "Thong bao tu Thu vien";
        String body = request != null && request.body() != null
                ? request.body()
                : "Day la thong bao test broadcast tu SLIB";

        try {
            pushNotificationService.sendToAll(title, body, NotificationType.SYSTEM, null);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Broadcast notification sent to all users"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Failed: " + e.getMessage()));
        }
    }

    /**
     * Request DTO for notification settings
     */
    public record NotificationSettingsRequest(
            Boolean notifyBooking,
            Boolean notifyReminder,
            Boolean notifyNews) {
    }

    /**
     * Request DTO for test notification
     */
    public record TestNotificationRequest(
            String title,
            String body) {
    }
}
