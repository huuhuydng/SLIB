package slib.com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class VerificationService {
    private final Map<String, String> verificationCodes = new HashMap<>();

    @Autowired
    private JavaMailSender mailSender;

    // 6 số random
    public String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    // mail xác nhận
    public void sendVerificationEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("hungtestpj01@gmail.com");
        message.setTo(toEmail);
        message.setSubject("Mã xác nhận đăng ký SLIB");
        message.setText("Mã xác nhận của bạn là: " + code);
        mailSender.send(message);
    }

    // Lưu mã vào cache
    public void saveCode(String email, String code) {
        verificationCodes.put(email, code);
    }

    // Kiểm tra mã
    public boolean verifyCode(String email, String code) {
        return verificationCodes.containsKey(email) && verificationCodes.get(email).equals(code);
    }

    // Xóa mã sau khi dùng
    public void removeCode(String email) {
        verificationCodes.remove(email);
    }
}
