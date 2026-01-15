package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import slib.com.example.dto.LibrarianLoginRequest;
import slib.com.example.dto.LibrarianLoginResponse;
import slib.com.example.dto.UserProfileResponse;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LibrarianService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    /**
     * Đăng nhập cho LIBRARIAN với email/password
     * Gọi Supabase Auth API để xác thực
     */
    public LibrarianLoginResponse login(LibrarianLoginRequest request) {
        // Validate input
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Mật khẩu không được để trống");
        }
        
        try {
            // 1. Gọi Supabase Auth API để xác thực
            String authUrl = supabaseUrl + "/auth/v1/token?grant_type=password";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseKey);
            
            Map<String, String> authRequest = new HashMap<>();
            authRequest.put("email", request.getEmail());
            authRequest.put("password", request.getPassword());
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(authRequest, headers);
            
            log.info("Đang xác thực librarian với email: {}", request.getEmail());
            
            ResponseEntity<Map> response = restTemplate.exchange(
                authUrl,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> authResponse = response.getBody();
                
                // 2. Lấy thông tin user từ response
                String accessToken = (String) authResponse.get("access_token");
                String refreshToken = (String) authResponse.get("refresh_token");
                Integer expiresIn = (Integer) authResponse.get("expires_in");
                
                Map<String, Object> userMap = (Map<String, Object>) authResponse.get("user");
                String userIdStr = (String) userMap.get("id");
                String email = (String) userMap.get("email");
                
                // 3. Kiểm tra user trong database
                UUID userId = UUID.fromString(userIdStr);
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng trong hệ thống"));
                
                // 4. Kiểm tra role phải là LIBRARIAN
                if (user.getRole() != Role.LIBRARIAN) {
                    log.warn("User {} không phải là LIBRARIAN, role hiện tại: {}", email, user.getRole());
                    throw new RuntimeException("Bạn không có quyền đăng nhập với vai trò thủ thư");
                }
                
                // 5. Tạo response
                UserProfileResponse userProfile = UserProfileResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .studentCode(user.getStudentCode())
                    .role(user.getRole().name())
                    .reputationScore(user.getReputationScore() != null ? user.getReputationScore() : 0)
                    .build();
                
                log.info("Đăng nhập thành công cho librarian: {}", email);
                
                return LibrarianLoginResponse.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .expiresIn(expiresIn != null ? expiresIn.longValue() : 3600L)
                    .refreshToken(refreshToken)
                    .user(userProfile)
                    .build();
            }
            
            throw new RuntimeException("Xác thực thất bại");
            
        } catch (HttpClientErrorException e) {
            log.error("Lỗi xác thực: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new RuntimeException("Email hoặc mật khẩu không đúng");
            }
            
            throw new RuntimeException("Lỗi xác thực: " + e.getMessage());
            
        } catch (Exception e) {
            log.error("Lỗi không xác định khi đăng nhập librarian", e);
            throw new RuntimeException("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // ============ NHÓM CHỨC NĂNG QUÊN MẬT KHẨU & OTP ============

    /**
     * Gửi email khôi phục mật khẩu (OTP) cho Librarian
     */
    public void sendRecoveryEmail(String email) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        try {
            String recoveryUrl = supabaseUrl + "/auth/v1/recover";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseKey);
            headers.set("Authorization", "Bearer " + supabaseKey);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            log.info("Gửi email khôi phục mật khẩu cho librarian: {}", email);

            restTemplate.exchange(
                recoveryUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            log.info("Đã gửi email khôi phục thành công");

        } catch (HttpClientErrorException e) {
            log.error("Lỗi gửi mail khôi phục: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Lỗi gửi mail khôi phục: " + e.getResponseBodyAsString());
        }
    }

    /**
     * Xác thực OTP email cho Librarian
     */
    public String verifyEmailOtp(String email, String token, String type) {
        Map<String, Object> body = new HashMap<>();
        String verifyType = (type == null || type.isEmpty()) ? "recovery" : type;

        body.put("type", verifyType);
        body.put("token", token.trim());
        body.put("email", email.trim().toLowerCase());

        try {
            String verifyUrl = supabaseUrl + "/auth/v1/verify";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseKey);
            headers.set("Authorization", "Bearer " + supabaseKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            log.info("Xác thực OTP cho librarian: {}, type: {}", email, verifyType);

            ResponseEntity<String> response = restTemplate.exchange(
                verifyUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            log.info("Xác thực OTP thành công cho librarian: {}", email);
            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("Lỗi xác thực OTP: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Lỗi từ Supabase: " + e.getResponseBodyAsString());
        }
    }

    /**
     * Gửi lại OTP cho Librarian
     */
    public void resendOtp(String email, String type) {
        Map<String, Object> body = new HashMap<>();
        String resendType = (type == null || type.isEmpty()) ? "signup" : type;

        body.put("type", resendType);
        body.put("email", email.trim().toLowerCase());

        try {
            String resendUrl = supabaseUrl + "/auth/v1/resend";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseKey);
            headers.set("Authorization", "Bearer " + supabaseKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            log.info("Gửi lại OTP cho librarian: {}, type: {}", email, resendType);

            restTemplate.exchange(
                resendUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            log.info("Đã gửi lại OTP thành công");

        } catch (HttpClientErrorException e) {
            log.error("Lỗi gửi lại OTP: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Lỗi gửi lại OTP: " + e.getResponseBodyAsString());
        }
    }

    /**
     * Cập nhật mật khẩu mới cho Librarian
     */
    public String updatePassword(String userAccessToken, String newPassword) {
        Map<String, String> body = new HashMap<>();
        body.put("password", newPassword);

        try {
            String updateUrl = supabaseUrl + "/auth/v1/user";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseKey);
            headers.set("Authorization", "Bearer " + userAccessToken);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            log.info("Cập nhật mật khẩu mới cho librarian");

            ResponseEntity<String> response = restTemplate.exchange(
                updateUrl,
                HttpMethod.PUT,
                entity,
                String.class
            );

            log.info("Đổi mật khẩu thành công");
            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("Lỗi đổi mật khẩu: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Đổi mật khẩu thất bại: " + e.getResponseBodyAsString());
        }
    }
}
