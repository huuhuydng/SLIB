package slib.com.example.controller.users;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import slib.com.example.dto.users.AuthResponse;
import slib.com.example.dto.users.ImportUserRequest;
import slib.com.example.dto.users.UserProfileResponse;
import slib.com.example.entity.users.ImportJob;
import slib.com.example.entity.users.User;
import slib.com.example.entity.users.UserImportStaging;
import slib.com.example.service.AsyncImportService;
import slib.com.example.service.AuthService;
import slib.com.example.service.StagingImportService;
import slib.com.example.service.UserService;
import slib.com.example.service.chat.CloudinaryService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/slib/users")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class UserController {

    // Avatar size limits
    private static final long MAX_SINGLE_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB per avatar
    private static final long MAX_BATCH_TOTAL_SIZE = 200 * 1024 * 1024; // 200MB total

    private final UserService userService;
    private final AuthService authService;
    private final CloudinaryService cloudinaryService;
    private final AsyncImportService asyncImportService;
    private final StagingImportService stagingImportService;

    /**
     * Login with Google ID Token
     * Note: This endpoint wraps the new AuthService for backward compatibility.
     * Consider migrating clients to use /slib/auth/google directly.
     */
    @PostMapping("/login-google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody Map<String, String> request) {
        String idToken = request.get("id_token");
        String fullName = request.get("full_name");
        String fcmToken = request.get("noti_device");
        String deviceInfo = request.get("device_info");

        if (idToken == null || idToken.isEmpty()) {
            return ResponseEntity.badRequest().body("Thiếu Google ID Token");
        }
        try {
            AuthResponse response = authService.loginWithGoogle(idToken, fullName, fcmToken, deviceInfo);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Token không hợp lệ hoặc hết hạn");
        }
        try {
            UserProfileResponse profile = userService.getMyProfile(userDetails.getUsername());
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Không tìm thấy user: " + e.getMessage());
        }
    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateMyProfile(@AuthenticationPrincipal UserDetails userDetails,
            @RequestBody User updateRequest) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body("Token không hợp lệ");
        }

        try {
            String email = userDetails.getUsername();
            UserProfileResponse currentProfile = userService.getMyProfile(email);

            User updatedUser = userService.updateUser(currentProfile.getId(), updateRequest);

            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi update: " + e.getMessage());
        }
    }

    @GetMapping("/getall")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    /**
     * Import users in bulk (Admin only)
     * Request body: Array of ImportUserRequest
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importUsers(@RequestBody List<ImportUserRequest> requests) {
        try {
            if (requests == null || requests.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Danh sách user không được rỗng"));
            }

            Map<String, Object> result = userService.importUsers(requests);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi import: " + e.getMessage()));
        }
    }

    /**
     * Lock/Unlock user account (Admin only)
     */
    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(
            @PathVariable java.util.UUID userId,
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean isActive = request.get("isActive");
            if (isActive == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "isActive field is required"));
            }

            User updatedUser = userService.toggleUserActive(userId, isActive);
            return ResponseEntity.ok(Map.of(
                    "message", isActive ? "Đã mở khóa tài khoản" : "Đã khóa tài khoản",
                    "userId", userId,
                    "isActive", updatedUser.getIsActive()));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete user by ID (Admin only)
     * This will delete all related data (reservations, access logs, chat sessions,
     * etc.)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable java.util.UUID userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Token không hợp lệ hoặc hết hạn");
        }

        try {
            userService.deleteUserById(userId);
            return ResponseEntity.ok(Map.of(
                    "message", "Đã xoá user và tất cả dữ liệu liên quan thành công",
                    "userId", userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi xoá user: " + e.getMessage());
        }
    }

    /**
     * Upload avatar for user import (Admin only)
     * Returns the Cloudinary URL
     */
    @PostMapping("/avatar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userCode") String userCode) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File không được rỗng"));
            }

            String url = cloudinaryService.uploadAvatar(file);
            return ResponseEntity.ok(Map.of(
                    "url", url,
                    "userCode", userCode));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi upload: " + e.getMessage()));
        }
    }

    /**
     * Batch upload avatars (Admin only)
     * Upload multiple avatars in one request for faster performance
     * File names should be the userCode (e.g., SE123456.jpg)
     */
    @PostMapping("/avatars/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadAvatarsBatch(@RequestParam("files") List<MultipartFile> files) {
        try {
            if (files == null || files.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không có file nào được upload"));
            }

            // Validate total batch size
            long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
            if (totalSize > MAX_BATCH_TOTAL_SIZE) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Tổng dung lượng batch quá lớn. Tối đa: 200MB",
                        "maxSize", "200MB",
                        "actualSize", String.format("%.1f MB", totalSize / (1024.0 * 1024.0))));
            }

            // Process uploads in parallel, skip files that are too large
            List<Map<String, Object>> results = files.parallelStream()
                    .filter(file -> !file.isEmpty())
                    .map(file -> {
                        String originalName = file.getOriginalFilename();
                        // Extract userCode from filename (remove extension)
                        String userCode = originalName != null
                                ? originalName.replaceAll("\\.[^.]+$", "").toUpperCase()
                                : "UNKNOWN";

                        // Validate individual file size
                        if (file.getSize() > MAX_SINGLE_AVATAR_SIZE) {
                            return Map.<String, Object>of(
                                    "userCode", userCode,
                                    "error", "File quá lớn. Tối đa mỗi avatar: 2MB",
                                    "success", false);
                        }

                        try {
                            String url = cloudinaryService.uploadAvatar(file);
                            return Map.<String, Object>of(
                                    "userCode", userCode,
                                    "url", url,
                                    "success", true);
                        } catch (Exception e) {
                            return Map.<String, Object>of(
                                    "userCode", userCode,
                                    "error", e.getMessage(),
                                    "success", false);
                        }
                    })
                    .toList();

            long successCount = results.stream().filter(r -> (Boolean) r.get("success")).count();

            return ResponseEntity.ok(Map.of(
                    "total", files.size(),
                    "success", successCount,
                    "failed", files.size() - successCount,
                    "results", results));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Loi batch upload: " + e.getMessage()));
        }
    }

    /**
     * Delete multiple avatars by URLs (for rollback when import fails)
     */
    @DeleteMapping("/avatars/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAvatarsBatch(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> urls = (List<String>) request.get("urls");

            if (urls == null || urls.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Khong co URL nao de xoa"));
            }

            int deletedCount = cloudinaryService.deleteAvatars(urls);

            return ResponseEntity.ok(Map.of(
                    "total", urls.size(),
                    "deleted", deletedCount,
                    "failed", urls.size() - deletedCount));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Loi xoa avatars: " + e.getMessage()));
        }
    }

    /**
     * Validate users before import (check duplicates)
     */
    @PostMapping("/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> validateUsers(@RequestBody List<ImportUserRequest> requests) {
        try {
            if (requests == null || requests.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Danh sách user không được rỗng"));
            }

            // Check for existing userCodes and emails
            List<Map<String, String>> duplicates = new java.util.ArrayList<>();
            for (ImportUserRequest req : requests) {
                if (userService.existsByEmail(req.getEmail())) {
                    duplicates.add(Map.of(
                            "userCode", req.getUserCode(),
                            "field", "email",
                            "message", "Email đã tồn tại: " + req.getEmail()));
                }
                // Could also check userCode if userService has that method
            }

            return ResponseEntity.ok(Map.of(
                    "valid", duplicates.isEmpty(),
                    "duplicates", duplicates));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi validate: " + e.getMessage()));
        }
    }

    // =====================================================
    // ADVANCED IMPORT ENDPOINTS (Async, Streaming, Staging)
    // =====================================================

    /**
     * Start async import from Excel file.
     * Uses SAX streaming parser for low memory usage.
     * Returns immediately with batchId for tracking.
     */
    @PostMapping("/import/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importFromExcel(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File không được rỗng"));
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chỉ hỗ trợ file Excel (.xlsx, .xls)"));
            }

            // Start import - parses to staging table
            UUID batchId = asyncImportService.startImport(file);

            // Trigger async processing (validation + import)
            asyncImportService.processImportAsync(batchId);

            // Return immediately with batchId for tracking
            return ResponseEntity.ok(Map.of(
                    "batchId", batchId.toString(),
                    "message", "Import đã bắt đầu. Sử dụng batchId để theo dõi tiến độ.",
                    "status", "PROCESSING"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi import: " + e.getMessage()));
        }
    }

    /**
     * Get import job status and progress.
     */
    @GetMapping("/import/{batchId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getImportStatus(@PathVariable String batchId) {
        try {
            UUID uuid = UUID.fromString(batchId);
            ImportJob job = stagingImportService.getJobStatus(uuid);

            if (job == null) {
                return ResponseEntity.notFound().build();
            }

            // Use HashMap since Map.of() only supports up to 10 entries
            java.util.HashMap<String, Object> response = new java.util.HashMap<>();
            response.put("batchId", job.getBatchId().toString());
            response.put("fileName", job.getFileName() != null ? job.getFileName() : "");
            response.put("status", job.getStatus().name());
            response.put("totalRows", job.getTotalRows());
            response.put("validCount", job.getValidCount());
            response.put("invalidCount", job.getInvalidCount());
            response.put("importedCount", job.getImportedCount());
            response.put("avatarCount", job.getAvatarCount());
            response.put("avatarUploaded", job.getAvatarUploaded());
            response.put("createdAt", job.getCreatedAt() != null ? job.getCreatedAt().toString() : "");
            response.put("completedAt", job.getCompletedAt() != null ? job.getCompletedAt().toString() : "");
            response.put("errorMessage", job.getErrorMessage() != null ? job.getErrorMessage() : "");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid batchId format"));
        }
    }

    /**
     * Get failed/invalid rows from import.
     */
    @GetMapping("/import/{batchId}/errors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getImportErrors(@PathVariable String batchId) {
        try {
            UUID uuid = UUID.fromString(batchId);
            List<UserImportStaging> failedRows = stagingImportService.getFailedRows(uuid);

            return ResponseEntity.ok(Map.of(
                    "count", failedRows.size(),
                    "errors", failedRows.stream().map(row -> Map.of(
                            "rowNumber", row.getRowNumber() != null ? row.getRowNumber() : 0,
                            "userCode", row.getUserCode() != null ? row.getUserCode() : "",
                            "email", row.getEmail() != null ? row.getEmail() : "",
                            "fullName", row.getFullName() != null ? row.getFullName() : "",
                            "errorMessage", row.getErrorMessage() != null ? row.getErrorMessage() : "")).toList()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid batchId format"));
        }
    }
}