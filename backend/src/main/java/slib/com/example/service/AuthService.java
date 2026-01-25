package slib.com.example.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
                "huuhuy.k4@gmail.com"
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

        // Extract student code from email (or use email prefix for non-FPT emails)
        String studentCode = email.endsWith("@fpt.edu.vn")
                ? extractStudentCode(email)
                : email.split("@")[0].toUpperCase();

        // Get or create user
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            String fullName = (fullNameFromClient != null && !fullNameFromClient.isEmpty())
                    ? fullNameFromClient
                    : googleName;

            user = User.builder()
                    .email(email)
                    .studentCode(studentCode)
                    .fullName(fullName != null ? fullName : studentCode)
                    .role(Role.STUDENT)
                    .isActive(true)
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

        // Save refresh token hash to database
        saveRefreshToken(user, refreshToken, deviceInfo);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .id(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .studentCode(user.getStudentCode())
                .role(user.getRole().name())
                .expiresIn(3600L) // 1 hour in seconds
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
                .studentCode(user.getStudentCode())
                .role(user.getRole().name())
                .expiresIn(3600L)
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

    private String extractStudentCode(String email) {
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
