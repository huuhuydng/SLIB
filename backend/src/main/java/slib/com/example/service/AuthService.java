package slib.com.example.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;
import slib.com.example.security.JwtService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${google.client-id}")
    private String googleClientId;

    /**
     * Login with Google ID token (no refresh token)
     */
    @Transactional
    public Map<String, Object> loginWithGoogle(String googleIdToken, String fullNameFromClient, String fcmToken) {
        // Verify Google ID token
        GoogleIdToken.Payload payload = verifyGoogleToken(googleIdToken);

        String email = payload.getEmail();
        String googleName = (String) payload.get("name");

        // Validate email: Allow @fpt.edu.vn OR specific admin emails
        if (!email.endsWith("@fpt.edu.vn") && !"phuckirito19@gmail.com".equalsIgnoreCase(email)) {
            throw new RuntimeException("Chỉ chấp nhận email @fpt.edu.vn hoặc email quản trị viên");
        }

        // Extract student code from email
        String studentCode = extractStudentCode(email);

        // Get or create user
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            String fullName = (fullNameFromClient != null && !fullNameFromClient.isEmpty())
                    ? fullNameFromClient
                    : googleName;

            // Determine role based on email
            Role role = determineRole(email);

            user = User.builder()
                    .email(email)
                    .studentCode(studentCode)
                    .fullName(fullName != null ? fullName : studentCode)
                    .role(role)
                    .reputationScore(100)
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

        // Generate access token
        String accessToken = jwtService.generateAccessToken(user);

        // Return response
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", accessToken);
        response.put("user", user);

        return response;
    }

    /**
     * Determine user role based on email pattern
     * - phucnhde170706@fpt.edu.vn -> LIBRARIAN (specific email)
     * - phuckirito19@gmail.com -> ADMIN (specific email)
     * - *admin*@fpt.edu.vn -> ADMIN
     * - Default -> STUDENT
     */
    public Role determineRole(String email) {
        // Check for specific LIBRARIAN emails
        if ("phucnhde170706@fpt.edu.vn".equalsIgnoreCase(email)) {
            return Role.LIBRARIAN;
        }
        
        // Check for specific ADMIN emails
        if ("phuckirito19@gmail.com".equalsIgnoreCase(email)) {
            return Role.ADMIN;
        }
        
        // Check for ADMIN pattern in email
        if (email.toLowerCase().contains("admin")) {
            return Role.ADMIN;
        }
        
        // Default is STUDENT
        return Role.STUDENT;
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
}
