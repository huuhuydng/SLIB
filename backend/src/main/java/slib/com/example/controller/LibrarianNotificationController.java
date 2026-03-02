package slib.com.example.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import slib.com.example.service.LibrarianNotificationService;
import slib.com.example.service.UserService;

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
        long count = librarianNotificationService.getUnreadChatCount(librarianId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
