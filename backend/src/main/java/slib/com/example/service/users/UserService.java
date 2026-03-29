package slib.com.example.service.users;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.users.ImportUserRequest;
import slib.com.example.dto.users.UserProfileResponse;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.entity.users.UserSetting;
import slib.com.example.repository.hce.AccessLogRepository;
import slib.com.example.repository.users.RefreshTokenRepository;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.StudentProfileRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.repository.users.UserSettingRepository;
import slib.com.example.repository.activity.ActivityLogRepository;
import slib.com.example.repository.activity.PointTransactionRepository;
import slib.com.example.repository.ai.ChatSessionRepository;
import slib.com.example.repository.chat.ConversationRepository;

import slib.com.example.service.chat.CloudinaryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import slib.com.example.service.notification.EmailService;
import slib.com.example.service.auth.AuthService;

/**
 * UserService - handles user profile and management operations.
 * Note: Authentication is now handled by AuthService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AccessLogRepository accessLogRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ReservationRepository reservationRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserSettingRepository userSettingRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ConversationRepository conversationRepository;
    private final AuthService authService;
    private final CloudinaryService cloudinaryService;
    private final EmailService emailService;
    private final EntityManager entityManager;

    /**
     * Get current user profile by email
     */
    public UserProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .userCode(user.getUserCode())
                .username(user.getUsername())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .dob(user.getDob())
                .phone(user.getPhone())
                .avtUrl(user.getAvtUrl())
                .passwordChanged(user.getPasswordChanged())
                .build();
    }

    /**
     * Get all users (admin only)
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Update user (e.g., FCM token for notifications)
     */
    @Transactional
    public User updateUser(UUID userId, User req) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + userId));

        if (req.getNotiDevice() != null && !req.getNotiDevice().isEmpty()) {
            // Xóa FCM token khỏi user khác trước → tránh gửi notification sai người
            userRepository.clearNotiDeviceForOtherUsers(req.getNotiDevice(), userId);
            existingUser.setNotiDevice(req.getNotiDevice());
        }
        if (req.getPhone() != null) {
            // Check duplicate phone
            if (!req.getPhone().isEmpty() && !req.getPhone().equals(existingUser.getPhone())
                    && userRepository.existsByPhone(req.getPhone())) {
                throw new RuntimeException("Số điện thoại đã được sử dụng");
            }
            existingUser.setPhone(req.getPhone());
        }
        if (req.getDob() != null) {
            existingUser.setDob(req.getDob());
        }
        if (req.getAvtUrl() != null) {
            existingUser.setAvtUrl(req.getAvtUrl());
        }
        if (req.getFullName() != null && !req.getFullName().isEmpty()) {
            existingUser.setFullName(req.getFullName());
        }
        return userRepository.save(existingUser);
    }

    /**
     * Update user profile (for mobile/frontend)
     */
    public User updateUserProfile(UUID userId, String fullName, String phone, String avtUrl) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + userId));

        if (fullName != null && !fullName.isEmpty()) {
            existingUser.setFullName(fullName);
        }
        if (phone != null && !phone.equals(existingUser.getPhone())) {
            // Kiểm tra phone đã tồn tại chưa
            if (userRepository.existsByPhone(phone)) {
                throw new RuntimeException("Số điện thoại đã được sử dụng");
            }
            existingUser.setPhone(phone);
        }
        if (avtUrl != null) {
            existingUser.setAvtUrl(avtUrl);
        }
        return userRepository.save(existingUser);
    }

    /**
     * 👇👇👇 HÀM MỚI THÊM VÀO CHO CHAT CONTROLLER 👇👇👇
     * Get user by email (Throw exception if not found)
     * Dùng hàm này an toàn hơn vì nó đảm bảo trả về User hoặc báo lỗi ngay.
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));
    }

    /**
     * Find user by email
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Check if user exists by email
     */
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * Import users in bulk (Admin only)
     * Returns a map with:
     * - "success": List of successfully imported users
     * - "failed": List of failed imports with reasons
     */
    @Transactional
    public Map<String, Object> importUsers(List<ImportUserRequest> requests) {
        List<Map<String, Object>> successList = new ArrayList<>();
        List<Map<String, Object>> failedList = new ArrayList<>();

        String encodedPassword = authService.encodeDefaultPassword();

        for (ImportUserRequest req : requests) {
            try {
                // Validate required fields
                if (req.getUserCode() == null || req.getUserCode().isEmpty()) {
                    throw new RuntimeException("User code is required");
                }
                if (req.getEmail() == null || req.getEmail().isEmpty()) {
                    throw new RuntimeException("Email is required");
                }
                if (req.getFullName() == null || req.getFullName().isEmpty()) {
                    throw new RuntimeException("Full name is required");
                }

                // Check for duplicates
                if (userRepository.existsByEmail(req.getEmail())) {
                    throw new RuntimeException("Email đã tồn tại: " + req.getEmail());
                }
                if (userRepository.existsByUserCode(req.getUserCode())) {
                    throw new RuntimeException("Mã số đã tồn tại: " + req.getUserCode());
                }

                // Create user
                User user = User.builder()
                        .userCode(req.getUserCode())
                        .username(req.getUserCode()) // Default username = userCode
                        .email(req.getEmail())
                        .fullName(req.getFullName())
                        .phone(req.getPhone())
                        .dob(req.getDob())
                        .role(req.getRole() != null ? req.getRole() : Role.STUDENT)
                        .password(encodedPassword)
                        .passwordChanged(false) // New users need to change password
                        .isActive(true)
                        .avtUrl(req.getAvtUrl()) // Avatar URL from import
                        .build();

                // Gắn UserSetting mặc định trước khi save (cascade ALL sẽ tự persist)
                UserSetting setting = UserSetting.builder()
                        .user(user)
                        .isHceEnabled(true)
                        .isAiRecommendEnabled(true)
                        .isBookingRemindEnabled(true)
                        .themeMode("light")
                        .languageCode("vi")
                        .build();
                user.setSettings(setting);

                User savedUser = userRepository.save(user);

                Map<String, Object> successEntry = new HashMap<>();
                successEntry.put("id", savedUser.getId());
                successEntry.put("userCode", savedUser.getUserCode());
                successEntry.put("email", savedUser.getEmail());
                successEntry.put("fullName", savedUser.getFullName());
                successEntry.put("role", savedUser.getRole().name());
                successList.add(successEntry);

            } catch (Exception e) {
                Map<String, Object> failedEntry = new HashMap<>();
                failedEntry.put("userCode", req.getUserCode());
                failedEntry.put("email", req.getEmail());
                failedEntry.put("reason", e.getMessage());
                failedList.add(failedEntry);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", successList);
        result.put("failed", failedList);
        result.put("successCount", successList.size());
        result.put("failedCount", failedList.size());
        return result;
    }

    /**
     * Gửi welcome email cho danh sách user vừa import thành công.
     * Gọi SAU KHI transaction commit → đảm bảo user đã được lưu.
     */
    public void sendWelcomeEmails(List<Map<String, Object>> successList) {
        for (Map<String, Object> entry : successList) {
            try {
                emailService.sendWelcomeEmail(
                        (String) entry.get("email"),
                        (String) entry.get("fullName"),
                        "Slib@2025",
                        (String) entry.get("role"));
            } catch (Exception e) {
                log.warn("[IMPORT] Failed to send welcome email to {}", entry.get("email"), e);
            }
        }
    }

    /**
     * Delete user by ID with all related data.
     * Deletes in correct order to avoid foreign key constraint violations.
     */
    @Transactional
    public void deleteUserById(UUID userId) {
        // Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + userId));

        ensureAdminRemovalAllowed(user);

        // 0. Delete avatar on Cloudinary if exists
        if (user.getAvtUrl() != null && !user.getAvtUrl().isEmpty()) {
            cloudinaryService.deleteImageByUrl(user.getAvtUrl());
        }

        // === Delete in order (child tables first, then parent) ===

        // 1. Activity logs & point transactions
        activityLogRepository.deleteByUserId(userId);
        pointTransactionRepository.deleteByUserId(userId);

        // 2. Messages (must delete before conversations due to FK
        // sender_id/receiver_id)
        entityManager.createNativeQuery("DELETE FROM messages WHERE sender_id = :uid OR receiver_id = :uid")
                .setParameter("uid", userId).executeUpdate();

        // 3. Conversations - clear librarian ref, then delete student conversations
        conversationRepository.clearLibrarianByUserId(userId);
        conversationRepository.deleteByStudentId(userId);

        // 4. Chat sessions (AI chat - cascade deletes via entity config)
        chatSessionRepository.deleteByUser_Id(userId);

        // 5. Access logs
        accessLogRepository.deleteByUser_Id(userId);

        // 6. Reservations
        reservationRepository.deleteByUser_Id(userId);

        // 7. Refresh tokens
        refreshTokenRepository.deleteByUser_Id(userId);

        // 8. Student profile & user settings
        studentProfileRepository.deleteByUserId(userId);
        try {
            userSettingRepository.deleteById(userId);
        } catch (Exception ignored) {
        }

        // 9. Other FK tables - use native SQL for tables without repositories
        entityManager.createNativeQuery("DELETE FROM complaints WHERE user_id = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("UPDATE complaints SET resolved_by = NULL WHERE resolved_by = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM feedbacks WHERE user_id = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("UPDATE feedbacks SET reviewed_by = NULL WHERE reviewed_by = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM kiosk_qr_sessions WHERE student_id = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM notifications WHERE user_id = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("UPDATE seat_status_reports SET verified_by = NULL WHERE verified_by = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM seat_status_reports WHERE user_id = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("UPDATE seat_violation_reports SET verified_by = NULL WHERE verified_by = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("UPDATE seat_violation_reports SET reporter_id = NULL WHERE reporter_id = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM seat_violation_reports WHERE violator_id = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("UPDATE support_requests SET resolved_by = NULL WHERE resolved_by = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM support_requests WHERE student_id = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("UPDATE backup_history SET created_by = NULL WHERE created_by = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("UPDATE new_books SET created_by = NULL WHERE created_by = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("UPDATE news SET user_id = NULL WHERE user_id = :uid")
                .setParameter("uid", userId).executeUpdate();
        entityManager.createNativeQuery("UPDATE system_logs SET user_id = NULL WHERE user_id = :uid")
                .setParameter("uid", userId).executeUpdate();

        // 10. Finally delete the user
        entityManager.flush();
        userRepository.delete(user);
    }

    /**
     * Lock/unlock user account
     */
    @Transactional
    public User toggleUserActive(UUID userId, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + userId));

        if (!isActive) {
            ensureAdminWillRemain(user, false, user.getRole());
        }

        user.setIsActive(isActive);

        if (!isActive) {
            // If locking, revoke all refresh tokens
            refreshTokenRepository.revokeAllByUserId(userId);
        }

        return userRepository.save(user);
    }

    /**
     * Get user by ID (Admin)
     */
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + userId));
    }

    /**
     * Save user entity (Admin)
     */
    @Transactional
    public User saveUser(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + user.getId()));

        ensureAdminWillRemain(existingUser, user.getIsActive(), user.getRole());
        return userRepository.save(user);
    }

    public User getActiveStudentByUserCode(String userCode) {
        User user = userRepository.findByUserCode(userCode.toUpperCase().trim())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên với mã: " + userCode));

        if (user.getRole() != Role.STUDENT) {
            throw new RuntimeException("Mã này không thuộc tài khoản sinh viên");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản sinh viên hiện đang bị khóa");
        }

        return user;
    }

    private void ensureAdminRemovalAllowed(User user) {
        if (user.getRole() == Role.ADMIN && Boolean.TRUE.equals(user.getIsActive())) {
            long activeAdmins = userRepository.countByRoleAndIsActiveTrue(Role.ADMIN);
            if (activeAdmins <= 1) {
                throw new RuntimeException("Không thể xoá quản trị viên cuối cùng đang hoạt động");
            }
        }
    }

    private void ensureAdminWillRemain(User existingUser, Boolean newIsActive, Role newRole) {
        if (existingUser.getRole() != Role.ADMIN || !Boolean.TRUE.equals(existingUser.getIsActive())) {
            return;
        }

        Role targetRole = newRole != null ? newRole : existingUser.getRole();
        boolean targetActive = newIsActive != null ? newIsActive : Boolean.TRUE.equals(existingUser.getIsActive());
        boolean stillActiveAdmin = targetRole == Role.ADMIN && targetActive;

        if (stillActiveAdmin) {
            return;
        }

        long activeAdmins = userRepository.countByRoleAndIsActiveTrue(Role.ADMIN);
        if (activeAdmins <= 1) {
            throw new RuntimeException("Phải luôn còn ít nhất một quản trị viên đang hoạt động");
        }
    }
}
