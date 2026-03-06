package slib.com.example.service;

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
}
