package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.users.UserProfileResponse;
import slib.com.example.entity.users.User;
import slib.com.example.repository.AccessLogRepository;
import slib.com.example.repository.RefreshTokenRepository;
import slib.com.example.repository.ReservationRepository;
import slib.com.example.repository.StudentProfileRepository;
import slib.com.example.repository.UserRepository;
import slib.com.example.repository.UserSettingRepository;
import slib.com.example.repository.activity.ActivityLogRepository;
import slib.com.example.repository.activity.PointTransactionRepository;
import slib.com.example.repository.ai.ChatSessionRepository;

import java.util.List;
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
                .studentCode(user.getStudentCode())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
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
        return userRepository.save(existingUser);
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
}