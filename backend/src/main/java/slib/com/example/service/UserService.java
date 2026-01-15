package slib.com.example.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import slib.com.example.dto.UserProfileResponse;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;
import slib.com.example.security.JwtService;

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
    private final JwtService jwtService;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    public UserService(UserRepository userRepository, WebClient.Builder webClientBuilder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.webClient = webClientBuilder.build();
        this.jwtService = jwtService;
    }

    /**
     * Login with Google ID Token
     * Mobile: Uses Supabase authentication (has fcmToken)
     * Web: Uses JWT decode (Google One Tap already verified token, no fcmToken)
     */
    public Map<String, Object> loginWithGoogle(String idToken, String fullNameFromClient, String fcmToken) {
        System.out.println("🔵 [loginWithGoogle] fcmToken: " + fcmToken);
        System.out.println("🔵 [loginWithGoogle] idToken starts with: " + (idToken != null ? idToken.substring(0, Math.min(20, idToken.length())) : "null"));
        
        // Nếu có fcmToken = Mobile app (Supabase flow)
        // Nếu không có fcmToken = Web admin (Google JWT flow)
        if (fcmToken != null && !fcmToken.isEmpty()) {
            System.out.println("📱 Mobile flow detected - using Supabase");
            return loginWithSupabase(idToken, fullNameFromClient, fcmToken);
        } else {
            System.out.println("💻 Web flow detected - using Google JWT");
            return loginWithGoogleJWT(idToken, fullNameFromClient);
        }
    }
    
    /**
     * Web Admin Login: Decode Google JWT directly
     */
    private Map<String, Object> loginWithGoogleJWT(String idToken, String fullNameFromClient) {
        try {
            System.out.println("🟡 [UserService] Web admin Google login");
            
            // Decode JWT token (Google đã verify)
            DecodedJWT jwt = JWT.decode(idToken);
            String email = jwt.getClaim("email").asString();
            String fullName = jwt.getClaim("name").asString();
            
            if (email == null || email.isEmpty()) {
                throw new RuntimeException("Không lấy được email từ Google token");
            }
            
            System.out.println("📧 Email from Google: " + email);
            
            // Tìm user trong DB
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user == null) {
                System.err.println("❌ User not found in database: " + email);
                throw new RuntimeException("Tài khoản chưa được đăng ký trong hệ thống. Vui lòng liên hệ admin.");
            }
            
            // ✅ Kiểm tra role - Web chỉ cho LIBRARIAN login
            if (user.getRole() != Role.LIBRARIAN) {
                System.err.println("❌ User is not librarian: " + user.getRole());
                throw new RuntimeException("Chỉ thủ thư mới được đăng nhập vào hệ thống web.");
            }
            
            System.out.println("✅ User found: " + user.getEmail() + " - Role: " + user.getRole());
            
            // Tạo JWT token của hệ thống
            String jwtToken = jwtService.generateToken(user.getEmail(), user.getRole().name());
            
            Map<String, Object> finalResponse = new HashMap<>();
            finalResponse.put("access_token", jwtToken);
            finalResponse.put("user", user);
            
            return finalResponse;

        } catch (RuntimeException e) {
            System.err.println("❌ Login error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ System error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi hệ thống: " + e.getMessage());
        }
    }
    
    /**
     * Mobile Login: Through Supabase
     */
    private Map<String, Object> loginWithSupabase(String idToken, String fullNameFromClient, String fcmToken) {
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

            User user = syncGoogleUserToLocalDB(jsonResponse, fullNameFromClient, fcmToken);

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
                .supabaseUid(user.getSupabaseUid())
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

