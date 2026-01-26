package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import slib.com.example.dto.users.UserProfileResponse;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;

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
                .reputationScore(user.getReputationScore() != null ? user.getReputationScore() : 100)
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


}