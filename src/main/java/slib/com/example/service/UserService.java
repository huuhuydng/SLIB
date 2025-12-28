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
    // 2. Hàm Xác thực OTP & Lưu User vào Database (QUAN TRỌNG)
    public String verifyEmailOtp(String email, String token, String type) {
        Map<String, Object> body = new HashMap<>();
        String verifyType = (type == null || type.isEmpty()) ? "signup" : type;

        body.put("type", verifyType);
        body.put("token", token.trim());
        body.put("email", email.trim().toLowerCase());

        try {
            // 1. Gọi Supabase để verify
            String jsonResponse = webClient.post()
                    .uri(supabaseUrl + "/auth/v1/verify")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 2. Nếu Verify thành công, tiến hành lưu User vào public.users
            saveUserToDatabase(jsonResponse);

            return jsonResponse;

        } catch (WebClientResponseException e) {
            throw new RuntimeException("Lỗi từ Supabase: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // Hàm phụ: Parse JSON từ Supabase và lưu vào DB
    private void saveUserToDatabase(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);

            // Lấy object User
            JsonNode userNode = root.get("user");
            if (userNode == null)
                return;

            // Lấy ID (UUID từ Supabase)
            String userId = userNode.get("id").asText();

            // Kiểm tra xem user đã có trong DB chưa (để tránh lưu trùng nếu verify 2 lần)
            if (userRepository.existsById(UUID.fromString(userId))) {
                return; // Đã có rồi thì thôi
            }

            // Lấy Metadata (nơi chứa tên, mssv, role...)
            JsonNode meta = userNode.get("user_metadata");

            // Tạo Entity mới
            UserEntity newUser = new UserEntity();
            newUser.setUserId(UUID.fromString(userId)); // Đồng bộ ID với Supabase
            newUser.setEmail(userNode.get("email").asText());

            // Map dữ liệu từ Metadata (Dùng .path để tránh null pointer)
            newUser.setFullName(meta.path("full_name").asText("Chưa cập nhật"));
            newUser.setStudentCode(meta.path("student_code").asText(""));

            // Parse ngày sinh (cẩn thận format)
            String dobStr = meta.path("dob").asText(null);
            if (dobStr != null) {
                newUser.setDob(java.time.LocalDate.parse(dobStr));
            }

            // Set Role
            String roleStr = meta.path("role").asText("STUDENT");
            try {
                newUser.setRole(Role.valueOf(roleStr));
            } catch (Exception e) {
                newUser.setRole(Role.STUDENT);
            }

            newUser.setReputationScore(100); // Điểm mặc định

            // LƯU VÀO DATABASE
            userRepository.save(newUser);
            System.out.println("✅ Đã lưu user mới vào Database: " + newUser.getEmail());

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi lưu user vào DB: " + e.getMessage());
            // Không ném lỗi ra ngoài để tránh làm user hoang mang,
            // vì thực tế verify đã thành công. Có thể log lại để admin xử lý.
        }
    }

    public String resendOtp(String email, String type) {
        Map<String, String> body = new HashMap<>();

        // Supabase hỗ trợ các type: "signup", "recovery" (quên pass), "invite",
        // "email_change"
        String resendType = (type == null || type.isEmpty()) ? "signup" : type;

        body.put("email", email.trim().toLowerCase());
        body.put("type", resendType);

        System.out.println("--- DEBUG RESEND OTP ---");
        System.out.println("Email: " + email);
        System.out.println("Type: " + resendType);

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
            System.err.println("Lỗi Resend Supabase: " + e.getResponseBodyAsString());
            // Supabase thường trả về lỗi nếu gửi quá nhanh (Rate Limit)
            throw new RuntimeException("Gửi lại thất bại: " + e.getResponseBodyAsString());
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
                // ĐÃ XÓA dòng: .avatarUrl(...)
                .build();
    }

    public void sendRecoveryEmail(String email) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        System.out.println("--- DEBUG RECOVERY ---");
        System.out.println("Gửi OTP khôi phục cho: " + email);

        try {
            webClient.post()
                    .uri(supabaseUrl + "/auth/v1/recover")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey) // Dùng Key Admin
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Lỗi gửi mail khôi phục: " + e.getResponseBodyAsString());
        }
    }

    // 2. Đặt lại mật khẩu mới (Cần có Access Token của user)
    public String updatePassword(String userAccessToken, String newPassword) {
        Map<String, String> body = new HashMap<>();
        body.put("password", newPassword);

        try {
            // Lưu ý: Endpoint này dùng TOKEN CỦA USER (để biết đổi pass cho ai)
            // Chứ không dùng Supabase Key của Admin
            return webClient.put()
                    .uri(supabaseUrl + "/auth/v1/user")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + userAccessToken) // ⚠️ Quan trọng: Token người dùng
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Đổi mật khẩu thất bại: " + e.getResponseBodyAsString());
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

    public String loginWithGoogle(String idToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("id_token", idToken);
        body.put("provider", "google");

        try {
            String result = webClient.post()
                    // Endpoint chuẩn cho id_token flow
                    .uri(supabaseUrl + "/auth/v1/token?grant_type=id_token")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return result;
        } catch (WebClientResponseException e) {
            System.err.println("Supabase Error Body: " + e.getResponseBodyAsString());
            throw new RuntimeException(e.getResponseBodyAsString());
        }
    }

}