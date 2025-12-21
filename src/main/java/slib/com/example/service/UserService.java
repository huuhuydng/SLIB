package slib.com.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

import slib.com.example.dto.RegisterRequest;
import slib.com.example.dto.UserProfileResponse;
import slib.com.example.entity.UserEntity;
import slib.com.example.repository.UserRepository;
import slib.com.example.entity.Role;

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

    // Constructor Injection
    public UserService(UserRepository userRepository, WebClient.Builder webClientBuilder) {
        this.userRepository = userRepository;
        this.webClient = webClientBuilder.build();
    }

    // 1. NHÓM CHỨC NĂNG AUTH (GỌI SUPABASE API)

    public String registerUser(RegisterRequest request) {
        // Kiểm tra email và student code đã tồn tại chưa
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại!");
        }
        if (userRepository.findByStudentCode(request.getStudentCode()).isPresent()) {
            throw new RuntimeException("Mã sinh viên đã tồn tại!");
        }

        // Chuẩn bị Metadata gửi sang Trigger
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("full_name", request.getFullName());
        metadata.put("student_code", request.getStudentCode());
        metadata.put("dob", request.getDob());
        metadata.put("role", "STUDENT");

        // Body chuẩn của Supabase Signup
        Map<String, Object> body = new HashMap<>();
        body.put("email", request.getEmail());
        body.put("password", request.getPassword());
        body.put("data", metadata);
        body.put("email_confirm", true);

        System.out.println("--- DEBUG REGISTER ---");
        System.out.println("Email gửi đi: [" + request.getEmail() + "]");
        System.out.println("Pass gửi đi: [" + request.getPassword() + "]");

        try {
            return webClient.post()
                    // Chứng minh lệnh từ admin
                    .uri(supabaseUrl + "/auth/v1/signup")
                    // API Gateway
                    .header("apikey", supabaseKey)
                    // Bearer Token
                    .header("Authorization", "Bearer " + supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    // Hàm asynchronous
                    .retrieve()
                    .bodyToMono(String.class) // đoạn này dùng Future bên Flutter -> hứa hẹn sẽ trả
                    .block(); // chuyển async sang sync => chờ kết quả trả về
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đăng ký Supabase: " + e.getMessage());
        }
    }

    // Trả về Token (JWT) nếu đúng, ném lỗi nếu sai
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
            // IN LỖI CHI TIẾT RA CONSOLE ĐỂ DEBUG
            System.out.println("--- LỖI LOGIN SUPABASE ---");
            System.out.println("Status Code: " + e.getStatusCode());
            System.out.println("Lỗi chi tiết: " + e.getResponseBodyAsString());

            // Ném lỗi chi tiết ra Postman xem luôn
            throw new RuntimeException("Supabase Error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // Gọi API verify của Supabase để xác thực email với mã OTP -> trả về Token JWT
    // nếu thành công -> App tự login luôn
    public String verifyEmailOtp(String email, String token, String type) {
        Map<String, Object> body = new HashMap<>();

        // NẾU KHÔNG TRUYỀN TYPE THÌ MẶC ĐỊNH LÀ SIGNUP
        String verifyType = (type == null || type.isEmpty()) ? "signup" : type;

        body.put("type", verifyType); // <--- Dùng biến dynamic, không gán cứng nữa
        body.put("token", token.trim());
        body.put("email", email.trim().toLowerCase());

        System.err.println("--- DEBUG VERIFY OTP ---");
        System.err.println("Email: " + email);
        System.err.println("Token: " + token);
        System.err.println("Type: " + verifyType); // In ra để kiểm tra

        try {
            return webClient.post()
                    .uri(supabaseUrl + "/auth/v1/verify")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            System.err.println("--- SUPABASE VERIFY ERROR ---");
            System.err.println("Status: " + e.getStatusCode());
            System.err.println("Body: " + e.getResponseBodyAsString());
            throw new RuntimeException("Lỗi từ Supabase: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
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
                // ĐÃ XÓA dòng: .avatarUrl(...)
                .build();
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