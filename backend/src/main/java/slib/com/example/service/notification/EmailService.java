package slib.com.example.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@slib.edu.vn}")
    private String fromEmail;

    /**
     * Gửi email OTP đặt lại mật khẩu
     */
    @Async
    public void sendPasswordResetOtp(String toEmail, String otpCode) {
        try {
            String htmlContent = loadEmailTemplate("templates/password-reset-email.html");
            htmlContent = htmlContent.replace("{{otpCode}}", otpCode);

            sendHtmlEmail(toEmail, "Mã OTP đặt lại mật khẩu SLib", htmlContent);
            log.info("Đã gửi email OTP đến: {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi gửi email OTP đến {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Không thể gửi email OTP: " + e.getMessage());
        }
    }

    /**
     * Gửi email HTML
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("Email đã gửi thành công đến: {}", to);
    }

    /**
     * Load email template từ resources
     */
    private String loadEmailTemplate(String templatePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(templatePath);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    /**
     * Gửi email đơn giản (text)
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);

            mailSender.send(message);
            log.info("Email đơn giản đã gửi đến: {}", to);
        } catch (MessagingException e) {
            log.error("Lỗi gửi email đến {}: {}", to, e.getMessage());
        }
    }

    /**
     * Gửi email chào mừng khi tạo tài khoản mới (thủ thư/sinh viên)
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String fullName, String defaultPassword, String role) {
        try {
            String htmlContent = loadEmailTemplate("templates/welcome-email.html");

            String normalizedRole = role != null ? role.toUpperCase() : "STUDENT";
            String roleLabel = switch (normalizedRole) {
                case "ADMIN" -> "Quản trị viên";
                case "LIBRARIAN" -> "Thủ thư";
                case "TEACHER" -> "Giáo viên";
                default -> "Sinh viên";
            };
            String loginUrl = ("LIBRARIAN".equals(normalizedRole) || "ADMIN".equals(normalizedRole))
                    ? "http://localhost:5173/login"
                    : "https://slib.edu.vn";

            htmlContent = htmlContent.replace("{{fullName}}", fullName != null ? fullName : toEmail);
            htmlContent = htmlContent.replace("{{email}}", toEmail);
            htmlContent = htmlContent.replace("{{password}}", defaultPassword);
            htmlContent = htmlContent.replace("{{role}}", roleLabel);
            htmlContent = htmlContent.replace("{{loginUrl}}", loginUrl);

            sendHtmlEmail(toEmail, "🎉 Chào mừng bạn đến với SLib - Thông tin đăng nhập", htmlContent);
            log.info("Đã gửi welcome email đến: {} (role: {})", toEmail, role);
        } catch (Exception e) {
            log.error("Lỗi gửi welcome email đến {}: {}", toEmail, e.getMessage());
        }
    }
}
