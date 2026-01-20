package slib.com.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import slib.com.example.dto.users.UserProfileResponse;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    public Map<String, Object> loginWithGoogle(String idToken, String fullNameFromClient, String fcmToken) {
        // Use AuthService instead of Supabase
        return authService.loginWithGoogle(idToken, fullNameFromClient, fcmToken);
    }

    private User syncGoogleUserToLocalDB(String jsonResponse, String clientFullName, String fcmToken) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        JsonNode userNode = root.get("user");

        if (userNode == null) {
            throw new RuntimeException("Không lấy được thông tin User từ Supabase");
        }

        String email = userNode.get("email").asText();
        String supabaseIdString = userNode.get("id").asText();
        UUID supabaseUid = UUID.fromString(supabaseIdString);

        if (!email.endsWith("@fpt.edu.vn")) {
            throw new RuntimeException("Chỉ chấp nhận email @fpt.edu.vn");
        }

        String emailPrefix = email.split("@")[0].toUpperCase(); 
        String studentCode = emailPrefix; 

        Pattern pattern = Pattern.compile("([A-Z]{2}\\d{4,})");
        Matcher matcher = pattern.matcher(emailPrefix);

        if (matcher.find()) {
            String found = matcher.group(1);
            if (emailPrefix.endsWith(found)) {
                studentCode = found;
            }
        }

        String fullName = (clientFullName != null && !clientFullName.isEmpty())
                ? clientFullName
                : userNode.path("user_metadata").path("full_name").asText(studentCode);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = User.builder()
                    .email(email)
                    .studentCode(studentCode) 
                    .fullName(fullName)
                    .supabaseUid(supabaseUid)
                    .role(Role.STUDENT)
                    .reputationScore(100)
                    .isActive(true)
                    .notiDevice(fcmToken)
                    .build();
        } else {
            if (user.getSupabaseUid() == null) {
                user.setSupabaseUid(supabaseUid);
                userRepository.save(user);
            }
            if (fcmToken != null && !fcmToken.isEmpty()) {
                user.setNotiDevice(fcmToken);
            }
        }
        return userRepository.save(user);
    }


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


    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUser(UUID userId, User req) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại với ID: " + userId));

        if (req.getNotiDevice() != null && !req.getNotiDevice().isEmpty()) {
            existingUser.setNotiDevice(req.getNotiDevice());
        }
        return userRepository.save(existingUser);
    }
}

