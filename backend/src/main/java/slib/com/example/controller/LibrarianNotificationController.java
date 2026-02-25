package slib.com.example.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import slib.com.example.service.LibrarianNotificationService;

import java.util.Map;

/**
 * API tổng hợp pending counts cho thủ thư
 */
@RestController
@RequestMapping("/slib/librarian")
@RequiredArgsConstructor
public class LibrarianNotificationController {

    private final LibrarianNotificationService librarianNotificationService;

    /**
     * Lấy tổng hợp số lượng mục cần xử lý
     * GET /slib/librarian/pending-counts
     */
    @GetMapping("/pending-counts")
    public ResponseEntity<Map<String, Object>> getPendingCounts() {
        return ResponseEntity.ok(librarianNotificationService.getPendingCounts());
    }
}
