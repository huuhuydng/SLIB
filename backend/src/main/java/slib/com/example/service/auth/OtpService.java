package slib.com.example.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.exception.BadRequestException;
import slib.com.example.entity.users.OtpToken;
import slib.com.example.entity.users.User;
import slib.com.example.repository.users.OtpTokenRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.notification.EmailService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;

    /**
     * Tạo và gửi OTP cho việc đặt lại mật khẩu
     */
    @Transactional
    public void sendPasswordResetOtp(String email) {
        // Kiểm tra user tồn tại
        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase().trim());
        if (userOpt.isEmpty()) {
            log.warn("Yêu cầu OTP cho email không tồn tại: {}", email);
            throw new BadRequestException("Tài khoản của bạn không nằm trong hệ thống");
        }

        User user = userOpt.get();
        // Kiểm tra user có email không (trường hợp import bằng MSSV không có email)
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            log.warn("User không có email: {}", email);
            throw new BadRequestException("Tài khoản của bạn chưa có email. Vui lòng liên hệ nhà trường để được hỗ trợ.");
        }

        // Vô hiệu hóa các OTP cũ
        otpTokenRepository.invalidateAllForEmail(email.toLowerCase().trim());

        // Tạo OTP mới
        String otpCode = generateOtp();

        OtpToken otpToken = OtpToken.builder()
                .email(email.toLowerCase().trim())
                .token(otpCode)
                .type(OtpToken.OtpType.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();

        otpTokenRepository.save(otpToken);
        log.info("Đã tạo OTP cho email: {}", email);

        // Gửi email
        emailService.sendPasswordResetOtp(email, otpCode);
    }

    /**
     * Xác thực OTP
     */
    @Transactional
    public boolean verifyOtp(String email, String otpCode) {
        Optional<OtpToken> otpOpt = otpTokenRepository.findValidOtp(
                email.toLowerCase().trim(),
                otpCode,
                LocalDateTime.now());

        if (otpOpt.isEmpty()) {
            log.warn("OTP không hợp lệ hoặc hết hạn cho email: {}", email);
            return false;
        }

        OtpToken otp = otpOpt.get();
        otp.setIsUsed(true);
        otpTokenRepository.save(otp);

        log.info("OTP xác thực thành công cho email: {}", email);
        return true;
    }

    /**
     * Gửi lại OTP
     */
    @Transactional
    public void resendOtp(String email) {
        sendPasswordResetOtp(email);
    }

    /**
     * Tạo mã OTP ngẫu nhiên
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * Xóa các OTP hết hạn (dùng cho scheduled job)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        otpTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Đã xóa các OTP hết hạn");
    }
}
