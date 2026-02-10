package slib.com.example.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.users.AuthResponse;
import slib.com.example.entity.users.RefreshToken;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.repository.RefreshTokenRepository;
import slib.com.example.repository.UserRepository;
import slib.com.example.security.JwtService;

import java.time.Instant;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${google.client-id}")
    private String googleClientId;

    /**
     * Login with Google ID token
     */
    @Transactional
    public AuthResponse loginWithGoogle(String googleIdToken, String fullNameFromClient, String fcmToken,
            String deviceInfo) {
        // Verify Google ID token
        GoogleIdToken.Payload payload = verifyGoogleToken(googleIdToken);

        String email = payload.getEmail();
        String googleName = (String) payload.get("name");

        // Whitelist for testing (add your email here)
        String[] whitelistEmails = {
                "huuhuydng@gmail.com",
                "huuhuy.k4@gmail.com",
                "tbhhjqk87976@gmail.com",
        };

        // Validate FPT email or whitelist
        boolean isWhitelisted = false;
        for (String whitelistedEmail : whitelistEmails) {
            if (email.equalsIgnoreCase(whitelistedEmail)) {
                isWhitelisted = true;
                break;
            }
        }

        if (!email.endsWith("@fpt.edu.vn") && !isWhitelisted) {
            throw new RuntimeException("Chỉ chấp nhận email @fpt.edu.vn hoặc email trong whitelist");
        }

        // Extract user code from email (or use email prefix for non-FPT emails)
        String userCode = email.endsWith("@fpt.edu.vn")
                ? extractUserCode(email)
                : email.split("@")[0].toUpperCase();

        // Get or create user
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            String fullName = (fullNameFromClient != null && !fullNameFromClient.isEmpty())
                    ? fullNameFromClient
                    : googleName;

            user = User.builder()
                    .email(email)
                    .userCode(userCode)
                    .username(userCode)
                    .fullName(fullName != null ? fullName : userCode)
                    .role(Role.STUDENT)
                    .isActive(true)
                    .passwordChanged(true) // Google users don't need password change
                    .notiDevice(fcmToken)
                    .build();
            user = userRepository.save(user);
        } else {
            // Update FCM token if provided
            if (fcmToken != null && !fcmToken.isEmpty()) {
                user.setNotiDevice(fcmToken);
                user = userRepository.save(user);
            }
        }

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Revoke all old refresh tokens before saving new one (single device policy)
        refreshTokenRepository.revokeAllByUserId(user.getId());

        // Save refresh token hash to database
        saveRefreshToken(user, refreshToken, deviceInfo);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .id(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userCode(user.getUserCode())
                .role(user.getRole().name())
                .expiresIn(3600L) // 1 hour in seconds
                .passwordChanged(user.getPasswordChanged() != null ? user.getPasswordChanged() : false)
                .build();
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        // Validate refresh token format
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Token không hợp lệ - không phải refresh token");
        }

        // Check if token exists and not revoked
        String tokenHash = jwtService.hashToken(refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Refresh token không tồn tại"));

        if (!storedToken.isValid()) {
            throw new RuntimeException("Refresh token đã hết hạn hoặc bị thu hồi");
        }

        // Get user from token
        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Keep same refresh token
                .id(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userCode(user.getUserCode())
                .role(user.getRole().name())
                .expiresIn(3600L)
                .passwordChanged(user.getPasswordChanged() != null ? user.getPasswordChanged() : false)
                .build();
    }

    /**
     * Logout - revoke refresh token
     */
    @Transactional
    public void logout(String refreshToken) {
        String tokenHash = jwtService.hashToken(refreshToken);
        refreshTokenRepository.revokeByTokenHash(tokenHash);
    }

    /**
     * Logout all devices - revoke all refresh tokens
     */
    @Transactional
    public void logoutAllDevices(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    // ==========================================
    // === PASSWORD AUTHENTICATION ===
    // ==========================================

    /**
     * Default password for imported users
     */
    private static final String DEFAULT_PASSWORD = "Slib@2025";

    /**
     * Login with email/username/MSSV and password
     */
    @Transactional
    public AuthResponse loginWithPassword(String identifier, String password, String deviceInfo) {
        // Tìm user bằng email hoặc username (case-insensitive)
        String identifierUpper = identifier.toUpperCase();
        User user = userRepository.findByEmailOrUsername(identifier, identifierUpper)
                .orElseThrow(() -> new RuntimeException("Tài khoản hoặc mật khẩu không đúng"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản đã bị khóa, vui lòng liên hệ quản trị viên để được hỗ trợ.");
        }

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new RuntimeException(
                    "Tài khoản chưa được thiết lập mật khẩu. Vui lòng liên hệ quản trị viên hoặc sử dụng đăng nhập Google.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Tài khoản hoặc mật khẩu không đúng");
        }

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Revoke all old refresh tokens before saving new one (single device policy)
        refreshTokenRepository.revokeAllByUserId(user.getId());

        // Save refresh token hash to database
        saveRefreshToken(user, refreshToken, deviceInfo);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .id(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userCode(user.getUserCode())
                .role(user.getRole().name())
                .expiresIn(3600L)
                .passwordChanged(user.getPasswordChanged() != null ? user.getPasswordChanged() : false)
                .build();
    }

    /**
     * Change password for authenticated user
     */
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // If user has a password set, verify current password
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new RuntimeException("Mật khẩu hiện tại không đúng");
            }
        }

        // Validate new password
        validatePassword(newPassword);

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChanged(true);
        userRepository.save(user);
    }

    /**
     * Admin reset password for user
     */
    @Transactional
    public void adminResetPassword(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setPasswordChanged(false);
        userRepository.save(user);

        // Revoke all refresh tokens to force re-login
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    /**
     * Update password (after OTP verification - no current password required)
     */
    @Transactional
    public void updatePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Validate new password
        validatePassword(newPassword);

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChanged(true);
        userRepository.save(user);

        // Revoke all refresh tokens to force re-login
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    /**
     * Validate password strength
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 8 ký tự");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 1 chữ hoa");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 1 chữ thường");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 1 số");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 1 ký tự đặc biệt");
        }
    }

    /**
     * Encode password for new user import
     */
    public String encodeDefaultPassword() {
        return passwordEncoder.encode(DEFAULT_PASSWORD);
    }

    // ==========================================
    // === PRIVATE HELPERS ===
    // ==========================================

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new RuntimeException("Google ID token không hợp lệ");
            }
            return idToken.getPayload();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xác thực Google: " + e.getMessage());
        }
    }

    private String extractUserCode(String email) {
        String emailPrefix = email.split("@")[0].toUpperCase();
        Pattern pattern = Pattern.compile("([A-Z]{2}\\d{4,})");
        Matcher matcher = pattern.matcher(emailPrefix);

        if (matcher.find()) {
            String found = matcher.group(1);
            if (emailPrefix.endsWith(found)) {
                return found;
            }
        }
        return emailPrefix;
    }

    private void saveRefreshToken(User user, String refreshToken, String deviceInfo) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(jwtService.hashToken(refreshToken))
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiration()))
                .deviceInfo(deviceInfo)
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);
    }
}
