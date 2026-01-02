package slib.com.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import slib.com.example.dto.UserProfileResponse;
import slib.com.example.entity.Role;
import slib.com.example.entity.UserEntity;
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
    private final WebClient webClient;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    public UserService(UserRepository userRepository, WebClient.Builder webClientBuilder) {
        this.userRepository = userRepository;
        this.webClient = webClientBuilder.build();
    }

    public Map<String, Object> loginWithGoogle(String idToken, String fullNameFromClient, String fcmToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("id_token", idToken);
        body.put("provider", "google");
        try {
            String jsonResponse = webClient.post()
                    .uri(supabaseUrl + "/auth/v1/token?grant_type=id_token")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            UserEntity user = syncGoogleUserToLocalDB(jsonResponse, fullNameFromClient, fcmToken);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);
            String accessToken = root.path("access_token").asText();


            Map<String, Object> finalResponse = new HashMap<>();
            finalResponse.put("access_token", accessToken);
            finalResponse.put("user", user);

            return finalResponse;

        } catch (WebClientResponseException e) {
            System.err.println("Supabase Error: " + e.getResponseBodyAsString());
            throw new RuntimeException("Lỗi xác thực Google: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace(); 
            throw new RuntimeException("Lỗi hệ thống: " + e.getMessage());
        }
    }

    private UserEntity syncGoogleUserToLocalDB(String jsonResponse, String clientFullName, String fcmToken) throws Exception {
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

        System.out.println("Email: " + email + " -> MSSV extracted: " + studentCode);

        String fullName = (clientFullName != null && !clientFullName.isEmpty())
                ? clientFullName
                : userNode.path("user_metadata").path("full_name").asText(studentCode);

        UserEntity user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = UserEntity.builder()
                    .email(email)
                    .studentCode(studentCode) 
                    .fullName(fullName)
                    .supabaseUid(supabaseUid)
                    .role(Role.student)
                    .reputationScore(100)
                    .isActive(true)
                    .notiDevice(fcmToken)
                    .build();
            System.out.println("✅ INSERT USER MỚI: " + email);
        } else {
            if (user.getSupabaseUid() == null) {
                user.setSupabaseUid(supabaseUid);
                userRepository.save(user);
                System.out.println("♻️ LINK SUPABASE UID: " + email);
            }
            if (fcmToken != null && !fcmToken.isEmpty()) {
                user.setNotiDevice(fcmToken);
                System.out.println("📲 Updated FCM Token for: " + email);
            }
        }
        return userRepository.save(user);
    }

    // --- CÁC HÀM GET DATA ---

    public UserProfileResponse getMyProfile(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserProfileResponse.builder()
                .id(user.getId())
                .supabaseUid(user.getSupabaseUid())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .studentCode(user.getStudentCode())
                .role(user.getRole().name()) 
                .reputationScore(user.getReputationScore() != null ? user.getReputationScore() : 100)
                .isActive(user.getIsActive())
                .build();
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public UserEntity updateUser(UUID id, UserEntity req) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (req.getFullName() != null)
            user.setFullName(req.getFullName());
        if (req.getNotiDevice() != null)
            user.setNotiDevice(req.getNotiDevice());

        return userRepository.save(user);
    }
}

