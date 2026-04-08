package slib.com.example.controller.users;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import slib.com.example.dto.users.AuthResponse;
import slib.com.example.dto.users.AdminCreateUserRequest;
import slib.com.example.dto.users.AdminUpdateUserRequest;
import slib.com.example.dto.users.AdminUserListItemResponse;
import slib.com.example.dto.users.ImportUserRequest;
import slib.com.example.dto.users.UserProfileResponse;
import slib.com.example.dto.users.UserListItemResponse;
import slib.com.example.entity.users.ImportJob;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.entity.users.UserImportStaging;
import slib.com.example.service.users.AsyncImportService;
import slib.com.example.service.auth.AuthService;
import slib.com.example.service.users.StagingImportService;
import slib.com.example.service.users.UserService;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.system.SystemLogService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/slib/users")
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
    private final SystemLogService systemLogService;

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
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
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
    public List<UserListItemResponse> getAllUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return userService.getAllUsers(role, parseStatus(status), search);
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminUserListItemResponse> getAdminUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return userService.getAdminUsers(role, parseStatus(status), search);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            AdminUserListItemResponse created = userService.createUser(request);
            systemLogService.logAudit(
                    "UserController",
                    "Tạo người dùng mới: " + created.email(),
                    null,
                    userDetails != null ? userDetails.getUsername() : null);
            return ResponseEntity.ok(Map.of(
                    "message", "Đã tạo người dùng mới",
                    "user", created));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Import users in bulk (Admin only)
     * Request body: Array of ImportUserRequest
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> importUsers(
            @RequestBody List<@Valid ImportUserRequest> requests,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (requests == null || requests.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Danh sách user không được rỗng"));
            }

            Map<String, Object> result = userService.importUsers(requests);

            // Gửi welcome email SAU KHI transaction commit thành công
            List<Map<String, Object>> successList = (List<Map<String, Object>>) result.get("success");
            if (successList != null && !successList.isEmpty()) {
                userService.sendWelcomeEmails(successList);
            }

            systemLogService.logAudit(
                    "UserController",
                    "Import người dùng: thành công %d, thất bại %d".formatted(
                            ((Number) result.get("successCount")).intValue(),
                            ((Number) result.get("failedCount")).intValue()),
                    null,
                    userDetails != null ? userDetails.getUsername() : null);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lỗi import: " + e.getMessage()));
        }
    }

    /**
     * Admin edit user profile (Admin only)
     * Allows editing: fullName, phone, email, dob, role
     */
    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminUpdateUser(
            @PathVariable java.util.UUID userId,
            @Valid @RequestBody AdminUpdateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User existingUser = userService.getUserById(userId);
            if (existingUser == null) {
                return ResponseEntity.notFound().build();
            }

            if (request.getFullName() != null) {
                String fullName = request.getFullName();
                if (fullName != null && !fullName.trim().isEmpty()) {
                    existingUser.setFullName(fullName.trim());
                }
            }
            if (request.getPhone() != null) {
                existingUser.setPhone(request.getPhone());
            }
            if (request.getEmail() != null) {
                String email = request.getEmail();
                existingUser.setEmail(email != null ? email.trim() : null);
            }
            if (request.getDob() != null) {
                String dobStr = request.getDob();
                if (dobStr != null && !dobStr.isEmpty()) {
                    existingUser.setDob(java.time.LocalDate.parse(dobStr));
                } else {
                    existingUser.setDob(null);
                }
            }
            if (request.getRole() != null) {
                existingUser.setRole(request.getRole());
            }

            User saved = userService.saveUser(existingUser);
            systemLogService.logAudit(
                    "UserController",
                    "Cập nhật thông tin người dùng: " + userId,
                    null,
                    userDetails != null ? userDetails.getUsername() : null);
            return ResponseEntity.ok(Map.of(
                    "message", "Đã cập nhật thông tin người dùng",
                    "userId", userId,
                    "fullName", saved.getFullName() != null ? saved.getFullName() : "",
                    "email", saved.getEmail() != null ? saved.getEmail() : ""));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lock/Unlock user account (Admin only)
     */
    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(
            @PathVariable java.util.UUID userId,
            @RequestBody UserStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Boolean isActive = request.isActive();
            if (isActive == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "isActive field is required"));
            }

            User updatedUser = userService.toggleUserActive(userId, isActive, request.reason());
            systemLogService.logAudit(
                    "UserController",
                    (isActive ? "Mở khóa" : "Khóa") + " tài khoản người dùng: " + userId,
                    null,
                    userDetails != null ? userDetails.getUsername() : null);
            Map<String, Object> response = new HashMap<>();
            response.put("message", isActive ? "Đã mở khóa tài khoản" : "Đã khóa tài khoản");
            response.put("userId", userId);
            response.put("isActive", updatedUser.getIsActive());
            response.put("lockReason", updatedUser.getLockReason());
            return ResponseEntity.ok(response);

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
            systemLogService.logAudit(
                    "UserController",
                    "Xóa người dùng: " + userId,
                    null,
                    userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "message", "Đã xóa vĩnh viễn người dùng và dữ liệu liên quan",
                    "userId", userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{userId}/active-bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserActiveBookings(@PathVariable UUID userId) {
        try {
            long count = userService.countActiveOrUpcomingBookings(userId);
            return ResponseEntity.ok(Map.of(
                    "count", count,
                    "hasActiveBookings", count > 0));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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

    /**
     * Download XLSX template for user import.
     * Generates a template file with correct column headers and sample data rows.
     */
    @GetMapping("/import/template")
    @PreAuthorize("hasRole('ADMIN')")
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=slib_user_import_template.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Import Users");

            // ===== Header style =====
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // ===== Note style (for sample rows) =====
            CellStyle sampleStyle = workbook.createCellStyle();
            Font sampleFont = workbook.createFont();
            sampleFont.setItalic(true);
            sampleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            sampleStyle.setFont(sampleFont);

            // ===== Date style =====
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.cloneStyleFrom(sampleStyle);
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy"));

            // ===== Column definitions matching AsyncImportService parser =====
            String[] headers = {
                    "Mã người dùng (userCode)", // A
                    "Họ và tên (fullName)", // B
                    "Email", // C
                    "Số điện thoại (phone)", // D
                    "Ngày sinh (dd/MM/yyyy)", // E
                    "Vai trò (role)" // F
            };

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample row 1
            Row sample1 = sheet.createRow(1);
            String[] data1 = { "SE123456", "Nguyễn Văn A", "nguyenvana@gmail.com", "0901234567", "15/03/2003",
                    "STUDENT" };
            for (int i = 0; i < data1.length; i++) {
                Cell cell = sample1.createCell(i);
                cell.setCellValue(data1[i]);
                cell.setCellStyle(sampleStyle);
            }

            // Sample row 2
            Row sample2 = sheet.createRow(2);
            String[] data2 = { "SE789012", "Trần Thị B", "tranthib@gmail.com", "0912345678", "20/08/2002", "STUDENT" };
            for (int i = 0; i < data2.length; i++) {
                Cell cell = sample2.createCell(i);
                cell.setCellValue(data2[i]);
                cell.setCellStyle(sampleStyle);
            }

            // ===== Column widths =====
            sheet.setColumnWidth(0, 20 * 256); // userCode
            sheet.setColumnWidth(1, 25 * 256); // fullName
            sheet.setColumnWidth(2, 30 * 256); // email
            sheet.setColumnWidth(3, 20 * 256); // phone
            sheet.setColumnWidth(4, 22 * 256); // dob
            sheet.setColumnWidth(5, 18 * 256); // role

            // ===== Notes sheet (hướng dẫn) =====
            Sheet notesSheet = workbook.createSheet("Hướng dẫn");

            CellStyle noteHeaderStyle = workbook.createCellStyle();
            Font noteHeaderFont = workbook.createFont();
            noteHeaderFont.setBold(true);
            noteHeaderFont.setFontHeightInPoints((short) 14);
            noteHeaderStyle.setFont(noteHeaderFont);

            CellStyle noteStyle = workbook.createCellStyle();
            noteStyle.setWrapText(true);

            int r = 0;
            Row nr = notesSheet.createRow(r++);
            Cell nc = nr.createCell(0);
            nc.setCellValue("HƯỚNG DẪN IMPORT NGƯỜI DÙNG - SLIB");
            nc.setCellStyle(noteHeaderStyle);

            r++;
            String[] notes = {
                    "1. Điền thông tin người dùng vào sheet \"Import Users\"",
                    "2. Xóa 2 dòng dữ liệu mẫu trước khi import",
                    "3. Cột A - Mã người dùng: Bắt buộc, duy nhất (VD: SE123456)",
                    "4. Cột B - Họ và tên: Bắt buộc",
                    "5. Cột C - Email: Bắt buộc, phải là email hợp lệ, duy nhất",
                    "6. Cột D - Số điện thoại: Không bắt buộc, 10 chữ số",
                    "7. Cột E - Ngày sinh: Không bắt buộc, định dạng dd/MM/yyyy",
                    "8. Cột F - Vai trò: STUDENT, TEACHER, LIBRARIAN hoặc ADMIN (mặc định: STUDENT)",
                    "",
                    "NHẬP KÈM AVATAR:",
                    "- Đặt tên file ảnh trùng với mã người dùng (VD: SE123456.jpg)",
                    "- Nén file template + tất cả ảnh vào 1 file .zip rồi upload"
            };

            for (String note : notes) {
                Row noteRow = notesSheet.createRow(r++);
                Cell noteCell = noteRow.createCell(0);
                noteCell.setCellValue(note);
                if (note.startsWith("NHẬP") || note.startsWith("1.")) {
                    noteCell.setCellStyle(noteHeaderStyle);
                }
            }
            notesSheet.setColumnWidth(0, 60 * 256);

            workbook.write(response.getOutputStream());
        }
    }

    private Boolean parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status.trim().toLowerCase()) {
            case "active", "hoạt động", "hoat dong" -> true;
            case "locked", "inactive", "đã khóa", "da khoa" -> false;
            default -> null;
        };
    }

    public record UserStatusUpdateRequest(Boolean isActive, String reason) {
    }
}
