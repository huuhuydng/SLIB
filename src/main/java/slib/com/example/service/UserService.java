package slib.com.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import slib.com.example.dto.RegisterRequest;
import slib.com.example.dto.UserProfileResponse;
import slib.com.example.entity.Role;
import slib.com.example.entity.UserEntity;
import slib.com.example.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final WebClient webClient;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}") // Service Role Key
    private String supabaseKey;

    public UserService(UserRepository userRepository, WebClient.Builder webClientBuilder) {
        this.userRepository = userRepository;
        this.webClient = webClientBuilder.build();
    }

    // 1. NHÓM CHỨC NĂNG AUTH (GỌI SUPABASE API)

    public String registerUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại!");
        }
        if (userRepository.findByStudentCode(request.getStudentCode()).isPresent()) {
            throw new RuntimeException("Mã sinh viên đã tồn tại!");
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("full_name", request.getFullName());
        metadata.put("student_code", request.getStudentCode());
        metadata.put("dob", request.getDob());
        metadata.put("role", "STUDENT");
        Map<String, Object> body = new HashMap<>();
        body.put("email", request.getEmail());
        body.put("password", request.getPassword());
        body.put("data", metadata);
        body.put("email_confirm", true);
        try {
            return webClient.post()
                    .uri(supabaseUrl + "/auth/v1/signup")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    // Hàm asynchronous
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            System.out.println("--- LỖI ĐĂNG KÍ SUPABASE ---");
            System.out.println("Status Code: " + e.getStatusCode());
            System.out.println("Lỗi chi tiết: " + e.getResponseBodyAsString());
            throw new RuntimeException("Supabase Error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đăng ký Supabase: " + e.getMessage());
        }
    }

    public String login(String email, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        try {
            return webClient.post()
                    .uri(supabaseUrl + "/auth/v1/token?grant_type=password")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            System.out.println("--- LỖI LOGIN SUPABASE ---");
            System.out.println("Status Code: " + e.getStatusCode());
            System.out.println("Lỗi chi tiết: " + e.getResponseBodyAsString());
            throw new RuntimeException("Supabase Error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi hệ thống: " + e.getMessage());
        }
    }

    public String verifyEmailOtp(String email, String token, String type) {
        Map<String, Object> body = new HashMap<>();
        String verifyType = (type == null || type.isEmpty()) ? "signup" : type;
        body.put("type", verifyType);
        body.put("token", token.trim());
        body.put("email", email.trim().toLowerCase());
        try {
            String jsonResponse = webClient.post()
                    .uri(supabaseUrl + "/auth/v1/verify")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            saveUserToDatabase(jsonResponse);
            return jsonResponse;
        } catch (WebClientResponseException e) {
            System.out.println("--- LỖI VERIFY MAIL SUPABASE ---");
            System.out.println("Status Code: " + e.getStatusCode());
            System.out.println("Lỗi chi tiết: " + e.getResponseBodyAsString());
            throw new RuntimeException("Supabase Error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi hệ thống: " + e.getMessage());
        }
    }

    public String verifyRecoveryOtp(String email, String otp) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email.trim().toLowerCase());
        body.put("token", otp.trim());
        body.put("type", "email"); 
        try {
            String jsonResponse = webClient.post()
                    .uri(supabaseUrl + "/auth/v1/verify")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return extractAccessToken(jsonResponse);
        } catch (WebClientResponseException e) {
            System.out.println("--- LỖI VERIFY OTP KHÔI PHỤC ---");
            System.out.println("Lỗi chi tiết: " + e.getResponseBodyAsString());
            throw new RuntimeException("Mã OTP không đúng hoặc đã hết hạn");
        }
    }

    private String extractAccessToken(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);
            if (root.has("access_token")) {
                return root.get("access_token").asText();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void saveUserToDatabase(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode userNode = root.get("user");
            if (userNode == null)
                return;
            String userId = userNode.get("id").asText();
            if (userRepository.existsById(UUID.fromString(userId))) {
                return;
            }
            JsonNode meta = userNode.get("user_metadata");
            UserEntity newUser = new UserEntity();
            newUser.setUserId(UUID.fromString(userId));
            newUser.setEmail(userNode.get("email").asText());

            // Dùng .path để tránh null pointer
            newUser.setFullName(meta.path("full_name").asText(""));
            newUser.setStudentCode(meta.path("student_code").asText(""));

            String dobStr = meta.path("dob").asText(null);
            if (dobStr != null) {
                newUser.setDob(java.time.LocalDate.parse(dobStr));
            }
            String roleStr = meta.path("role").asText("STUDENT");
            try {
                newUser.setRole(Role.valueOf(roleStr));
            } catch (Exception e) {
                newUser.setRole(Role.STUDENT);
            }
            newUser.setReputationScore(100);
            userRepository.save(newUser);
            System.out.println("✅ Đã lưu user mới vào Database: " + newUser.getEmail());

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi lưu user vào DB: " + e.getMessage());
        }
    }

    public String resendOtp(String email, String type) {
        Map<String, String> body = new HashMap<>();
        String resendType = (type == null || type.isEmpty()) ? "signup" : type;
        body.put("email", email.trim().toLowerCase());
        body.put("type", resendType);
        try {
            return webClient.post()
                    .uri(supabaseUrl + "/auth/v1/resend")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            System.out.println("--- LỖI RESEND OTP SUPABASE ---");
            System.out.println("Status Code: " + e.getStatusCode());
            System.out.println("Lỗi chi tiết: " + e.getResponseBodyAsString());
            throw new RuntimeException("Supabase Error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi hệ thống: " + e.getMessage());
        }
    }

    public UserProfileResponse getMyProfile(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserProfileResponse.builder()
                .id(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .studentCode(user.getStudentCode())
                .role(user.getRole().name())
                .reputationScore(user.getReputationScore() != null ? user.getReputationScore() : 100)
                .build();
    }

    public void sendRecoveryEmail(String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("create_user", false); 
        try {
            webClient.post()
                    .uri(supabaseUrl + "/auth/v1/otp")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            System.out.println("--- LỖI GỬI OTP KHÔI PHỤC ---");
            System.out.println("Status Code: " + e.getStatusCode());
            System.out.println("Lỗi chi tiết: " + e.getResponseBodyAsString());
            throw new RuntimeException("Supabase Error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi hệ thống: " + e.getMessage());
        }
    }

    public String updatePassword(String userAccessToken, String newPassword) {
        Map<String, String> body = new HashMap<>();
        body.put("password", newPassword);
        try {
            return webClient.put()
                    .uri(supabaseUrl + "/auth/v1/user")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + userAccessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
             System.out.println("--- LỖI ĐỔI MẬT KHẨU SUPABASE ---");
            System.out.println("Status Code: " + e.getStatusCode());
            System.out.println("Lỗi chi tiết: " + e.getResponseBodyAsString());
            throw new RuntimeException("Supabase Error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // 2. NHÓM CHỨC NĂNG DATA (GỌI REPOSITORY)

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }
    
    public UserEntity getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với email: " + email));
    }

    public UserEntity getUserByStudentCode(String studentCode) {
        return userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với MSSV: " + studentCode));
    }

    public UserEntity getUserById(UUID uuid) {
        return userRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user ID: " + uuid));
    }

    public UserEntity updateUser(UUID userId, UserEntity userDetails) {
        UserEntity existingUser = getUserById(userId);
        if (userDetails.getFullName() != null) {
            existingUser.setFullName(userDetails.getFullName());
        }
        if (userDetails.getNotiDevice() != null) {
            existingUser.setNotiDevice(userDetails.getNotiDevice());
        }
        return userRepository.save(existingUser);
    }

    // Xoá User: Lưu ý, cái này chỉ xoá trong bảng Public
    // Muốn xoá triệt để phải gọi API Auth xoá User (Admin function)
    public boolean deleteUser(UUID userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }

}