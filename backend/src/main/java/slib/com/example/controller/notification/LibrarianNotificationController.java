package slib.com.example.controller.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import slib.com.example.service.notification.LibrarianNotificationService;
import slib.com.example.service.users.UserService;

import java.util.Map;
import java.util.UUID;

/**
 * API tổng hợp pending counts cho thủ thư
 */
@RestController
@RequestMapping("/slib/librarian")
@RequiredArgsConstructor
public class LibrarianNotificationController {

    private final LibrarianNotificationService librarianNotificationService;
    private final UserService userService;

    /**
     * Lấy tổng hợp số lượng mục cần xử lý
     * GET /slib/librarian/pending-counts
     */
    @GetMapping("/pending-counts")
    public ResponseEntity<Map<String, Object>> getPendingCounts() {
        return ResponseEntity.ok(librarianNotificationService.getPendingCounts());
    }

    /**
     * Lấy số tin nhắn chưa đọc từ student
     * GET /slib/librarian/unread-chat-count
     */
    @GetMapping("/unread-chat-count")
    public ResponseEntity<Map<String, Object>> getUnreadChatCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = userService.getUserByEmail(userDetails.getUsername()).getId();
        long unreadMessages = librarianNotificationService.getUnreadChatCount(librarianId);
        long unreadConversations = librarianNotificationService.getUnreadChatConversationCount(librarianId);
        return ResponseEntity.ok(Map.of(
                "count", unreadMessages,
                "unreadMessages", unreadMessages,
                "unreadConversations", unreadConversations));
    }

    /**
     * Đánh dấu tất cả tin nhắn student trong conversation đã đọc
     * POST /slib/librarian/chat/{conversationId}/mark-read
     */
    @PostMapping("/chat/{conversationId}/mark-read")
    public ResponseEntity<Map<String, Object>> markConversationAsRead(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        int updated = librarianNotificationService.markConversationAsRead(conversationId);
        UUID librarianId = userService.getUserByEmail(userDetails.getUsername()).getId();
        long remainingMessages = librarianNotificationService.getUnreadChatCount(librarianId);
        long remainingConversations = librarianNotificationService.getUnreadChatConversationCount(librarianId);
        return ResponseEntity.ok(Map.of(
                "updated", updated,
                "remainingUnread", remainingMessages,
                "remainingUnreadMessages", remainingMessages,
                "remainingUnreadConversations", remainingConversations));
    }
}
