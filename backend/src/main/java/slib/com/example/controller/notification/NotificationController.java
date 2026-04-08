package slib.com.example.controller.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.notification.NotificationDTO;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.users.User;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.notification.PushNotificationService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for managing user notifications
 */
@RestController
@RequestMapping("/slib/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final PushNotificationService pushNotificationService;
    private final UserRepository userRepository;

    private User requireCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }

        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private UUID resolveAuthorizedUserId(UUID requestedUserId, UserDetails userDetails) {
        User currentUser = requireCurrentUser(userDetails);
        if (currentUser.getRole() != null && currentUser.getRole().isStaff()) {
            return requestedUserId;
        }
        if (!currentUser.getId().equals(requestedUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền thao tác trên thông báo của người khác.");
        }
        return currentUser.getId();
    }

    /**
     * Get notifications for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationDTO>> getUserNotifications(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);
        return ResponseEntity.ok(pushNotificationService.getUserNotificationDTOs(resolvedUserId, limit));
    }

    /**
     * Get unread notification count for a user
     */
    @GetMapping("/unread-count/{userId}")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);
        long count = pushNotificationService.getUnreadCount(resolvedUserId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Mark a notification as read
     */
    @PutMapping("/mark-read/{notificationId}")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID notificationId,
            @RequestParam UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);
        pushNotificationService.markAsRead(notificationId, resolvedUserId);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all notifications as read for a user
     */
    @PutMapping("/mark-all-read/{userId}")
    public ResponseEntity<Void> markAllAsRead(@PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);
        pushNotificationService.markAllAsRead(resolvedUserId);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all notifications as read for a user by category
     */
    @PutMapping("/mark-all-read/{userId}/category/{category}")
    public ResponseEntity<Map<String, Object>> markAllAsReadByCategory(@PathVariable UUID userId,
            @PathVariable String category,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);
        int updated = pushNotificationService.markAllAsReadByCategory(resolvedUserId, category);
        long remainingUnread = pushNotificationService.getUnreadCount(resolvedUserId);

        return ResponseEntity.ok(Map.of(
                "updated", updated,
                "category", category.toUpperCase(),
                "remainingUnread", remainingUnread));
    }

    /**
     * Delete a notification
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID notificationId,
            @RequestParam UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);
        pushNotificationService.deleteNotification(notificationId, resolvedUserId);
        return ResponseEntity.ok().build();
    }

    /**
     * Update notification settings for a user
     */
    @PutMapping("/settings/{userId}")
    public ResponseEntity<Map<String, Object>> updateSettings(
            @PathVariable UUID userId,
            @RequestBody NotificationSettingsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);

        User user = userRepository.findById(resolvedUserId)
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
    public ResponseEntity<Map<String, Object>> getSettings(@PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID resolvedUserId = resolveAuthorizedUserId(userId, userDetails);
        User user = userRepository.findById(resolvedUserId)
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

    @PostMapping("/staff/behavior-warning")
    public ResponseEntity<Map<String, Object>> sendBehaviorWarning(
            @RequestBody BehaviorWarningRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = requireCurrentUser(userDetails);
        if (currentUser.getRole() == null || !currentUser.getRole().isStaff()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền gửi cảnh báo hành vi.");
        }

        User targetUser = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String title = "Thư viện nhắc nhở về việc sử dụng chỗ ngồi";
        String primaryIssue = request.primaryIssue() != null && !request.primaryIssue().isBlank()
                ? request.primaryIssue().trim()
                : "cần chú ý thêm";
        String detail = request.detail() != null && !request.detail().isBlank()
                ? request.detail().trim()
                : "Hệ thống ghi nhận tài khoản của bạn có dấu hiệu sử dụng chỗ ngồi chưa thật sự ổn định.";
        String body = "Thủ thư vừa gửi nhắc nhở: " + primaryIssue + ". " + detail
                + ". Vui lòng kiểm tra lịch sử đặt chỗ, vi phạm và tuân thủ nội quy để tránh bị hạn chế đặt chỗ.";

        pushNotificationService.sendToUser(
                targetUser.getId(),
                title,
                body,
                NotificationType.SYSTEM,
                targetUser.getId(),
                "BEHAVIOR_ALERT",
                "BEHAVIOR_ALERT");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã gửi nhắc nhở đến " + targetUser.getFullName(),
                "userId", targetUser.getId().toString()));
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

    public record BehaviorWarningRequest(
            UUID userId,
            String primaryIssue,
            String detail) {
    }
}
