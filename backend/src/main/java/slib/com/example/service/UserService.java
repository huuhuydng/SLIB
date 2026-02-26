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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
     * Optimized: batch duplicate check + batch save (no N+1 queries)
     */
    @Transactional
    public Map<String, Object> importUsers(List<ImportUserRequest> requests) {
        List<Map<String, Object>> successList = new ArrayList<>();
        List<Map<String, Object>> failedList = new ArrayList<>();

        String encodedPassword = authService.encodeDefaultPassword();

        // Batch check duplicates (2 queries total thay vì 2*N queries)
        Set<String> emailsToCheck = requests.stream()
                .map(r -> r.getEmail() != null ? r.getEmail().toLowerCase() : "")
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toSet());
        Set<String> codesToCheck = requests.stream()
                .map(r -> r.getUserCode() != null ? r.getUserCode().toUpperCase() : "")
                .filter(c -> !c.isEmpty())
                .collect(Collectors.toSet());

        Set<String> existingEmails = new HashSet<>();
        Set<String> existingCodes = new HashSet<>();
        if (!emailsToCheck.isEmpty()) {
            existingEmails = new HashSet<>(userRepository.findExistingEmails(emailsToCheck));
            existingEmails = existingEmails.stream().map(String::toLowerCase).collect(Collectors.toSet());
        }
        if (!codesToCheck.isEmpty()) {
            existingCodes = new HashSet<>(userRepository.findExistingUserCodes(codesToCheck));
            existingCodes = existingCodes.stream().map(String::toUpperCase).collect(Collectors.toSet());
        }

        // Track emails/codes in this batch to detect intra-batch duplicates
        Set<String> batchEmails = new HashSet<>();
        Set<String> batchCodes = new HashSet<>();
        List<User> usersToSave = new ArrayList<>();

        for (ImportUserRequest req : requests) {
            try {
                if (req.getUserCode() == null || req.getUserCode().isEmpty()) {
                    throw new RuntimeException("User code is required");
                }
                if (req.getEmail() == null || req.getEmail().isEmpty()) {
                    throw new RuntimeException("Email is required");
                }
                if (req.getFullName() == null || req.getFullName().isEmpty()) {
                    throw new RuntimeException("Full name is required");
                }

                String email = req.getEmail().toLowerCase();
                String code = req.getUserCode().toUpperCase();

                // Check DB duplicates
                if (existingEmails.contains(email)) {
                    throw new RuntimeException("Email đã tồn tại: " + req.getEmail());
                }
                if (existingCodes.contains(code)) {
                    throw new RuntimeException("Mã người dùng đã tồn tại: " + req.getUserCode());
                }
                // Check intra-batch duplicates
                if (!batchEmails.add(email)) {
                    throw new RuntimeException("Email trùng trong batch: " + req.getEmail());
                }
                if (!batchCodes.add(code)) {
                    throw new RuntimeException("Mã người dùng trùng trong batch: " + req.getUserCode());
                }

                User user = User.builder()
                        .userCode(code)
                        .username(code)
                        .email(email)
                        .fullName(req.getFullName())
                        .phone(req.getPhone())
                        .dob(req.getDob())
                        .role(req.getRole() != null ? req.getRole() : Role.STUDENT)
                        .password(encodedPassword)
                        .passwordChanged(false)
                        .isActive(true)
                        .avtUrl(req.getAvtUrl())
                        .build();

                usersToSave.add(user);
                successList.add(Map.of(
                        "userCode", code,
                        "email", email,
                        "fullName", req.getFullName()));

            } catch (Exception e) {
                failedList.add(Map.of(
                        "userCode", req.getUserCode() != null ? req.getUserCode() : "",
                        "email", req.getEmail() != null ? req.getEmail() : "",
                        "reason", e.getMessage()));
            }
        }

        // Batch save all valid users
        if (!usersToSave.isEmpty()) {
            List<User> savedUsers = userRepository.saveAll(usersToSave);

            // Batch create UserSettings
            try {
                List<UserSetting> settings = savedUsers.stream()
                        .map(user -> UserSetting.builder()
                                .userId(user.getId())
                                .user(user)
                                .isHceEnabled(true)
                                .isAiRecommendEnabled(true)
                                .isBookingRemindEnabled(true)
                                .themeMode("light")
                                .languageCode("vi")
                                .build())
                        .toList();
                userSettingRepository.saveAll(settings);
            } catch (Exception e) {
                // Ignore if DB trigger already created settings
            }
        }

        return Map.of(
                "success", successList,
                "failed", failedList,
                "successCount", successList.size(),
                "failedCount", failedList.size());
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