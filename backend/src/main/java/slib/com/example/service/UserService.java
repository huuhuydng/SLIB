package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.users.ImportUserRequest;
import slib.com.example.dto.users.UserProfileResponse;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.entity.users.UserSetting;
import slib.com.example.repository.AccessLogRepository;
import slib.com.example.repository.RefreshTokenRepository;
import slib.com.example.repository.ReservationRepository;
import slib.com.example.repository.StudentProfileRepository;
import slib.com.example.repository.UserRepository;
import slib.com.example.repository.UserSettingRepository;
import slib.com.example.repository.activity.ActivityLogRepository;
import slib.com.example.repository.activity.PointTransactionRepository;
import slib.com.example.repository.ai.ChatSessionRepository;

import slib.com.example.service.chat.CloudinaryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UserService - handles user profile and management operations.
 * Note: Authentication is now handled by AuthService.
 */
@Service
@RequiredArgsConstructor
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
    private final AuthService authService;
    private final CloudinaryService cloudinaryService;

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
    public User updateUser(UUID userId, User req) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + userId));

        if (req.getNotiDevice() != null && !req.getNotiDevice().isEmpty()) {
            existingUser.setNotiDevice(req.getNotiDevice());
        }
        if (req.getPhone() != null) {
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
        if (phone != null) {
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
                    throw new RuntimeException("Email already exists: " + req.getEmail());
                }
                if (userRepository.existsByUserCode(req.getUserCode())) {
                    throw new RuntimeException("User code already exists: " + req.getUserCode());
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

                User savedUser = userRepository.save(user);

                // Tạo UserSetting mặc định cho user mới
                try {
                    UserSetting setting = UserSetting.builder()
                            .userId(savedUser.getId())
                            .user(savedUser)
                            .isHceEnabled(true)
                            .isAiRecommendEnabled(true)
                            .isBookingRemindEnabled(true)
                            .themeMode("light")
                            .languageCode("vi")
                            .build();
                    userSettingRepository.save(setting);
                } catch (Exception settingEx) {
                    // Bỏ qua nếu DB trigger đã tạo sẵn
                }

                Map<String, Object> successEntry = new HashMap<>();
                successEntry.put("id", savedUser.getId());
                successEntry.put("userCode", savedUser.getUserCode());
                successEntry.put("email", savedUser.getEmail());
                successEntry.put("fullName", savedUser.getFullName());
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
     * Delete user by ID with all related data.
     * Deletes in correct order to avoid foreign key constraint violations.
     */
    @Transactional
    public void deleteUserById(UUID userId) {
        // Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + userId));

        // 0. Delete avatar on Cloudinary if exists
        if (user.getAvtUrl() != null && !user.getAvtUrl().isEmpty()) {
            cloudinaryService.deleteImageByUrl(user.getAvtUrl());
        }

        // Delete in order (child tables first, then parent)
        // 1. Delete activity logs (no FK constraint, just column)
        activityLogRepository.deleteByUserId(userId);

        // 2. Delete point transactions (no FK constraint, just column)
        pointTransactionRepository.deleteByUserId(userId);

        // 3. Delete chat sessions (cascade deletes messages via entity config)
        chatSessionRepository.deleteByUser_Id(userId);

        // 4. Delete access logs
        accessLogRepository.deleteByUser_Id(userId);

        // 5. Delete reservations
        reservationRepository.deleteByUser_Id(userId);

        // 6. Delete refresh tokens
        refreshTokenRepository.deleteByUser_Id(userId);

        // 7. Delete student profile (OneToOne with @MapsId)
        studentProfileRepository.deleteByUserId(userId);

        // 8. Delete user settings (OneToOne with @MapsId, or cascade from User entity)
        userSettingRepository.deleteById(userId);

        // 9. Finally delete the user
        userRepository.delete(user);
    }

    /**
     * Lock/unlock user account
     */
    @Transactional
    public User toggleUserActive(UUID userId, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + userId));

        user.setIsActive(isActive);

        if (!isActive) {
            // If locking, revoke all refresh tokens
            refreshTokenRepository.revokeAllByUserId(userId);
        }

        return userRepository.save(user);
    }
}